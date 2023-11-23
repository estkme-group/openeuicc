package im.angry.openeuicc.core

import android.se.omapi.SEService
import net.typeblog.lpac_jni.ApduInterface
import net.typeblog.lpac_jni.LocalProfileAssistant
import net.typeblog.lpac_jni.impl.HttpInterfaceImpl
import net.typeblog.lpac_jni.impl.LocalProfileAssistantImpl

class OmapiApduInterface(
    private val service: SEService,
    private val info: EuiccChannelInfo
): ApduInterface {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun logicalChannelClose(handle: Int) {
        TODO("Not yet implemented")
    }

    override fun transmit(tx: ByteArray): ByteArray {
        TODO("Not yet implemented")
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
