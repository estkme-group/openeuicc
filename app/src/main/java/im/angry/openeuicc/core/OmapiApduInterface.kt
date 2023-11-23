package im.angry.openeuicc.core

import android.se.omapi.Channel
import android.se.omapi.SEService
import android.se.omapi.Session
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl
import java.lang.IllegalStateException

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
        if (this::lastChannel.isInitialized) {
            throw IllegalStateException("Can only open one channel")
        }
        lastChannel = session.openLogicalChannel(aid)!!;
        return 0;
    }

    override fun logicalChannelClose(handle: Int) {
        if (handle != 0 || !this::lastChannel.isInitialized) {
            throw IllegalStateException("Unknown channel")
        }
        lastChannel.close()
    }

    override fun transmit(tx: ByteArray): ByteArray {
        if (!this::lastChannel.isInitialized) {
            throw IllegalStateException("Unknown channel")
        }

        return lastChannel.transmit(tx)
    }

}

class OmapiChannel(
    service: SEService,
    info: EuiccChannelInfo,
) : EuiccChannel(info) {
    companion object {
        private const val TAG = "OmapiChannel"
        private val APPLET_ID = byteArrayOf(-96, 0, 0, 5, 89, 16, 16, -1, -1, -1, -1, -119, 0, 0, 1, 0)

        /*fun tryConnect(service: SEService, info: EuiccChannelInfo): OmapiChannel? {
            try {
                val reader = service.getUiccReader(info.slotId + 1) // slotId from telephony starts from 0
                val session = reader.openSession()
                val channel = session.openLogicalChannel(APPLET_ID) ?: return null
                return OmapiChannel(info, channel)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to open eUICC channel for slot ${info.slotId}, skipping")
                Log.e(TAG, Log.getStackTraceString(e))
                return null
            }
        }*/
    }

    override val lpa: LocalProfileAssistant = LocalProfileAssistantImpl(
        OmapiApduInterface(service, info),
        HttpInterfaceImpl())

    override val valid: Boolean
        get() = true // TODO: This has to be implemented properly

    override fun close() = lpa.close()
}
