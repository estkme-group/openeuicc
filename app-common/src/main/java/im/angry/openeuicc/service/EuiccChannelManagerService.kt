package im.angry.openeuicc.service

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import im.angry.openeuicc.common.R
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        private const val CHANNEL_ID = "tasks"
        private const val FOREGROUND_ID = 1000
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

    private fun updateForegroundNotification(title: String, iconRes: Int) {
        val channel =
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.task_notification))
                .setVibrationEnabled(false)
                .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)

        val state = foregroundTaskState.value

        if (state is ForegroundTaskState.InProgress) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setProgress(100, state.progress, state.progress == 0)
                .setSmallIcon(iconRes)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            if (state.progress == 0) {
                startForeground(FOREGROUND_ID, notification)
            } else if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(FOREGROUND_ID, notification)
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    /**
     * Launch a potentially blocking foreground task in this service's lifecycle context.
     * This function does not block, but returns a Flow that emits ForegroundTaskState
     * updates associated with this task. The last update the returned flow will emit is
     * always ForegroundTaskState.Done.
     *
     * The task closure is expected to update foregroundTaskState whenever appropriate.
     * If a foreground task is already running, this function returns null.
     *
     * The function will set the state back to Idle once it sees ForegroundTaskState.Done.
     */
    private fun launchForegroundTask(
        title: String,
        iconRes: Int,
        task: suspend EuiccChannelManagerService.() -> Unit
    ): Flow<ForegroundTaskState>? {
        // Atomically set the state to InProgress. If this returns true, we are
        // the only task currently in progress.
        if (!foregroundTaskState.compareAndSet(
                ForegroundTaskState.Idle,
                ForegroundTaskState.InProgress(0)
            )
        ) {
            return null
        }

        lifecycleScope.launch(Dispatchers.Main) {
            // Wait until our self-start command has succeeded.
            // We can only call startForeground() after that
            foregroundStarted.first()
            updateForegroundNotification(title, iconRes)

            try {
                withContext(Dispatchers.IO) {
                    this@EuiccChannelManagerService.task()
                }
                foregroundTaskState.value = ForegroundTaskState.Done(null)
            } catch (t: Throwable) {
                foregroundTaskState.value = ForegroundTaskState.Done(t)
            } finally {
                stopSelf()
            }

            updateForegroundNotification(title, iconRes)
        }

        // We 've launched the coroutine, now we can self-start
        // This is required in order to use startForeground()
        // This will end up calling onStartCommand(), which will emit
        // into foregroundStarted and unblock the coroutine above
        startForegroundService(Intent(this, this::class.java))

        // We should be the only task running, so we can subscribe to foregroundTaskState
        // until we encounter ForegroundTaskState.Done.
        // Then, we complete the returned flow, but we also set the state back to Idle.
        // The state update back to Idle won't show up in the returned stream, because
        // it has been completed by that point.
        return foregroundTaskState.transformWhile {
            // Also update our notification when we see an update
            withContext(Dispatchers.Main) {
                updateForegroundNotification(title, iconRes)
            }
            emit(it)
            it !is ForegroundTaskState.Done
        }.onCompletion { foregroundTaskState.value = ForegroundTaskState.Idle }
    }

    val isForegroundTaskRunning: Boolean
        get() = foregroundTaskState.value != ForegroundTaskState.Idle

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
    ): Flow<ForegroundTaskState>? =
        launchForegroundTask(
            getString(R.string.task_profile_download),
            R.drawable.ic_task_sim_card_download
        ) {
            euiccChannelManager.beginTrackedOperationBlocking(slotId, portId) {
                val channel = euiccChannelManager.findEuiccChannelByPort(slotId, portId)
                val res = channel!!.lpa.downloadProfile(
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

                if (!res) {
                    // TODO: Provide more details on the error
                    throw RuntimeException("Failed to download profile; this is typically caused by another error happened before.")
                }

                preferenceRepository.notificationDownloadFlow.first()
            }
        }
}