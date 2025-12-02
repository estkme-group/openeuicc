package im.angry.openeuicc.core

import android.se.omapi.Channel
import android.se.omapi.SEService
import android.se.omapi.Session
import android.util.Log
import im.angry.openeuicc.util.UiccPortInfoCompat
import im.angry.openeuicc.util.encodeHex
import im.angry.openeuicc.util.getUiccReaderCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.typeblog.lpac_jni.ApduInterface
import java.util.concurrent.atomic.AtomicInteger

class OmapiApduInterface(
    private val service: SEService,
    private val port: UiccPortInfoCompat,
    private val verboseLoggingFlow: Flow<Boolean>
) : ApduInterface, ApduInterfaceAtrProvider {
    companion object {
        const val TAG = "OmapiApduInterface"
    }

    private lateinit var session: Session
    private val index = AtomicInteger(0)
    private val channels = mutableMapOf<Int, Channel>()

    override val valid: Boolean
        get() = service.isConnected && (this::session.isInitialized && !session.isClosed)

    override val atr: ByteArray?
        get() = session.atr

    override fun connect() {
        session = service.getUiccReaderCompat(port.logicalSlotIndex + 1).openSession()
    }

    override fun disconnect() {
        session.close()
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        val channel = session.openLogicalChannel(aid)
        check(channel != null) { "Failed to open logical channel (${aid.encodeHex()})" }
        val handle = index.incrementAndGet()
        synchronized(channels) { channels[handle] = channel }
        return handle
    }

    override fun logicalChannelClose(handle: Int) {
        val channel = channels[handle]
        check(channel != null) { "Invalid logical channel handle $handle" }
        if (channel.isOpen) channel.close()
        synchronized(channels) { channels.remove(handle) }
    }

    override fun transmit(handle: Int, tx: ByteArray): ByteArray {
        val channel = channels[handle]
        check(channel != null) { "Invalid logical channel handle $handle" }

        if (runBlocking { verboseLoggingFlow.first() }) {
            Log.d(TAG, "OMAPI APDU: ${tx.encodeHex()}")
        }

        try {
            for (i in 0..10) {
                val res = channel.transmit(tx)
                if (runBlocking { verboseLoggingFlow.first() }) {
                    Log.d(TAG, "OMAPI APDU response: ${res.encodeHex()}")
                }

                if (res.size == 2 && res[0] == 0x66.toByte() && res[1] == 0x01.toByte()) {
                    Log.d(TAG, "Received checksum error 0x6601, retrying (count = $i)")
                    continue
                }

                return res
            }

            throw RuntimeException("Retransmit attempts exhausted; this was likely caused by checksum errors")
        } catch (e: Exception) {
            Log.e(TAG, "OMAPI APDU exception")
            e.printStackTrace()
            throw e
        }
    }
}
