package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class EuiccChannel(
    val port: UiccPortInfoCompat,
    apduInterface: ApduInterface,
) {
    val slotId = port.card.physicalSlotIndex // PHYSICAL slot
    val logicalSlotId = port.logicalSlotIndex
    val portId = port.portIndex

    val lpa: LocalProfileAssistant = LocalProfileAssistantImpl(apduInterface, HttpInterfaceImpl())

    val valid: Boolean
        get() = lpa.valid

    fun close() = lpa.close()
}
