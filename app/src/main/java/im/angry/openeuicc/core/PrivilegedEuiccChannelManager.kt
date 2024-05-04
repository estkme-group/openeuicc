package im.angry.openeuicc.core

import android.content.Context
import im.angry.openeuicc.di.AppContainer
import im.angry.openeuicc.util.*
import java.lang.Exception

class PrivilegedEuiccChannelManager(
    appContainer: AppContainer,
    context: Context
) :
    DefaultEuiccChannelManager(appContainer, context) {
    override val uiccCards: Collection<UiccCardInfoCompat>
        get() = tm.uiccCardsInfoCompat

    // Clean up channels left open in TelephonyManager
    // due to a (potentially) forced restart
    // This should be called every time the application is restarted
    fun closeAllStaleChannels() {
        for (card in tm.uiccCardsInfo) {
            for (channel in 0 until 10) {
                try {
                    tm.iccCloseLogicalChannelBySlot(card.slotIndex, channel)
                } catch (_: Exception) {
                    // We do not care
                }
            }
        }
    }

    override fun notifyEuiccProfilesChanged(logicalSlotId: Int) {
        appContainer.subscriptionManager.apply {
            findEuiccChannelBySlotBlocking(logicalSlotId)?.let {
                tryRefreshCachedEuiccInfo(it.cardId)
            }
        }
    }
}