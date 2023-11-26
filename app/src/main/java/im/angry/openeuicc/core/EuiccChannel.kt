package im.angry.openeuicc.core

import net.typeblog.lpac_jni.LocalProfileAssistant

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
    val valid: Boolean
        get() {
            try {
                // Try to ping the eUICC card by reading the EID
                lpa.eID
            } catch (e: Exception) {
                return false
            }
            return true
        }

    abstract fun close()
}
