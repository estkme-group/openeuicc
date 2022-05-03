package im.angry.openeuicc.service

import android.service.euicc.*
import android.telephony.euicc.DownloadableSubscription
import android.telephony.euicc.EuiccInfo
import com.truphone.lpa.LocalProfileAssistant
import com.truphone.lpa.LocalProfileInfo
import im.angry.openeuicc.OpenEuiccApplication

class OpenEuiccService : EuiccService() {
    private fun findLpa(slotId: Int): LocalProfileAssistant? =
        (application as OpenEuiccApplication).euiccChannelManager
            .findEuiccChannelBySlotBlocking(slotId)?.lpa

    override fun onGetEid(slotId: Int): String? =
        findLpa(slotId)?.eid

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
        TODO("Not yet implemented")
    }

    override fun onGetDefaultDownloadableSubscriptionList(
        slotId: Int,
        forceDeactivateSim: Boolean
    ): GetDefaultDownloadableSubscriptionListResult {
        TODO("Not yet implemented")
    }

    override fun onGetEuiccProfileInfoList(slotId: Int): GetEuiccProfileInfoListResult? {
        val profiles = (findLpa(slotId) ?: return null).profiles.filter {
            it.profileClass != LocalProfileInfo.Clazz.Testing
        }.map {
            EuiccProfileInfo.Builder(it.iccidLittleEndian).apply {
                setProfileName(it.name)
                setNickname(it.nickName)
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

        return GetEuiccProfileInfoListResult(RESULT_OK, profiles.toTypedArray(), false)
    }

    override fun onGetEuiccInfo(slotId: Int): EuiccInfo {
        TODO("Not yet implemented")
    }

    override fun onDeleteSubscription(slotId: Int, iccid: String?): Int {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun onSwitchToSubscription(
        slotId: Int,
        iccid: String?,
        forceDeactivateSim: Boolean
    ): Int {
        TODO("Not yet implemented")
    }

    override fun onUpdateSubscriptionNickname(slotId: Int, iccid: String?, nickname: String?): Int {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun onEraseSubscriptions(slotId: Int): Int {
        TODO("Not yet implemented")
    }

    override fun onRetainSubscriptionsForFactoryReset(slotId: Int): Int {
        TODO("Not yet implemented")
    }
}