package im.angry.openeuicc.service

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.euicc.DownloadSubscriptionResult
import android.service.euicc.EuiccProfileInfo
import android.service.euicc.EuiccService
import android.service.euicc.GetDefaultDownloadableSubscriptionListResult
import android.service.euicc.GetDownloadableSubscriptionMetadataResult
import android.service.euicc.GetEuiccProfileInfoListResult
import android.telephony.TelephonyManager
import android.telephony.UiccAccessRule
import android.telephony.UiccSlotMapping
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccInfo
import android.util.Log
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.service.EuiccChannelManagerService.Companion.waitDone
import im.angry.openeuicc.service.EuiccChannelManagerService.ForegroundTaskHandle
import im.angry.openeuicc.service.EuiccChannelManagerService.ForegroundTaskState
import im.angry.openeuicc.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.ProfileClass
import net.typeblog.lpac_jni.ProfileDownloadInput
import net.typeblog.lpac_jni.ProfileDownloadState
import kotlin.time.Duration.Companion.milliseconds

class OpenEuiccService : EuiccService(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "OpenEuiccService"

        /**
         * The profile download that OpenEuiccService left waiting for confirmation in
         * EuiccChannelManagerService, if any, together with the activation code it was started
         * for. See pendingDownloadTask.
         */
        @Volatile
        private var pendingDownload: PendingDownload? = null

        /**
         * This lock is used in withEuiccManager() to serialize access to EuiccManager in this class.
         * See that method for details.
         */
        private val euiccLock = Mutex()

        /**
         * Cancel the download left waiting for confirmation, if there is one.
         *
         * It is assumed that the pending download is the one just declined; the platform is
         * unlikely to have started another order this quickly.
         */
        suspend fun cancelPendingDownload(euiccChannelManagerService: EuiccChannelManagerService) {
            euiccLock.withLock {
                pendingDownload?.let { pending ->
                    euiccChannelManagerService.recoverForegroundTaskSubscriber(pending.taskId)
                        ?.backChannel?.trySend(false)
                }
                pendingDownload = null
            }
        }
    }

    // A download task left in EuiccChannelManagerService waiting for confirmation.
    // LPAString is remembered to prevent reuse across different download sessions however unlikely.
    // Confirmation code is not included here since it does not change the identity of the profile being downloaded,
    // and could be supplied later.
    private data class PendingDownload(val taskId: Long, val lpaString: LPAString)

    private val seId = EuiccChannel.SecureElementId.DEFAULT

    private val hasInternalEuicc by lazy {
        telephonyManager.uiccCardsInfoCompat.any { it.isEuicc && !it.isRemovable }
    }

    // TODO: Should this be configurable?
    private fun shouldIgnoreSlot(physicalSlotId: Int) =
        if (hasInternalEuicc) {
            // For devices with an internal eUICC slot, ignore any removable UICC
            telephonyManager.uiccCardsInfoCompat.find { it.physicalSlotIndex == physicalSlotId }!!.isRemovable
        } else {
            // Otherwise, we can report at least one removable eUICC to the system without confusing
            // it too much.
            telephonyManager.uiccCardsInfoCompat.firstOrNull { it.isEuicc }?.physicalSlotIndex != physicalSlotId
        }

    private data class EuiccChannelManagerContext(
        val euiccChannelManagerService: EuiccChannelManagerService
    ) {
        val euiccChannelManager
            get() = euiccChannelManagerService.euiccChannelManager
    }

    /**
     * Bind to EuiccChannelManagerService, run the callback with a EuiccChannelManager instance,
     * and then unbind after the callback is finished. All methods in this class that require access
     * to a EuiccChannelManager should be wrapped inside this call.
     *
     * This ensures that we only spawn and connect to APDU channels when we absolutely need to,
     * instead of keeping them open unnecessarily in the background at all times.
     *
     * This function also serializes all access to EuiccChannelManager from this class with a mutex,
     * and it also ensures that when it calls fn(), no other foreground tasks managed by EuiccChannelManagerService
     * are running. This is currently a massive hack and requires fragile interaction between the two systems,
     * and is EXTREMELY hacky. THIS SHALL BE REPLACED AS SOON AS POSSIBLE.
     *
     * This function cannot be inline because non-local returns may bypass to unbind
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> withEuiccChannelManager(
        waitForDownload: Boolean = true,
        fn: suspend EuiccChannelManagerContext.() -> T
    ): T {
        val (binder, unbind) = runBlocking {
            bindServiceSuspended(
                Intent(this@OpenEuiccService, EuiccChannelManagerService::class.java),
                BIND_AUTO_CREATE
            )
        }

        if (binder == null) {
            throw RuntimeException("Unable to bind to EuiccChannelManagerService; aborting")
        }

        val localBinder = binder as EuiccChannelManagerService.LocalBinder

        val ret = try {
            runBlocking {
                // Wrapped in Result so that a legitimate null returned by fn() (e.g. onGetEid on
                // an unmapped slot) is not confused with "fn() has not run yet".
                var result: kotlin.Result<T>? = null
                // THIS IS WHERE I GO TO HELL FOR KEEPING THIS HORRIBLE SECTION OF CODE.
                // The main complexity here is that we need to serialize on both the mutex _in this class_,
                // and also on the foreground task system managed by EuiccChannelManagerService.
                // If any other method is currently accessing EuiccChannelManager, OR if any foreground task
                // is running, even if they're not launched from this service, we must not allow the callback
                // to run.
                while (true) {
                    val task = euiccLock.withLock {
                        if (waitForDownload) {
                            val pending = pendingDownload
                            val task = pending?.taskId?.let {
                                localBinder.service.recoverForegroundTaskSubscriber(it)?.takeIf { handle ->
                                    // This is okay for a similar reason as pendingDownloadTask().
                                    !handle.backChannel.isClosedForSend
                                }
                            }
                            if (task == null) {
                                // The task is gone or has already finished
                                if (pending != null) pendingDownload = null
                                result =
                                    runCatching { EuiccChannelManagerContext(localBinder.service).fn() }
                            }
                            task
                        } else {
                            result =
                                runCatching { EuiccChannelManagerContext(localBinder.service).fn() }
                            null
                        }
                    }
                    if (task == null) break
                    task.stateFlow.waitDone()
                }
                // Always assigned by the time the loop breaks
                checkNotNull(result).getOrThrow()
            }
        } finally {
            unbind()
        }

        return ret
    }

    override fun onGetEid(slotId: Int): String? = withEuiccChannelManager {
        val portId = euiccChannelManager.findFirstAvailablePort(slotId)
        if (portId < 0) return@withEuiccChannelManager null
        euiccChannelManager.withEuiccChannel(slotId, portId, seId) { channel ->
            channel.lpa.eID
        }
    }

    private fun ensurePortIsMapped(slotId: Int, portId: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val mappings = telephonyManager.simSlotMapping.toMutableList()

        mappings.firstOrNull { it.physicalSlotIndex == slotId && it.portIndex == portId }?.let {
            throw IllegalStateException("Slot $slotId port $portId has already been mapped")
        }

        val idx = mappings.indexOfFirst { it.physicalSlotIndex != slotId || it.portIndex != portId }
        if (idx >= 0) {
            mappings[idx] = UiccSlotMapping(portId, slotId, mappings[idx].logicalSlotIndex)
        }

        mappings.firstOrNull { it.physicalSlotIndex == slotId && it.portIndex == portId } ?: run {
            throw IllegalStateException("Cannot map slot $slotId port $portId")
        }

        try {
            telephonyManager.simSlotMapping = mappings
            return
        } catch (_: Exception) {
            // ignore
        }

        // Sometimes hardware supports one ordering but not the reverse
        telephonyManager.simSlotMapping = mappings.reversed()
    }

    private suspend fun <T> retryWithTimeout(
        timeoutMillis: Int,
        backoff: Int = 1000,
        f: suspend () -> T?
    ): T? {
        val startTimeMillis = System.currentTimeMillis()
        do {
            try {
                f()?.let { return@retryWithTimeout it }
            } catch (_: Exception) {
                // Ignore
            } finally {
                delay(backoff.toLong().milliseconds)
            }
        } while (System.currentTimeMillis() - startTimeMillis < timeoutMillis)
        return null
    }

    override fun onGetOtaStatus(slotId: Int): Int {
        // Not implemented
        return 5 // EUICC_OTA_STATUS_UNAVAILABLE
    }

    override fun onStartOtaIfNecessary(
        slotId: Int,
        statusChangedCallback: OtaStatusChangedCallback?
    ) {
        // Not implemented
    }

    private fun DownloadableSubscription.parseActivationCode(): LPAString? =
        encodedActivationCode?.let { runCatching { LPAString.parse(it) }.getOrNull() }

    // Wait for a download task to reach ConfirmingDownload; null if it failed before
    private suspend fun ForegroundTaskHandle.awaitConfirmingDownload() =
        // FIXME: This only awaits for ConfirmingDownload, but not later states. What happens if we happen to
        //        race against the task in a way that makes it go past that state when we get here?
        //        It should probably not happen normally since Android should not launch another download task
        //        before it learns about teh completion of a previous one, but we probably need to address this.
        stateFlow.first {
            it is ForegroundTaskState.Done ||
                    (it as? ForegroundTaskState.InProgress)?.context is ProfileDownloadState.ConfirmingDownload
        }.let { (it as? ForegroundTaskState.InProgress)?.context as? ProfileDownloadState.ConfirmingDownload }

    // Returns the download task left pending by the metadata fetch call waiting for confirmation to
    // finish, but only if it was started for the same activation code.
    // Call sites should probably not expect a pending download task to always exist.
    // If it does not, you should probably just launch a new one via launchAndRememberDownloadTask,
    // which also cancels any pending download task for other sessions.
    @OptIn(DelicateCoroutinesApi::class)
    private fun EuiccChannelManagerContext.pendingDownloadTask(lpaString: LPAString): ForegroundTaskHandle? {
        val pending = pendingDownload ?: return null
        if (pending.lpaString != lpaString) {
            // A task is pending, but for another activation code. Leave it remembered so that
            // launchAndRememberDownloadTask cancels it; just do not reuse it here.
            return null
        }
        return euiccChannelManagerService.recoverForegroundTaskSubscriber(pending.taskId)?.takeIf {
            // Technically this is racy because the backChannel could be closed as we are
            // checking this status, but that is unlikely, and even if it happened, we would
            // just return a task handle that will emit an error (or Done). The user may see
            // a transient error and be asked to try again.
            !it.backChannel.isClosedForSend
        } ?: run {
            // Lazily clear the remembered task now that we know it is gone (or no longer waiting).
            // As its name suggests this variable and this function only cares about tasks left over by the
            // metadata fetch function.
            pendingDownload = null
            null
        }
    }

    /**
     * Launch a profile download task based on an existing bound EuiccChannelManager,
     * and remember it as pending for future confirmation.
     *
     * This function is almost entirely for the purpose of onGetDownloadableSubscriptionMetadata.
     * To acquire profile metadata, we must start a downlaod session, but onGetDownloadableSubscriptionMetadata
     * also must return once metadata is fetched. So it leaves two options: you either cancel the session
     * at that point, which, on many SM-DP+ servers, will also invalidate the matching code; or, you
     * keep the session around somehow and reuse it when the user finally proceeds to download.
     *
     * We keep the download task running in background for up to 5 minutes. This should be more than enough
     * for "normal" download sessions.
     *
     * Of course, it is also possible to launch a download task directly as part of onDownloadSubscription.
     * That probably does not happen except for privileged caller apps (system privileged)/
     *
     * Returns null if the caller should return RESULT_MUST_DEACTIVATE_SIM.
     */
    private suspend fun EuiccChannelManagerContext.launchAndRememberDownloadTask(
        slotId: Int,
        portIndex: Int,
        lpaString: LPAString,
        confirmationCode: String?,
        forceDeactivateSim: Boolean
    ): ForegroundTaskHandle? {
        // Only one task can run at a time; don't wait until another waiting download times out.
        pendingDownload?.let { pending ->
            euiccChannelManagerService.recoverForegroundTaskSubscriber(pending.taskId)
                ?.backChannel?.trySend(false)
        }
        pendingDownload = null

        val port = euiccChannelManager.findFirstAvailablePort(slotId).takeIf { it >= 0 } ?: run {
            if (!forceDeactivateSim) return null
            val port = if (portIndex >= 0) portIndex else 0
            ensurePortIsMapped(slotId, port)
            retryWithTimeout(5000) {
                euiccChannelManager.withEuiccChannel(slotId, port, seId) { channel ->
                    if (!channel.valid) {
                        throw IllegalStateException("Slot $slotId port $port is unavailable; may need to try again")
                    }
                    port
                }
            } ?: throw IllegalStateException("Slot $slotId port $port did not become available")
        }
        val imei = euiccChannelManager.withEuiccChannel(slotId, port, seId) { channel ->
            runCatching { telephonyManager.getImei(channel.logicalSlotId) }.getOrNull()
        }

        euiccChannelManagerService.waitForForegroundTask()
        val handle = euiccChannelManagerService.launchProfileDownloadTask(
            slotId,
            port,
            seId,
            ProfileDownloadInput(
                lpaString.address,
                lpaString.matchingId,
                imei,
                confirmationCode
            ),
            // Leaves time for the user to consent or enter a confirmation code in between
            5 * 60 * 1000
        )
        pendingDownload = PendingDownload(handle.taskId, lpaString)
        return handle
    }

    override fun onGetDownloadableSubscriptionMetadata(
        slotId: Int,
        subscription: DownloadableSubscription?,
        forceDeactivateSim: Boolean
    ): GetDownloadableSubscriptionMetadataResult =
        if (subscription == null) {
            GetDownloadableSubscriptionMetadataResult(RESULT_FIRST_USER, null)
        } else {
            // -1 = any port
            onGetDownloadableSubscriptionMetadata(slotId, -1, subscription, forceDeactivateSim)
        }

    override fun onGetDownloadableSubscriptionMetadata(
        slotId: Int,
        portIndex: Int,
        subscription: DownloadableSubscription,
        forceDeactivateSim: Boolean
    ): GetDownloadableSubscriptionMetadataResult = withEuiccChannelManager(waitForDownload = false) {
        Log.i(
            TAG,
            "onGetDownloadableSubscriptionMetadata slotId=$slotId portIndex=$portIndex forceDeactivateSim=$forceDeactivateSim"
        )
        val lpaString = subscription.parseActivationCode()
        if (shouldIgnoreSlot(slotId) || lpaString == null) {
            return@withEuiccChannelManager GetDownloadableSubscriptionMetadataResult(
                RESULT_FIRST_USER,
                null
            )
        }

        val confirming = try {
            // Either reuse a pending download task we launched before or create a new one.
            // As described in launchAndRememberDownloadTask, we must start to download in order to fetch metadata,
            // and we can't safely cancel the task in the general case for arbitrary SM-DP+ servers, so we need
            // to memorize the task to continue later.
            // It's possible that the system may call this function again for the same profile, so
            // we do need to check pendingDownloadTask.
            (pendingDownloadTask(lpaString)
                ?: launchAndRememberDownloadTask(
                    slotId,
                    portIndex,
                    lpaString,
                    subscription.confirmationCode?.ifEmpty { null },
                    forceDeactivateSim
                )
                ?: return@withEuiccChannelManager GetDownloadableSubscriptionMetadataResult(
                    RESULT_MUST_DEACTIVATE_SIM,
                    null
                )).awaitConfirmingDownload()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile metadata", e)
            null
        } ?: return@withEuiccChannelManager GetDownloadableSubscriptionMetadataResult(
            RESULT_FIRST_USER,
            null
        )

        val builder = DownloadableSubscription.Builder(subscription)
        confirming.metadata?.let { metadata ->
            if (metadata.providerName.isNotBlank()) {
                builder.setCarrierName(metadata.providerName)
            }
            if (metadata.accessRules.isNotEmpty()) {
                builder.setAccessRules(metadata.accessRules.map {
                    UiccAccessRule(it.certificateHash.decodeHex(), it.packageName, 0)
                })
            }
        }
        GetDownloadableSubscriptionMetadataResult(RESULT_OK, builder.build())
    }

    override fun onDownloadSubscription(
        slotIndex: Int,
        portIndex: Int,
        subscription: DownloadableSubscription,
        switchAfterDownload: Boolean,
        forceDeactivateSim: Boolean,
        resolvedBundle: Bundle
    ): DownloadSubscriptionResult {
        val (downloadResult, iccid) = withEuiccChannelManager(waitForDownload = false) {
            Log.i(
                TAG,
                "onDownloadSubscription slotIndex=$slotIndex portIndex=$portIndex switchAfterDownload=$switchAfterDownload forceDeactivateSim=$forceDeactivateSim " +
                        "callingPackage=${resolvedBundle.getString("android.service.euicc.extra.PACKAGE_NAME")}"
            )

            val lpaString = subscription.parseActivationCode()
            if (shouldIgnoreSlot(slotIndex) || lpaString == null) {
                return@withEuiccChannelManager Pair(
                    DownloadSubscriptionResult(RESULT_FIRST_USER, 0, TelephonyManager.UNSUPPORTED_CARD_ID),
                    null
                )
            }

            // FIXME: I'm not sure this is entirely correct since it is possible that we only knew about
            //        the confirmation code when the server returned an error. We need to handle that case
            //        as well.
            if (lpaString.confirmationCodeRequired && subscription.confirmationCode.isNullOrEmpty()) {
                return@withEuiccChannelManager Pair(
                    DownloadSubscriptionResult(
                        RESULT_RESOLVABLE_ERRORS,
                        RESOLVABLE_ERROR_CONFIRMATION_CODE,
                        TelephonyManager.UNSUPPORTED_CARD_ID
                    ), null
                )
            }

            val cardId = telephonyManager.uiccCardsInfoCompat.find { it.physicalSlotIndex == slotIndex }
                ?.cardId ?: TelephonyManager.UNSUPPORTED_CARD_ID

            suspend fun confirm(handle: ForegroundTaskHandle) =
                handle.awaitConfirmingDownload()?.takeIf {
                    it.confirmationCode = subscription.confirmationCode?.ifEmpty { null }
                    // Fails if the download is not waiting anymore, e.g. timed out
                    handle.backChannel.trySend(true).isSuccess
                }

            // Confirm the download onGetDownloadableSubscriptionMetadata() left waiting, or a new
            // one (it's possible to skip the get metadata step depending on caller privilege)
            val waiting = pendingDownloadTask(lpaString)
            val (handle, confirming) = waiting?.let { h -> confirm(h)?.let { Pair(h, it) } } ?: try {
                launchAndRememberDownloadTask(
                    slotIndex, portIndex, lpaString,
                    subscription.confirmationCode?.ifEmpty { null }, forceDeactivateSim
                )
                    ?.let { Pair(it, confirm(it)) }
                    ?: return@withEuiccChannelManager Pair(
                        DownloadSubscriptionResult(RESULT_MUST_DEACTIVATE_SIM, 0, cardId), null
                    )
            } catch (e: Exception) {
                Log.e(TAG, "Could not start the profile download", e)
                return@withEuiccChannelManager Pair(DownloadSubscriptionResult(RESULT_FIRST_USER, 0, cardId), null)
            }

            val error = handle.stateFlow.waitDone()
            pendingDownload = null
            if (error != null) {
                Log.e(TAG, "Profile download failed", error)
                Pair(DownloadSubscriptionResult(RESULT_FIRST_USER, 0, cardId), null)
            } else {
                Pair(DownloadSubscriptionResult(RESULT_OK, 0, cardId), confirming?.metadata?.iccid)
            }
        }

        if (downloadResult.result != RESULT_OK || !switchAfterDownload) return downloadResult

        // Calling onSwitchToSubscriptionWithPort must happen outside of withEuiccChannelManager because
        // of the lock inside of that function.
        if (iccid.isNullOrEmpty() ||
            onSwitchToSubscriptionWithPort(slotIndex, portIndex, iccid, true) != RESULT_OK
        ) {
            Log.e(TAG, "Profile $iccid downloaded but not enabled; it has to be enabled manually")
            // The platform only refreshes its list after the switch
            appContainer.subscriptionManager.tryRefreshCachedEuiccInfo(downloadResult.cardId)
        }
        return downloadResult
    }

    override fun onGetDefaultDownloadableSubscriptionList(
        slotId: Int,
        forceDeactivateSim: Boolean
    ): GetDefaultDownloadableSubscriptionListResult {
        // Stub: we do not implement this (as this would require phoning in a central GSMA server)
        return GetDefaultDownloadableSubscriptionListResult(RESULT_OK, arrayOf())
    }

    override fun onGetEuiccProfileInfoList(slotId: Int): GetEuiccProfileInfoListResult =
        withEuiccChannelManager {
            Log.i(TAG, "onGetEuiccProfileInfoList slotId=$slotId")
            if (slotId == -1 || shouldIgnoreSlot(slotId)) {
                Log.i(TAG, "ignoring slot $slotId")
                return@withEuiccChannelManager GetEuiccProfileInfoListResult(
                    RESULT_FIRST_USER,
                    arrayOf(),
                    true
                )
            }

            val port = euiccChannelManager.findFirstAvailablePort(slotId)
            if (port == -1) {
                return@withEuiccChannelManager GetEuiccProfileInfoListResult(
                    RESULT_FIRST_USER,
                    arrayOf(),
                    true
                )
            }

            return@withEuiccChannelManager try {
                euiccChannelManager.withEuiccChannel(slotId, port, seId) { channel ->
                    val filteredProfiles =
                        if (preferenceRepository.unfilteredProfileListFlow.first())
                            channel.lpa.profiles
                        else
                            channel.lpa.profiles.operational
                    val profiles = filteredProfiles.map {
                        EuiccProfileInfo.Builder(it.iccid).apply {
                            setProfileName(it.name)
                            setNickname(it.displayName)
                            setServiceProviderName(it.providerName)
                            setState(
                                when (it.state) {
                                    LocalProfileInfo.State.Enabled -> EuiccProfileInfo.PROFILE_STATE_ENABLED
                                    LocalProfileInfo.State.Disabled -> EuiccProfileInfo.PROFILE_STATE_DISABLED
                                }
                            )
                            setProfileClass(
                                when (it.profileClass) {
                                    ProfileClass.Testing -> EuiccProfileInfo.PROFILE_CLASS_TESTING
                                    ProfileClass.Provisioning -> EuiccProfileInfo.PROFILE_CLASS_PROVISIONING
                                    ProfileClass.Operational -> EuiccProfileInfo.PROFILE_CLASS_OPERATIONAL
                                }
                            )
                        }.build()
                    }

                    GetEuiccProfileInfoListResult(
                        RESULT_OK,
                        profiles.toTypedArray(),
                        channel.port.card.isRemovable
                    )
                }
            } catch (_: EuiccChannelManager.EuiccChannelNotFoundException) {
                GetEuiccProfileInfoListResult(
                    RESULT_FIRST_USER,
                    arrayOf(),
                    true
                )
            }
        }

    override fun onGetEuiccInfo(slotId: Int): EuiccInfo {
        return EuiccInfo("Unknown") // TODO: Can we actually implement this?
    }

    override fun onDeleteSubscription(slotId: Int, iccid: String): Int = withEuiccChannelManager {
        Log.i(TAG, "onDeleteSubscription slotId=$slotId iccid=$iccid")
        if (shouldIgnoreSlot(slotId)) return@withEuiccChannelManager RESULT_FIRST_USER

        val ports = euiccChannelManager.findAvailablePorts(slotId)
        if (ports.isEmpty()) return@withEuiccChannelManager RESULT_FIRST_USER

        // Check that the profile has been disabled on all slots
        val enabledAnywhere = ports.any { port ->
            euiccChannelManager.withEuiccChannel(slotId, port, seId) { channel ->
                channel.lpa.profiles.enabled?.iccid == iccid
            }
        }

        if (enabledAnywhere) return@withEuiccChannelManager RESULT_FIRST_USER

        euiccChannelManagerService.waitForForegroundTask()
        val success = euiccChannelManagerService.launchProfileDeleteTask(
            slotId,
            ports[0],
            EuiccChannel.SecureElementId.DEFAULT,
            iccid
        )
            .stateFlow.waitDone() == null

        return@withEuiccChannelManager if (success) {
            RESULT_OK
        } else {
            RESULT_FIRST_USER
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onSwitchToSubscription(
        slotId: Int,
        iccid: String?,
        forceDeactivateSim: Boolean
    ): Int =
        // -1 = any port
        onSwitchToSubscriptionWithPort(slotId, -1, iccid, forceDeactivateSim)

    override fun onSwitchToSubscriptionWithPort(
        slotId: Int,
        portIndex: Int,
        iccid: String?,
        forceDeactivateSim: Boolean
    ): Int = withEuiccChannelManager {
        Log.i(
            TAG,
            "onSwitchToSubscriptionWithPort slotId=$slotId portIndex=$portIndex iccid=$iccid forceDeactivateSim=$forceDeactivateSim"
        )
        if (shouldIgnoreSlot(slotId)) return@withEuiccChannelManager RESULT_FIRST_USER

        try {
            // First, try to find a pair of slotId and portId we can use for the switching operation
            // retryWithTimeout is needed here because this function may be called just after
            // AOSP has switched slot mappings, in which case the slots may not be ready yet.
            val (foundSlotId, foundPortId) = retryWithTimeout(5000) {
                if (portIndex == -1) {
                    // If port is not indicated, we can use any port
                    val port = euiccChannelManager.findFirstAvailablePort(slotId).let {
                        if (it < 0) {
                            throw IllegalStateException("No mapped port available; may need to try again")
                        }

                        it
                    }

                    Pair(slotId, port)
                } else {
                    // Else, check until the indicated port is available
                    euiccChannelManager.withEuiccChannel(slotId, portIndex, seId) { channel ->
                        if (!channel.valid) {
                            throw IllegalStateException("Indicated slot / port combination is unavailable; may need to try again")
                        }
                    }

                    Pair(slotId, portIndex)
                }
            } ?: run {
                // Failure case: mapped slots / ports aren't usable per constraints
                // If we can't find a usable slot / port already mapped, and we aren't allowed to
                // deactivate a SIM, we can only abort
                if (!forceDeactivateSim) {
                    return@withEuiccChannelManager RESULT_MUST_DEACTIVATE_SIM
                }

                // If port ID is not indicated, we just try to map port 0
                // This is because in order to get here, we have to have failed findFirstAvailablePort(),
                // which means no eUICC port is mapped or connected properly whatsoever.
                val foundPortId = if (portIndex == -1) {
                    0
                } else {
                    portIndex
                }

                // Now we can try to map an unused port
                try {
                    ensurePortIsMapped(slotId, foundPortId)
                } catch (_: Exception) {
                    return@withEuiccChannelManager RESULT_FIRST_USER
                }

                // Wait for availability again
                retryWithTimeout(5000) {
                    euiccChannelManager.withEuiccChannel(slotId, foundPortId, seId) { channel ->
                        if (!channel.valid) {
                            throw IllegalStateException("Indicated slot / port combination is unavailable; may need to try again")
                        }
                    }
                } ?: return@withEuiccChannelManager RESULT_FIRST_USER

                Pair(slotId, foundPortId)
            }

            Log.i(TAG, "Found slotId=$foundSlotId, portId=$foundPortId for switching")

            // Now, figure out what they want us to do: disabling a profile, or enabling a new one?
            val (foundIccid, enable) = if (iccid == null) {
                // iccid == null means disabling
                val foundIccid =
                    euiccChannelManager.withEuiccChannel(foundSlotId, foundPortId, seId) { channel ->
                        channel.lpa.profiles.enabled?.iccid
                    } ?: return@withEuiccChannelManager RESULT_FIRST_USER
                Pair(foundIccid, false)
            } else {
                Pair(iccid, true)
            }

            val res = euiccChannelManagerService.launchProfileSwitchTask(
                foundSlotId,
                foundPortId,
                EuiccChannel.SecureElementId.DEFAULT,
                foundIccid,
                enable,
                30 * 1000
            ).stateFlow.waitDone()

            if (res != null) {
                Log.e(TAG, "Profile switch task failed (iccid=$foundIccid enable=$enable)", res)
                return@withEuiccChannelManager RESULT_FIRST_USER
            }

            return@withEuiccChannelManager RESULT_OK
        } catch (e: Exception) {
            Log.e(TAG, "onSwitchToSubscriptionWithPort failed", e)
            return@withEuiccChannelManager RESULT_FIRST_USER
        } finally {
            euiccChannelManager.invalidate()
        }
    }

    override fun onUpdateSubscriptionNickname(slotId: Int, iccid: String, nickname: String?): Int =
        withEuiccChannelManager {
            Log.i(
                TAG,
                "onUpdateSubscriptionNickname slotId=$slotId iccid=$iccid nickname=$nickname"
            )
            if (shouldIgnoreSlot(slotId)) return@withEuiccChannelManager RESULT_FIRST_USER
            val port = euiccChannelManager.findFirstAvailablePort(slotId)
            if (port < 0) {
                return@withEuiccChannelManager RESULT_FIRST_USER
            }

            euiccChannelManagerService.waitForForegroundTask()
            val success =
                (euiccChannelManagerService.launchProfileRenameTask(
                    slotId,
                    port,
                    EuiccChannel.SecureElementId.DEFAULT,
                    iccid,
                    nickname!!
                )
                    .stateFlow.waitDone()) == null

            euiccChannelManager.withEuiccChannel(slotId, port, seId) { channel ->
                appContainer.subscriptionManager.tryRefreshCachedEuiccInfo(channel.cardId)
            }
            return@withEuiccChannelManager if (success) {
                RESULT_OK
            } else {
                RESULT_FIRST_USER
            }
        }

    @Deprecated("Deprecated in Java")
    override fun onEraseSubscriptions(slotId: Int): Int {
        // No-op
        return RESULT_FIRST_USER
    }

    override fun onRetainSubscriptionsForFactoryReset(slotId: Int): Int {
        // No-op -- we do not care
        return RESULT_FIRST_USER
    }
}
