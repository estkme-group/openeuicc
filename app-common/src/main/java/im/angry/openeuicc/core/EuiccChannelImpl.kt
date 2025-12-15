package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class EuiccChannelImpl(
    override val type: String,
    override val port: UiccPortInfoCompat,
    override val intrinsicChannelName: String?,
    override val apduInterface: ApduInterface,
    override val isdrAid: ByteArray,
    override val seId: EuiccChannel.SecureElementId,
    verboseLoggingFlow: Flow<Boolean>,
    ignoreTLSCertificateFlow: Flow<Boolean>,
    es10xMssFlow: Flow<Int>,
) : EuiccChannel {
    override val slotId = port.card.physicalSlotIndex
    override val logicalSlotId = port.logicalSlotIndex
    override val portId = port.portIndex

    override val lpa: LocalProfileAssistant =
        LocalProfileAssistantImpl(
            isdrAid,
            apduInterface,
            HttpInterfaceImpl(verboseLoggingFlow, ignoreTLSCertificateFlow),
        ).also {
            it.setEs10xMss(runBlocking { es10xMssFlow.first().toByte() })
        }

    override val atr: ByteArray?
        get() = (apduInterface as? ApduInterfaceAtrProvider)?.atr

    override val valid: Boolean
        get() = lpa.valid

    private var hasMultipleSEInitialized = false

    override var hasMultipleSE: Boolean = false
        set(value) {
            if (hasMultipleSEInitialized) {
                throw IllegalStateException("already initialized")
            }

            field = value
        }

    override fun close() = lpa.close()
}
