package im.angry.openeuicc.core

import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.LocalProfileAssistant

class EuiccChannelWrapper(private val _inner: EuiccChannel) : EuiccChannel {
    private var wrapperInvalidated = false

    private val channel: EuiccChannel
        get() {
            if (wrapperInvalidated) {
                throw IllegalStateException("This wrapper has been invalidated")
            }

            return _inner
        }
    override val port: UiccPortInfoCompat
        get() = channel.port
    override val slotId: Int
        get() = channel.slotId
    override val logicalSlotId: Int
        get() = channel.logicalSlotId
    override val portId: Int
        get() = channel.portId
    private val lpaDelegate = lazy {
        LocalProfileAssistantWrapper(_inner.lpa)
    }
    override val lpa: LocalProfileAssistant by lpaDelegate
    override val valid: Boolean
        get() = channel.valid

    override fun close() = channel.close()

    fun invalidateWrapper() {
        wrapperInvalidated = true

        if (lpaDelegate.isInitialized()) {
            (lpa as LocalProfileAssistantWrapper).invalidateWrapper()
        }
    }
}