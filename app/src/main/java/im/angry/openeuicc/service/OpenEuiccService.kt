package im.angry.openeuicc.service

import android.service.euicc.*
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccInfo
import com.truphone.lpa.LocalProfileInfo
import com.truphone.lpad.progress.Progress
import im.angry.openeuicc.OpenEuiccApplication
import im.angry.openeuicc.core.EuiccChannel
import im.angry.openeuicc.util.*

class OpenEuiccService : EuiccService() {
    private val openEuiccApplication
        get() = application as OpenEuiccApplication

    private fun findChannel(slotId: Int): EuiccChannel? =
        openEuiccApplication.euiccChannelManager
            .findEuiccChannelBySlotBlocking(slotId)

    override fun onGetEid(slotId: Int): String? =
        findChannel(slotId)?.lpa?.eid

    // When two eSIM cards are present on one device, the Android settings UI
    // gets confused and sets the incorrect slotId for profiles from one of
    // the cards. This function helps Detect this case and abort early.
    private fun EuiccChannel.profileExists(iccid: String?) =
        lpa.profiles.any { it.iccid == iccid }

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

    override fun onGetEuiccProfileInfoList(slotId: Int): GetEuiccProfileInfoListResult? {
        val channel = findChannel(slotId) ?: return null
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
        try {
            val channel = findChannel(slotId) ?: return RESULT_FIRST_USER

            if (!channel.profileExists(iccid)) {
                return RESULT_FIRST_USER
            }

            val profile = channel.lpa.profiles.find {
                it.iccid == iccid
            } ?: return RESULT_FIRST_USER

            if (profile.state == LocalProfileInfo.State.Enabled) {
                // Must disable the profile first
                return RESULT_FIRST_USER
            }

            return if (channel.lpa.deleteProfile(iccid, Progress())) {
                RESULT_OK
            } else {
                RESULT_FIRST_USER
            }
        } catch (e: Exception) {
            return RESULT_FIRST_USER
        }
    }

    // TODO: on some devices we need to update the mapping (and potentially disable a pSIM)
    //       for eSIM to be usable, in which case we will have to respect forceDeactivateSim.
    //       This is the same for our custom LUI. Both have to take this into consideration.
    @Deprecated("Deprecated in Java")
    override fun onSwitchToSubscription(
        slotId: Int,
        iccid: String?,
        forceDeactivateSim: Boolean
    ): Int {
        try {
            val channel = findChannel(slotId) ?: return RESULT_FIRST_USER

            if (!channel.profileExists(iccid)) {
                return RESULT_FIRST_USER
            }

            if (iccid == null) {
                // Disable active profile
                val activeProfile = channel.lpa.profiles.find {
                    it.state == LocalProfileInfo.State.Enabled
                } ?: return RESULT_OK

                return if (channel.lpa.disableProfile(activeProfile.iccid, Progress())) {
                    RESULT_OK
                } else {
                    RESULT_FIRST_USER
                }
            } else {
                return if (channel.lpa.enableProfile(iccid, Progress())) {
                    RESULT_OK
                } else {
                    RESULT_FIRST_USER
                }
            }
        } catch (e: Exception) {
            return RESULT_FIRST_USER
        } finally {
            openEuiccApplication.euiccChannelManager.invalidate()
        }
    }

    override fun onUpdateSubscriptionNickname(slotId: Int, iccid: String, nickname: String?): Int {
        val channel = findChannel(slotId) ?: return RESULT_FIRST_USER
        if (!channel.profileExists(iccid)) {
            return RESULT_FIRST_USER
        }
        val success = channel.lpa
            .setNickname(iccid, nickname)
        openEuiccApplication.subscriptionManager.tryRefreshCachedEuiccInfo(channel.cardId)
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