package im.angry.openeuicc.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.euicc.*
import android.telephony.UiccSlotMapping
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccInfo
import android.util.Log
import net.typeblog.lpac_jni.LocalProfileInfo
import im.angry.openeuicc.core.EuiccChannelManager
import im.angry.openeuicc.service.EuiccChannelManagerService.Companion.waitDone
import im.angry.openeuicc.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.IllegalStateException

class OpenEuiccService : EuiccService(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "OpenEuiccService"
    }

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
            telephonyManager.uiccCardsInfoCompat.firstOrNull { it.isEuicc }?.physicalSlotIndex == physicalSlotId
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
     * This function cannot be inline because non-local returns may bypass the unbind
     */
    private fun <T> withEuiccChannelManager(fn: suspend EuiccChannelManagerContext.() -> T): T {
        val (binder, unbind) = runBlocking {
            bindServiceSuspended(
                Intent(
                    this@OpenEuiccService,
                    EuiccChannelManagerService::class.java
                ), Context.BIND_AUTO_CREATE
            )
        }

        if (binder == null) {
            throw RuntimeException("Unable to bind to EuiccChannelManagerService; aborting")
        }

        val localBinder = binder as EuiccChannelManagerService.LocalBinder

        val ret = runBlocking {
            EuiccChannelManagerContext(localBinder.service).fn()
        }

        unbind()
        return ret
    }

    override fun onGetEid(slotId: Int): String? = withEuiccChannelManager {
        val portId = euiccChannelManager.findFirstAvailablePort(slotId)
        if (portId < 0) return@withEuiccChannelManager null
        euiccChannelManager.withEuiccChannel(slotId, portId) { channel ->
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

    override fun onGetDownloadableSubscriptionMetadata(
        slotId: Int,
        subscription: DownloadableSubscription?,
        forceDeactivateSim: Boolean
    ): GetDownloadableSubscriptionMetadataResult {
        // Stub: return as-is and do not fetch anything
        // This is incompatible with carrier eSIM apps; should we make it compatible?
        return GetDownloadableSubscriptionMetadataResult(RESULT_OK, subscription)
    }

    override fun onGetDefaultDownloadableSubscriptionList(
        slotId: Int,
        forceDeactivateSim: Boolean
    ): GetDefaultDownloadableSubscriptionListResult {
        // Stub: we do not implement this (as this would require phoning in a central GSMA server)
        return GetDefaultDownloadableSubscriptionListResult(RESULT_OK, arrayOf())
    }

    override fun onGetEuiccProfileInfoList(slotId: Int): GetEuiccProfileInfoListResult = withEuiccChannelManager {
        Log.i(TAG, "onGetEuiccProfileInfoList slotId=$slotId")
        if (slotId == -1 || shouldIgnoreSlot(slotId)) {
            Log.i(TAG, "ignoring slot $slotId")
            return@withEuiccChannelManager GetEuiccProfileInfoListResult(
                RESULT_FIRST_USER,
                arrayOf(),
                true
            )
        }

        // TODO: Temporarily enable the slot to access its profiles if it is currently unmapped
        val port = euiccChannelManager.findFirstAvailablePort(slotId)
        if (port == -1) {
            return@withEuiccChannelManager GetEuiccProfileInfoListResult(
                RESULT_FIRST_USER,
                arrayOf(),
                true
            )
        }

        try {
            return@withEuiccChannelManager euiccChannelManager.withEuiccChannel(
                slotId,
                port
            ) { channel ->
                val filteredProfiles =
                    if (runBlocking { preferenceRepository.unfilteredProfileListFlow.first() })
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
                                LocalProfileInfo.Clazz.Testing -> EuiccProfileInfo.PROFILE_CLASS_TESTING
                                LocalProfileInfo.Clazz.Provisioning -> EuiccProfileInfo.PROFILE_CLASS_PROVISIONING
                                LocalProfileInfo.Clazz.Operational -> EuiccProfileInfo.PROFILE_CLASS_OPERATIONAL
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
        } catch (e: EuiccChannelManager.EuiccChannelNotFoundException) {
            return@withEuiccChannelManager GetEuiccProfileInfoListResult(
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
            euiccChannelManager.withEuiccChannel(slotId, port) { channel ->
                val profile = channel.lpa.profiles.find {
                    it.iccid == iccid
                } ?: return@withEuiccChannel false

                profile.state == LocalProfileInfo.State.Enabled
            }
        }

        if (enabledAnywhere) return@withEuiccChannelManager RESULT_FIRST_USER

        euiccChannelManagerService.waitForForegroundTask()
        val success = euiccChannelManagerService.launchProfileDeleteTask(slotId, ports[0], iccid)
            .waitDone() == null

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
        Log.i(TAG,"onSwitchToSubscriptionWithPort slotId=$slotId portIndex=$portIndex iccid=$iccid forceDeactivateSim=$forceDeactivateSim")
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
                    euiccChannelManager.withEuiccChannel(slotId, portIndex) { channel ->
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
                    euiccChannelManager.withEuiccChannel(slotId, foundPortId) { channel ->
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
                    euiccChannelManager.withEuiccChannel(foundSlotId, foundPortId) { channel ->
                        channel.lpa.profiles.find { it.state == LocalProfileInfo.State.Enabled }
                    }?.iccid ?: return@withEuiccChannelManager RESULT_FIRST_USER
                Pair(foundIccid, false)
            } else {
                Pair(iccid, true)
            }

            val res = euiccChannelManagerService.launchProfileSwitchTask(
                foundSlotId,
                foundPortId,
                foundIccid,
                enable,
                30 * 1000
            ).waitDone()

            if (res != null) return@withEuiccChannelManager RESULT_FIRST_USER

            return@withEuiccChannelManager RESULT_OK
        } catch (e: Exception) {
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
                (euiccChannelManagerService.launchProfileRenameTask(slotId, port, iccid, nickname!!)
                    .waitDone()) == null

            euiccChannelManager.withEuiccChannel(slotId, port) { channel ->
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
