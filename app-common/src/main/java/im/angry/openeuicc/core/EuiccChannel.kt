package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.LocalProfileAssistant

abstract class EuiccChannel(
    port: UiccPortInfoCompat
) {
    val slotId = port.card.physicalSlotIndex // PHYSICAL slot
    val logicalSlotId = port.logicalSlotIndex
    val portId = port.portIndex
    val cardId = port.card.cardId
    val name = "SLOT ${port.card.physicalSlotIndex}:${port.portIndex}"
    val removable = port.card.isRemovable

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

    fun close() = lpa.close()
}
