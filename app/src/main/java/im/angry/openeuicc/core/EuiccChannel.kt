package im.angry.openeuicc.core

import com.truphone.lpa.LocalProfileAssistant

// A custom type to avoid compatibility issues with UiccCardInfo / UiccPortInfo
data class EuiccChannelInfo(
    val slotId: Int,
    val cardId: Int,
    val name: String,
    val imei: String,
    val removable: Boolean
)

abstract class EuiccChannel(
    info: EuiccChannelInfo
) {
    val slotId = info.slotId
    val cardId = info.cardId
    val name = info.name
    val imei = info.imei
    val removable = info.removable

    abstract val lpa: LocalProfileAssistant
    abstract val valid: Boolean

    abstract fun close()
}
