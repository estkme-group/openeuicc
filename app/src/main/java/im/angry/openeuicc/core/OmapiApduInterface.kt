package im.angry.openeuicc.core

import android.se.omapi.Channel
import android.se.omapi.SEService
import android.se.omapi.Session
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class OmapiApduInterface(
    private val service: SEService,
    private val info: EuiccChannelInfo
): ApduInterface {
    private lateinit var session: Session
    private lateinit var lastChannel: Channel

    override fun connect() {
        session = service.getUiccReader(info.slotId + 1).openSession()
    }

    override fun disconnect() {
        session.close()
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        check(!this::lastChannel.isInitialized) {
            "Can only open one channel"
        }
        lastChannel = session.openLogicalChannel(aid)!!;
        return 0;
    }

    override fun logicalChannelClose(handle: Int) {
        check(handle == 0 && !this::lastChannel.isInitialized) {
            "Unknown channel"
        }
        lastChannel.close()
    }

    override fun transmit(tx: ByteArray): ByteArray {
        check(this::lastChannel.isInitialized) {
            "Unknown channel"
        }

        return lastChannel.transmit(tx)
    }

}

class OmapiChannel(
    service: SEService,
    info: EuiccChannelInfo,
) : EuiccChannel(info) {
    override val lpa: LocalProfileAssistant = LocalProfileAssistantImpl(
        OmapiApduInterface(service, info),
        HttpInterfaceImpl())

    override fun close() = lpa.close()
}
