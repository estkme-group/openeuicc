package im.angry.openeuicc.core

import android.telephony.IccOpenLogicalChannelResponse
import android.telephony.TelephonyManager
import android.util.Log
import im.angry.openeuicc.util.UiccPortInfoCompat
import im.angry.openeuicc.util.decodeHex
import im.angry.openeuicc.util.encodeHex
import im.angry.openeuicc.util.iccCloseLogicalChannelByPortCompat
import im.angry.openeuicc.util.iccOpenLogicalChannelByPortCompat
import im.angry.openeuicc.util.iccTransmitApduLogicalChannelByPortCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.typeblog.lpac_jni.ApduInterface

class TelephonyManagerApduInterface(
    private val port: UiccPortInfoCompat,
    private val tm: TelephonyManager,
    private val verboseLoggingFlow: Flow<Boolean>
) : ApduInterface {
    companion object {
        const val TAG = "TelephonyManagerApduInterface"
    }

    override val valid: Boolean
        get() = channels.isNotEmpty()

    private var channels = mutableSetOf<Int>()

    override fun connect() {
        // Do nothing
    }

    override fun disconnect() {
        // Do nothing
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        val hex = aid.encodeHex()
        val channel = tm.iccOpenLogicalChannelByPortCompat(port.card.physicalSlotIndex, port.portIndex, hex, 0)
        if (channel.status != IccOpenLogicalChannelResponse.STATUS_NO_ERROR || channel.channel == IccOpenLogicalChannelResponse.INVALID_CHANNEL) {
            throw IllegalArgumentException("Cannot open logical channel $hex via TelephonyManager on slot ${port.card.physicalSlotIndex} port ${port.portIndex}")
        }
        channels.add(channel.channel)
        return channel.channel
    }

    override fun logicalChannelClose(handle: Int) {
        check(channels.contains(handle)) {
            "Invalid logical channel handle $handle"
        }
        tm.iccCloseLogicalChannelByPortCompat(port.card.physicalSlotIndex, port.portIndex, handle)
        channels.remove(handle)
    }

    override fun transmit(handle: Int, tx: ByteArray): ByteArray {
        check(channels.contains(handle)) {
            "Invalid logical channel handle $handle"
        }
        if (runBlocking { verboseLoggingFlow.first() }) {
            Log.d(TAG, "TelephonyManager APDU: ${tx.encodeHex()}")
        }
        val result = tm.iccTransmitApduLogicalChannelByPortCompat(
            port.card.physicalSlotIndex, port.portIndex, handle,
            tx,
        )
        if (runBlocking { verboseLoggingFlow.first() })
            Log.d(TAG, "TelephonyManager APDU response: $result")
        return result?.decodeHex() ?: byteArrayOf()
    }
}
