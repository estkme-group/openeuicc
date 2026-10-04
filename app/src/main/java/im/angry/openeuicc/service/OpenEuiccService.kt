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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.typeblog.lpac_jni.LocalProfileInfo
import net.typeblog.lpac_jni.ProfileClass
import net.typeblog.lpac_jni.ProfileDownloadInput
import net.typeblog.lpac_jni.ProfileDownloadState

class OpenEuiccService : EuiccService(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "OpenEuiccService"

        /**
         * ID of the download task that OpenEuiccService left waiting for confirmation in
         * EuiccChannelManagerService, if any. Only one download can be running at a time, so a
         * single ID is enough. Remembered here (rather than in EuiccChannelManagerService) because
         * only OpenEuiccService -- and EuiccResolutionActivity, which runs in the same process --
         * need it; it is cleared lazily when the task is gone or has completed.
         */
        @Volatile
        var runningDownloadTaskId: Long? = null
    }

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
     * It also serializes EuiccService methods that touch the eUICC on the service instance: the
     * platform may call them concurrently from different binder threads, and without this a channel
     * invalidated by one method (e.g. after a profile switch) could be closed under another method
     * that is still using it. By default it also waits for a running download task to finish first,
     * so that the eUICC is quiescent when the method runs (e.g. a fresh profile list) -- only the
     * download methods themselves opt out, because they interact with the running download task.
     * The wait happens without holding the lock, so that onDownloadSubscription() can still confirm
     * the download and unblock the waiters.
     *
     * This function cannot be inline because non-local returns may bypass to unbind
     */
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

        if (waitForDownload) {
            runningDownloadTaskId?.let { taskId ->
                localBinder.service.recoverForegroundTaskSubscriber(taskId)
                    ?.stateFlow?.let { runBlocking { it.waitDone() } }
            }
        }

        val ret = synchronized(this@OpenEuiccService) {
            runBlocking {
                EuiccChannelManagerContext(localBinder.service).fn()
            }
        }

        unbind()
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
                delay(backoff.toLong())
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
        stateFlow.first {
            it is ForegroundTaskState.Done ||
                    (it as? ForegroundTaskState.InProgress)?.context is ProfileDownloadState.ConfirmingDownload
        }.let { (it as? ForegroundTaskState.InProgress)?.context as? ProfileDownloadState.ConfirmingDownload }

    /**
     * The download task OpenEuiccService left waiting at ConfirmingDownload, if it is still
     * waiting. Clears the remembered ID when the task is gone or has already left the waiting
     * step (its backChannel is closed once it does), so that a later call can start a new one.
     */
    private fun EuiccChannelManagerContext.currentDownloadTask(): ForegroundTaskHandle? {
        val taskId = runningDownloadTaskId ?: return null
        return euiccChannelManagerService.recoverForegroundTaskSubscriber(taskId)?.takeIf {
            !it.backChannel.isClosedForSend
        } ?: run {
            runningDownloadTaskId = null
            null
        }
    }

    /**
     * The profile metadata is only available within a download session (from
     * ES9+.AuthenticateClient on), and many SM-DP+ servers refuse a second session for the same
     * order. So onGetDownloadableSubscriptionMetadata() launches the download, which then waits at
     * ConfirmingDownload until onDownloadSubscription() confirms it, the user declines
     * (EuiccResolutionActivity) or it times out. The service remembers the task's ID so that the
     * later calls can find the same session again (only one download can run at a time).
     * Meanwhile, it holds the eUICC: other methods wait for it via withEuiccChannelManager.
     *
     * Returns null if the caller should return RESULT_MUST_DEACTIVATE_SIM.
     */
    private suspend fun EuiccChannelManagerContext.launchAndRememberDownloadTask(
        slotId: Int,
        portIndex: Int,
        lpaString: LPAString,
        subscription: DownloadableSubscription,
        forceDeactivateSim: Boolean
    ): ForegroundTaskHandle? {
        // Only one task can run at a time; don't wait until another waiting download times out
        runningDownloadTaskId?.let { taskId ->
            euiccChannelManagerService.recoverForegroundTaskSubscriber(taskId)
                ?.backChannel?.trySend(false)
        }
        runningDownloadTaskId = null

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
                subscription.confirmationCode?.ifEmpty { null }
            ),
            // Leaves time for the user to consent or enter a confirmation code in between
            5 * 60 * 1000
        )
        runningDownloadTaskId = handle.taskId
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
            // The platform asks again after the user entered a confirmation code, while the
            // download is still waiting
            (currentDownloadTask()
                ?: launchAndRememberDownloadTask(slotId, portIndex, lpaString, subscription, forceDeactivateSim)
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

        // The platform checks the calling (carrier) app against the access rules
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
    ): DownloadSubscriptionResult = withEuiccChannelManager(waitForDownload = false) {
        Log.i(
            TAG,
            "onDownloadSubscription slotIndex=$slotIndex portIndex=$portIndex switchAfterDownload=$switchAfterDownload forceDeactivateSim=$forceDeactivateSim " +
                    "callingPackage=${resolvedBundle.getString("android.service.euicc.extra.PACKAGE_NAME")}"
        )

        fun result(code: Int, resolvableErrors: Int = 0, cardId: Int = TelephonyManager.UNSUPPORTED_CARD_ID) =
            DownloadSubscriptionResult(code, resolvableErrors, cardId)

        val lpaString = subscription.parseActivationCode()
        if (shouldIgnoreSlot(slotIndex) || lpaString == null) return@withEuiccChannelManager result(RESULT_FIRST_USER)

        if (lpaString.confirmationCodeRequired && subscription.confirmationCode.isNullOrEmpty()) {
            // The platform asks the user for the code, then requests the metadata and the download
            // again. The waiting download keeps waiting until then.
            return@withEuiccChannelManager result(RESULT_RESOLVABLE_ERRORS, RESOLVABLE_ERROR_CONFIRMATION_CODE)
        }

        val cardId = telephonyManager.uiccCardsInfoCompat.find { it.physicalSlotIndex == slotIndex }
            ?.cardId ?: TelephonyManager.UNSUPPORTED_CARD_ID

        suspend fun confirm(handle: ForegroundTaskHandle) =
            handle.awaitConfirmingDownload()?.takeIf {
                it.confirmationCode = subscription.confirmationCode?.ifEmpty { null }
                // Fails if the download is not waiting any more, e.g. timed out
                handle.backChannel.trySend(true).isSuccess
            }

        // Confirm the download onGetDownloadableSubscriptionMetadata() left waiting, or a new
        // one (privileged callers skip the metadata step)
        val waiting = currentDownloadTask()
        val (handle, confirming) = waiting?.let { h -> confirm(h)?.let { Pair(h, it) } } ?: try {
            launchAndRememberDownloadTask(slotIndex, portIndex, lpaString, subscription, forceDeactivateSim)
                ?.let { Pair(it, confirm(it)) }
                ?: return@withEuiccChannelManager result(RESULT_MUST_DEACTIVATE_SIM)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start the profile download", e)
            return@withEuiccChannelManager result(RESULT_FIRST_USER)
        }

        val error = handle.stateFlow.waitDone()
        runningDownloadTaskId = null
        val (downloadResult, iccid) = if (error != null) {
            Log.e(TAG, "Profile download failed", error)
            Pair(result(RESULT_FIRST_USER, cardId = cardId), null)
        } else {
            Pair(result(RESULT_OK, cardId = cardId), confirming?.metadata?.iccid)
        }

        if (downloadResult.result != RESULT_OK || !switchAfterDownload) return@withEuiccChannelManager downloadResult

        // Never RESULT_MUST_DEACTIVATE_SIM at this point (forceDeactivateSim = true): the profile is
        // installed, and the platform would download it again after the user's consent. No other
        // error either: the carrier app would ask for a new activation code, which counts against
        // the order's retry limit.
        if (iccid.isNullOrEmpty() || onSwitchToSubscriptionWithPort(slotIndex, portIndex, iccid, true) != RESULT_OK) {
            Log.e(TAG, "Profile $iccid downloaded but not enabled; it has to be enabled manually")
            // The platform only refreshes its list after the switch
            appContainer.subscriptionManager.tryRefreshCachedEuiccInfo(cardId)
        }
        downloadResult
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

            // No need to special-case a running download: withEuiccChannelManager waited for
            // it to finish, so the eUICC is quiescent and we read the current profile list.
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
