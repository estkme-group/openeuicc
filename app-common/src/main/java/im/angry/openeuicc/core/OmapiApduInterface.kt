package im.angry.openeuicc.core

import android.se.omapi.Channel
import android.se.omapi.SEService
import android.se.omapi.Session
import android.util.Log
import im.angry.openeuicc.util.*
import net.typeblog.lpac_jni.ApduInterface

class OmapiApduInterface(
    private val service: SEService,
    private val port: UiccPortInfoCompat
): ApduInterface {
    companion object {
        const val TAG = "OmapiApduInterface"
    }

    private lateinit var session: Session
    private lateinit var lastChannel: Channel

    override val valid: Boolean
        get() = service.isConnected && (this::session.isInitialized && !session.isClosed)

    override fun connect() {
        session = service.getUiccReaderCompat(port.logicalSlotIndex + 1).openSession()
    }

    override fun disconnect() {
        session.close()
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        check(!this::lastChannel.isInitialized) {
            "Can only open one channel"
        }
        lastChannel = session.openLogicalChannel(aid)!!;
        return 1;
    }

    override fun logicalChannelClose(handle: Int) {
        check(handle == 1 && !this::lastChannel.isInitialized) {
            "Unknown channel"
        }
        lastChannel.close()
    }

    override fun transmit(tx: ByteArray): ByteArray {
        check(this::lastChannel.isInitialized) {
            "Unknown channel"
        }

        Log.d(TAG, "OMAPI APDU: ${tx.encodeHex()}")

        try {
            return lastChannel.transmit(tx).also {
                Log.d(TAG, "OMAPI APDU response: ${it.encodeHex()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "OMAPI APDU exception")
            e.printStackTrace()
            throw e
        }
    }

}