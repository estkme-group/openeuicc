package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant

class EuiccChannelWrapper(orig: EuiccChannel) : EuiccChannel {
    private var _inner: EuiccChannel? = orig

    private val channel: EuiccChannel
        get() {
            if (_inner == null) {
                throw IllegalStateException("This wrapper has been invalidated")
            }

            return _inner!!
        }

    override val type: String
        get() = channel.type
    override val port: UiccPortInfoCompat
        get() = channel.port
    override val slotId: Int
        get() = channel.slotId
    override val logicalSlotId: Int
        get() = channel.logicalSlotId
    override val portId: Int
        get() = channel.portId
    override val seId: EuiccChannel.SecureElementId
        get() = channel.seId
    private val lpaDelegate = lazy {
        LocalProfileAssistantWrapper(channel.lpa)
    }
    override val lpa: LocalProfileAssistant by lpaDelegate
    override val valid: Boolean
        get() = channel.valid
    override val intrinsicChannelName: String?
        get() = channel.intrinsicChannelName
    override val apduInterface: ApduInterface
        get() = channel.apduInterface
    override val atr: ByteArray?
        get() = channel.atr
    override val isdrAid: ByteArray
        get() = channel.isdrAid

    override fun close() = channel.close()

    fun invalidateWrapper() {
        _inner = null

        if (lpaDelegate.isInitialized()) {
            (lpa as LocalProfileAssistantWrapper).invalidateWrapper()
        }
    }
}