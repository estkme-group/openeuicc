package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.Flow
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class EuiccChannelImpl(
    override val port: UiccPortInfoCompat,
    apduInterface: ApduInterface,
    verboseLoggingFlow: Flow<Boolean>
) : EuiccChannel {
    override val slotId = port.card.physicalSlotIndex
    override val logicalSlotId = port.logicalSlotIndex
    override val portId = port.portIndex

    override val lpa: LocalProfileAssistant =
        LocalProfileAssistantImpl(apduInterface, HttpInterfaceImpl(verboseLoggingFlow))

    override val valid: Boolean
        get() = lpa.valid

    override fun close() = lpa.close()
}
