package im.angry.openeuicc.service

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import net.typeblog.lpac_jni.ProfileDownloadCallback

/**
 * An Android Service wrapper for EuiccChannelManager.
 * The purpose of this wrapper is mainly lifecycle-wise: having a Service allows the manager
 * instance to have its own independent lifecycle. This way it can be created as requested and
 * destroyed when no other components are bound to this service anymore.
 * This behavior allows us to avoid keeping the APDU channels open at all times. For example,
 * the EuiccService implementation should *only* bind to this service when it requires an
 * instance of EuiccChannelManager. UI components can keep being bound to this service for
 * their entire lifecycles, since the whole purpose of them is to expose the current state
 * to the user.
 *
 * Additionally, this service is also responsible for long-running "foreground" tasks that
 * are not suitable to be managed by UI components. This includes profile downloading, etc.
 * When a UI component needs to run one of these tasks, they have to bind to this service
 * and call one of the `launch*` methods, which will run the task inside this service's
 * lifecycle context and return a Flow instance for the UI component to subscribe to its
 * progress.
 */
class EuiccChannelManagerService : LifecycleService(), OpenEuiccContextMarker {
    companion object {
        private const val TAG = "EuiccChannelManagerService"
        private const val CHANNEL_ID = "tasks"
        private const val FOREGROUND_ID = 1000
        private const val TASK_FAILURE_ID = 1000

        /**
         * Utility function to wait for a foreground task to be done, return its
         * error if any, or null on success.
         */
        suspend fun Flow<ForegroundTaskState>.waitDone(): Throwable? =
            (this.last() as ForegroundTaskState.Done).error

        /**
         * Apply transform to a ForegroundTaskState flow so that it completes when a Done is seen.
         *
         * This must be applied each time a flow is returned for subscription purposes. If applied
         * beforehand, we lose the ability to subscribe multiple times.
         */
        private fun Flow<ForegroundTaskState>.applyCompletionTransform() =
            transformWhile {
                emit(it)
                it !is ForegroundTaskState.Done
            }
    }

    inner class LocalBinder : Binder() {
        val service = this@EuiccChannelManagerService
    }

    private val euiccChannelManagerDelegate = lazy {
        appContainer.euiccChannelManagerFactory.createEuiccChannelManager(this)
    }
    val euiccChannelManager: EuiccChannelManager by euiccChannelManagerDelegate

    /**
     * The state of a "foreground" task (named so due to the need to startForeground())
     */
    sealed interface ForegroundTaskState {
        data object Idle : ForegroundTaskState
        data class InProgress(val progress: Int) : ForegroundTaskState
        data class Done(val error: Throwable?) : ForegroundTaskState
    }

    /**
     * This flow emits whenever the service has had a start command, from startService()
     * The service self-starts when foreground is required, because other components
     * only bind to this service and do not start it per-se.
     */
    private val foregroundStarted: MutableSharedFlow<Unit> = MutableSharedFlow()

    /**
     * This flow is used to emit progress updates when a foreground task is running.
     */
    private val foregroundTaskState: MutableStateFlow<ForegroundTaskState> =
        MutableStateFlow(ForegroundTaskState.Idle)

    /**
     * A simple wrapper over a flow with taskId added.
     *
     * taskID is the exact millisecond-precision timestamp when the task is launched.
     */
    class ForegroundTaskSubscriberFlow(val taskId: Long, inner: Flow<ForegroundTaskState>) :
        Flow<ForegroundTaskState> by inner

