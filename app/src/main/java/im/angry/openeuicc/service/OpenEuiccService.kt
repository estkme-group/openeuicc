package im.angry.openeuicc.service

import android.service.euicc.*
import android.telephony.UiccSlotMapping
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccInfo
import android.util.Log
import net.typeblog.lpac_jni.LocalProfileInfo
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*
import java.lang.IllegalStateException

class OpenEuiccService : EuiccService(), OpenEuiccContextMarker {
    companion object {
        const val TAG = "OpenEuiccService"
    }

    private fun findChannel(physicalSlotId: Int): EuiccChannel? =
        euiccChannelManager.findEuiccChannelByPhysicalSlotBlocking(physicalSlotId)

    private fun findChannel(slotId: Int, portId: Int): EuiccChannel? =
        euiccChannelManager.findEuiccChannelByPortBlocking(slotId, portId)

    private fun findAllChannels(physicalSlotId: Int): List<EuiccChannel>? =
        euiccChannelManager.findAllEuiccChannelsByPhysicalSlotBlocking(physicalSlotId)

    override fun onGetEid(slotId: Int): String? =
        findChannel(slotId)?.lpa?.eID

    // When two eSIM cards are present on one device, the Android settings UI
    // gets confused and sets the incorrect slotId for profiles from one of
    // the cards. This function helps Detect this case and abort early.
    private fun EuiccChannel.profileExists(iccid: String?) =
        lpa.profiles.any { it.iccid == iccid }

    private fun ensurePortIsMapped(slotId: Int, portId: Int) {
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

    private fun <T> retryWithTimeout(timeoutMillis: Int, backoff: Int = 1000, f: () -> T?): T? {
        val startTimeMillis = System.currentTimeMillis()
        do {
            try {
                f()?.let { return@retryWithTimeout it }
            } catch (_: Exception) {
                // Ignore
            } finally {
                Thread.sleep(backoff.toLong())
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

    override fun onGetEuiccProfileInfoList(slotId: Int): GetEuiccProfileInfoListResult {
        Log.i(TAG, "onGetEuiccProfileInfoList slotId=$slotId")
        val channel = findChannel(slotId)!!
        val profiles = channel.lpa.profiles.operational.map {
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

        return GetEuiccProfileInfoListResult(RESULT_OK, profiles.toTypedArray(), channel.removable)
    }

    override fun onGetEuiccInfo(slotId: Int): EuiccInfo {
        return EuiccInfo("Unknown") // TODO: Can we actually implement this?
    }

    override fun onDeleteSubscription(slotId: Int, iccid: String): Int {
        Log.i(TAG, "onDeleteSubscription slotId=$slotId iccid=$iccid")
        try {
            val channels = findAllChannels(slotId) ?: return RESULT_FIRST_USER

            if (!channels[0].profileExists(iccid)) {
                return RESULT_FIRST_USER
            }

            // If the profile is enabled by ANY channel (port), we cannot delete it
            channels.forEach { channel ->
                val profile = channel.lpa.profiles.find {
                    it.iccid == iccid
                } ?: return RESULT_FIRST_USER

                if (profile.state == LocalProfileInfo.State.Enabled) {
                    // Must disable the profile first
                    return RESULT_FIRST_USER
                }
            }

            return if (channels[0].lpa.deleteProfile(iccid)) {
                RESULT_OK
            } else {
                RESULT_FIRST_USER
            }
        } catch (e: Exception) {
            return RESULT_FIRST_USER
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
    ): Int {
        Log.i(TAG,"onSwitchToSubscriptionWithPort slotId=$slotId portIndex=$portIndex iccid=$iccid forceDeactivateSim=$forceDeactivateSim")
        try {
            // retryWithTimeout is needed here because this function may be called just after
            // AOSP has switched slot mappings, in which case the slots may not be ready yet.
            val channel = if (portIndex == -1) {
                retryWithTimeout(5000) { findChannel(slotId) }
            } else {
                retryWithTimeout(5000) { findChannel(slotId, portIndex) }
            } ?: run {
                if (!forceDeactivateSim) {
                    // The user must select which SIM to deactivate
                    return@onSwitchToSubscriptionWithPort RESULT_MUST_DEACTIVATE_SIM
                } else {
                    try {
                        // If we are allowed to deactivate any SIM we like, try mapping the indicated port first
                        ensurePortIsMapped(slotId, portIndex)
                        retryWithTimeout(5000) { findChannel(slotId, portIndex) }
                    } catch (e: Exception) {
                        // We cannot map the port (or it is already mapped)
                        // but we can also use any port available on the card
                        retryWithTimeout(5000) { findChannel(slotId) }
                    } ?: return@onSwitchToSubscriptionWithPort RESULT_FIRST_USER
                }
            }

            if (iccid != null && !channel.profileExists(iccid)) {
                Log.i(TAG, "onSwitchToSubscriptionWithPort iccid=$iccid not found")
                return RESULT_FIRST_USER
            }

            // Disable any active profile first if present
            channel.lpa.profiles.find {
                it.state == LocalProfileInfo.State.Enabled
            }?.let { if (!channel.lpa.disableProfile(it.iccid)) return RESULT_FIRST_USER }

            if (iccid != null) {
                if (!channel.lpa.enableProfile(iccid)) {
                    return RESULT_FIRST_USER
                }
            }

            return RESULT_OK
        } catch (e: Exception) {
            return RESULT_FIRST_USER
        } finally {
            euiccChannelManager.invalidate()
        }
    }

    override fun onUpdateSubscriptionNickname(slotId: Int, iccid: String, nickname: String?): Int {
        Log.i(TAG, "onUpdateSubscriptionNickname slotId=$slotId iccid=$iccid nickname=$nickname")
        val channel = findChannel(slotId) ?: return RESULT_FIRST_USER
        if (!channel.profileExists(iccid)) {
            return RESULT_FIRST_USER
        }
        val success = channel.lpa
            .setNickname(iccid, nickname!!)
        appContainer.subscriptionManager.tryRefreshCachedEuiccInfo(channel.cardId)
        return if (success) {
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
