package im.angry.openeuicc.core

import android.content.Context
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.util.UiccCardInfoCompat
import im.angry.openeuicc.util.cardId
import im.angry.openeuicc.util.iccCloseLogicalChannelBySlot
import im.angry.openeuicc.util.tryRefreshCachedEuiccInfo
import im.angry.openeuicc.util.uiccCardsInfoCompat

class PrivilegedEuiccChannelManager(appContainer: AppContainer, context: Context) :
    DefaultEuiccChannelManager(appContainer, context) {
    override val uiccCards: Collection<UiccCardInfoCompat>
        get() = tm.uiccCardsInfoCompat

    // Clean up channels left open in TelephonyManager
    // due to a (potentially) forced restart
    // This should be called every time the application is restarted
    fun closeAllStaleChannels() {
        for (card in tm.uiccCardsInfoCompat) {
            // As a further option, support of logical channels is expanded up to 19 supplementary logical channels as defined by the latest version of [ISO 7816-4].
            //
            // from <https://globalplatform.org/wp-content/uploads/2018/05/GPC_CardSpecification_v2.3.1_PublicRelease_CC.pdf#page=34>
            // from <https://globalplatform.org/wp-content/uploads/2018/05/GPC_CardSpecification_v2.3.1_PublicRelease_CC.pdf#page=147>
            // from <https://github.com/seek-for-android/pool/blob/01627dfb/src/smartcard-api/src/org/simalliance/openmobileapi/service/Channel.java#L156-L183>
            for (channel in 0 until 20) {
                try {
                    tm.iccCloseLogicalChannelBySlot(card.physicalSlotIndex, channel)
                } catch (_: Exception) {
                    // We do not care
                }
            }
        }
    }

    override suspend fun notifyEuiccProfilesChanged(logicalSlotId: Int) {
        val channel = findEuiccChannelByLogicalSlot(logicalSlotId) ?: return
        appContainer.subscriptionManager.tryRefreshCachedEuiccInfo(channel.cardId)
    }
}
