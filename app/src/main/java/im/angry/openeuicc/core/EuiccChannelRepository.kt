package im.angry.openeuicc.core

import com.truphone.lpa.LocalProfileAssistant

data class EuiccChannel(
    val slotId: Int,
    val name: String,
    val lpa: LocalProfileAssistant
)

interface EuiccChannelRepository {
    suspend fun load()
    val availableChannels: List<EuiccChannel>
}