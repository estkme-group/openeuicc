package im.angry.openeuicc.core

import im.angry.openeuicc.util.UiccPortInfoCompat
import im.angry.openeuicc.util.decodeHex
import kotlinx.coroutines.flow.Flow
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class EuiccChannelImpl(
    override val type: String,
    override val port: UiccPortInfoCompat,
    override val intrinsicChannelName: String?,
    override val apduInterface: ApduInterface,
    verboseLoggingFlow: Flow<Boolean>,
    ignoreTLSCertificateFlow: Flow<Boolean>
) : EuiccChannel {
    companion object {
        // TODO: This needs to go somewhere else.
        val ISDR_AID = "A0000005591010FFFFFFFF8900000100".decodeHex()
    }

    override val slotId = port.card.physicalSlotIndex
    override val logicalSlotId = port.logicalSlotIndex
    override val portId = port.portIndex

    override val lpa: LocalProfileAssistant =
        LocalProfileAssistantImpl(
            ISDR_AID,
            apduInterface,
            HttpInterfaceImpl(verboseLoggingFlow, ignoreTLSCertificateFlow)
        )

    override val atr: ByteArray?
        get() = (apduInterface as? ApduInterfaceAtrProvider)?.atr

    override val valid: Boolean
        get() = lpa.valid

    override fun close() = lpa.close()
}
