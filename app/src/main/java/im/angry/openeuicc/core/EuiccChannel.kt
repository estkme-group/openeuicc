package im.angry.openeuicc.core

import com.truphone.lpa.LocalProfileAssistant

interface EuiccChannelStateManager {
    val valid: Boolean
    fun destroy()
}

data class EuiccChannel(
    val slotId: Int,
    val cardId: Int,
    val name: String,
    val lpa: LocalProfileAssistant,
    val stateManager: EuiccChannelStateManager
)