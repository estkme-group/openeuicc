package im.angry.openeuicc.core.usb

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import net.typeblog.lpac_jni.ApduInterface

class UsbApduInterface(
    private val conn: UsbDeviceConnection,
    private val bulkIn: UsbEndpoint,
    private val bulkOut: UsbEndpoint
): ApduInterface {
    private lateinit var ccidDescription: UsbCcidDescription

    override fun connect() {
        ccidDescription = UsbCcidDescription.fromRawDescriptors(conn.rawDescriptors)!!
        ccidDescription.checkTransportProtocol()
    }

    override fun disconnect() {
        conn.close()
    }

    override fun logicalChannelOpen(aid: ByteArray): Int {
        return 0
    }

    override fun logicalChannelClose(handle: Int) {

    }

    override fun transmit(tx: ByteArray): ByteArray {
        return byteArrayOf()
    }

    override val valid: Boolean
        get() = true
}