    /**
     * A cache of subscribers to 5 recently-launched foreground tasks, identified by ID
     *
     * Only one can be run at the same time, but those that are done will be kept in this
     * map for a little while -- because UI components may be stopped and recreated while
     * tasks are running. Having this buffer allows the components to re-subscribe even if
     * the task completes while they are being recreated.
     */
    private val foregroundTaskSubscribers: MutableMap<Long, SharedFlow<ForegroundTaskState>> =
        mutableMapOf()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return LocalBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (euiccChannelManagerDelegate.isInitialized()) {
            euiccChannelManager.invalidate()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId).also {
            lifecycleScope.launch {
                foregroundStarted.emit(Unit)
            }
        }
    }

    private fun ensureForegroundTaskNotificationChannel() {
        val nm = NotificationManagerCompat.from(this)
        if (nm.getNotificationChannelCompat(CHANNEL_ID) == null) {
            val channel =
                NotificationChannelCompat.Builder(
                    CHANNEL_ID,
                    NotificationManagerCompat.IMPORTANCE_LOW
                )
                    .setName(getString(R.string.task_notification))
                    .setVibrationEnabled(false)
                    .build()
            nm.createNotificationChannel(channel)
        }
    }

    private suspend fun updateForegroundNotification(title: String, iconRes: Int) {
        ensureForegroundTaskNotificationChannel()

        val nm = NotificationManagerCompat.from(this)
        val state = foregroundTaskState.value

        if (state is ForegroundTaskState.InProgress) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setProgress(100, state.progress, state.progress == 0)
                .setSmallIcon(iconRes)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()

            if (state.progress == 0) {
                startForeground(FOREGROUND_ID, notification)
            } else if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                nm.notify(FOREGROUND_ID, notification)
            }

            // Yield out so that the main looper can handle the notification event
            // Without this yield, the notification sent above will not be shown in time.
            yield()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun postForegroundTaskFailureNotification(title: String) {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_x_black)
            .build()
        NotificationManagerCompat.from(this).notify(TASK_FAILURE_ID, notification)
    }

    /**
     * Recover the subscriber to a foreground task that is recently launched.
     *
     * null if the task doesn't exist, or was launched too long ago.
     */
    fun recoverForegroundTaskSubscriber(taskId: Long): ForegroundTaskSubscriberFlow? =
        foregroundTaskSubscribers[taskId]?.let {
            ForegroundTaskSubscriberFlow(taskId, it.applyCompletionTransform())
        }

    /**
     * Launch a potentially blocking foreground task in this service's lifecycle context.
     * This function does not block, but returns a Flow that emits ForegroundTaskState
     * updates associated with this task. The last update the returned flow will emit is
     * always ForegroundTaskState.Done.
     *
     * The returned flow can only be subscribed to once even though the underlying implementation
     * is a SharedFlow. This is due to the need to apply transformations so that the stream
     * actually completes. In order to subscribe multiple times, use `recoverForegroundTaskSubscriber`
     * to acquire another instance.
     *
     * The task closure is expected to update foregroundTaskState whenever appropriate.
     * If a foreground task is already running, this function returns null.
     *
     * To wait for foreground tasks to be available, use waitForForegroundTask().
     *
     * The function will set the state back to Idle once it sees ForegroundTaskState.Done.
     */
    private fun launchForegroundTask(
        title: String,
        failureTitle: String,
        iconRes: Int,
        task: suspend EuiccChannelManagerService.() -> Unit
    ): ForegroundTaskSubscriberFlow {
        val taskID = System.currentTimeMillis()

        // Atomically set the state to InProgress. If this returns true, we are
        // the only task currently in progress.
        if (!foregroundTaskState.compareAndSet(
                ForegroundTaskState.Idle,
                ForegroundTaskState.InProgress(0)
            )
        ) {
            return ForegroundTaskSubscriberFlow(
                taskID,
                flow { emit(ForegroundTaskState.Done(IllegalStateException("There are tasks currently running"))) })
        }

        lifecycleScope.launch(Dispatchers.Main) {
            // Wait until our self-start command has succeeded.
            // We can only call startForeground() after that
            val res = withTimeoutOrNull(30 * 1000) {
                foregroundStarted.first()
            }

            if (res == null) {
                // The only case where the wait above could time out is if the subscriber
                // to the flow is stuck. Or we failed to start foreground.
                // In that case, we should just set our state back to Idle -- setting it
                // to Done wouldn't help much because nothing is going to then set it Idle.
                foregroundTaskState.value = ForegroundTaskState.Idle
                return@launch
            }

            updateForegroundNotification(title, iconRes)

            try {
                withContext(Dispatchers.IO + NonCancellable) { // Any LPA-related task must always complete
                    this@EuiccChannelManagerService.task()
                }
                // This update will be sent by the subscriber (as shown below)
                foregroundTaskState.value = ForegroundTaskState.Done(null)
            } catch (t: Throwable) {
                Log.e(TAG, "Foreground task encountered an error")
                Log.e(TAG, Log.getStackTraceString(t))
                foregroundTaskState.value = ForegroundTaskState.Done(t)

                if (isActive) {
                    postForegroundTaskFailureNotification(failureTitle)
                }
            } finally {
                if (isActive) {
                    stopSelf()
                }
            }
        }

        // This is the flow we are going to return. We allow multiple subscribers by
        // re-emitting state updates into this flow from another coroutine.
        // replay = 2 ensures that we at least have 1 previous state whenever subscribed to.
        // This is helpful when the task completed and is then re-subscribed to due to a
        // UI recreation event -- this way, the UI will know at least one last progress event
        // before completion / failure
        val subscriberFlow = MutableSharedFlow<ForegroundTaskState>(
            replay = 2,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        // We should be the only task running, so we can subscribe to foregroundTaskState
        // until we encounter ForegroundTaskState.Done.
        // Then, we complete the returned flow, but we also set the state back to Idle.
        // The state update back to Idle won't show up in the returned stream, because
        // it has been completed by that point.
        lifecycleScope.launch(Dispatchers.Main) {
            foregroundTaskState
                .applyCompletionTransform()
                .onEach {
                    // Also update our notification when we see an update
                    // But ignore the first progress = 0 update -- that is the current value.
                    // we need that to be handled by the main coroutine after it finishes.
                    if (it !is ForegroundTaskState.InProgress || it.progress != 0) {
                        updateForegroundNotification(title, iconRes)
                    }

                    subscriberFlow.emit(it)
                }
                .onCompletion {
                    // Reset state back to Idle when we are done.
                    // We do it here because otherwise Idle and Done might become conflated
                    // when emitted by the main coroutine in quick succession.
                    // Doing it here ensures we've seen Done. This Idle event won't be
                    // emitted to the consumer because the subscription has completed here.
                    foregroundTaskState.value = ForegroundTaskState.Idle
                }
                .collect()
        }

        foregroundTaskSubscribers[taskID] = subscriberFlow.asSharedFlow()

        if (foregroundTaskSubscribers.size > 5) {
            // Remove enough elements so that the size is kept at 5
            for (key in foregroundTaskSubscribers.keys.sorted()
                .take(foregroundTaskSubscribers.size - 5)) {
                foregroundTaskSubscribers.remove(key)
            }
        }

        // Before we return, and after we have set everything up,
        // self-start with foreground permission.
        // This is going to unblock the main coroutine handling the task.
        startForegroundService(
            Intent(
                this@EuiccChannelManagerService,
                this@EuiccChannelManagerService::class.java
            )
        )

        return ForegroundTaskSubscriberFlow(
            taskID,
            subscriberFlow.asSharedFlow().applyCompletionTransform()
        )
    }

    suspend fun waitForForegroundTask() {
        foregroundTaskState.takeWhile { it != ForegroundTaskState.Idle }
            .collect()
    }

    fun launchProfileDownloadTask(
        slotId: Int,
        portId: Int,
        smdp: String,
        matchingId: String?,
        confirmationCode: String?,
        imei: String?
    ): ForegroundTaskSubscriberFlow =
        launchForegroundTask(
            getString(R.string.task_profile_download),
            getString(R.string.task_profile_download_failure),
            R.drawable.ic_task_sim_card_download
        ) {
            euiccChannelManager.beginTrackedOperation(slotId, portId) {
                euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
                    channel.lpa.downloadProfile(
                        smdp,
                        matchingId,
                        imei,
                        confirmationCode,
                        object : ProfileDownloadCallback {
                            override fun onStateUpdate(state: ProfileDownloadCallback.DownloadState) {
                                if (state.progress == 0) return
                                foregroundTaskState.value =
                                    ForegroundTaskState.InProgress(state.progress)
                            }
                        })
                }

                preferenceRepository.notificationDownloadFlow.first()
            }
        }

    fun launchProfileRenameTask(
        slotId: Int,
        portId: Int,
        iccid: String,
        name: String
    ): ForegroundTaskSubscriberFlow =
        launchForegroundTask(
            getString(R.string.task_profile_rename),
            getString(R.string.task_profile_rename_failure),
            R.drawable.ic_task_rename
        ) {
            euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
                channel.lpa.setNickname(
                    iccid,
                    name
                )
            }
        }

    fun launchProfileDeleteTask(
        slotId: Int,
        portId: Int,
        iccid: String
    ): ForegroundTaskSubscriberFlow =
        launchForegroundTask(
            getString(R.string.task_profile_delete),
            getString(R.string.task_profile_delete_failure),
            R.drawable.ic_task_delete
        ) {
            euiccChannelManager.beginTrackedOperation(slotId, portId) {
                euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
                    channel.lpa.deleteProfile(iccid)
                }

                preferenceRepository.notificationDeleteFlow.first()
            }
        }

    class SwitchingProfilesRefreshException : Exception()

    fun launchProfileSwitchTask(
        slotId: Int,
        portId: Int,
        iccid: String,
        enable: Boolean, // Enable or disable the profile indicated in iccid
        reconnectTimeoutMillis: Long = 0 // 0 = do not wait for reconnect
    ): ForegroundTaskSubscriberFlow =
        launchForegroundTask(
            getString(R.string.task_profile_switch),
            getString(R.string.task_profile_switch_failure),
            R.drawable.ic_task_switch
        ) {
            euiccChannelManager.beginTrackedOperation(slotId, portId) {
                val (res, refreshed) = euiccChannelManager.withEuiccChannel(
                    slotId,
                    portId
                ) { channel ->
                    if (!channel.lpa.switchProfile(iccid, enable, refresh = true)) {
                        // Sometimes, we *can* enable or disable the profile, but we cannot
                        // send the refresh command to the modem because the profile somehow
                        // makes the modem "busy". In this case, we can still switch by setting
                        // refresh to false, but then the switch cannot take effect until the
                        // user resets the modem manually by toggling airplane mode or rebooting.
                        Pair(channel.lpa.switchProfile(iccid, enable, refresh = false), false)
                    } else {
                        Pair(true, true)
                    }
                }

                if (!res) {
                    throw RuntimeException("Could not switch profile")
                }

                if (!refreshed && slotId != EuiccChannelManager.USB_CHANNEL_ID) {
                    // We may have switched the profile, but we could not refresh. Tell the caller about this
                    // but only if we are talking to a modem and not a USB reader
                    throw SwitchingProfilesRefreshException()
                }

                if (reconnectTimeoutMillis > 0) {
                    // Add an unconditional delay first to account for any race condition between
                    // the card sending the refresh command and the modem actually refreshing
                    delay(reconnectTimeoutMillis / 10)

                    // This throws TimeoutCancellationException if timed out
                    euiccChannelManager.waitForReconnect(
                        slotId,
                        portId,
                        reconnectTimeoutMillis / 10 * 9
                    )
                }

                preferenceRepository.notificationSwitchFlow.first()
            }
        }

    fun launchMemoryReset(slotId: Int, portId: Int): ForegroundTaskSubscriberFlow =
        launchForegroundTask(
            getString(R.string.task_euicc_memory_reset),
            getString(R.string.task_euicc_memory_reset_failure),
            R.drawable.ic_euicc_memory_reset
        ) {
            euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
                channel.lpa.euiccMemoryReset()
            }
        }
}