package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.Flow
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class EuiccChannelImpl(
    override val type: String,
    override val port: UiccPortInfoCompat,
    override val intrinsicChannelName: String?,
    private val apduInterface: ApduInterface,
    verboseLoggingFlow: Flow<Boolean>,
    ignoreTLSCertificateFlow: Flow<Boolean>
) : EuiccChannel {
    override val slotId = port.card.physicalSlotIndex
    override val logicalSlotId = port.logicalSlotIndex
    override val portId = port.portIndex

    override val lpa: LocalProfileAssistant =
        LocalProfileAssistantImpl(apduInterface, HttpInterfaceImpl(verboseLoggingFlow, ignoreTLSCertificateFlow))

    override val atr: ByteArray?
        get() = (apduInterface as? ApduInterfaceAtrProvider)?.atr

    override val valid: Boolean
        get() = lpa.valid

    override fun close() = lpa.close()
}
