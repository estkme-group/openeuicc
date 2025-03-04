package im.angry.openeuicc.vendored

import android.util.Log
import im.angry.openeuicc.core.ApduInterfaceAtrProvider
import im.angry.openeuicc.util.TAG
import im.angry.openeuicc.util.decodeHex
import net.typeblog.lpac_jni.ApduInterface

data class ESTKmeInfo(
    val serialNumber: String?,
    val bootloaderVersion: String?,
    val firmwareVersion: String?,
    val skuName: String?,
)

fun isESTKmeATR(iface: ApduInterface): Boolean {
    if (iface !is ApduInterfaceAtrProvider) return false
    val atr = iface.atr ?: return false
    val fpr = "estk.me".encodeToByteArray()
    for (index in atr.indices) {
        if (atr.size - index < fpr.size) break
        if (atr.sliceArray(index until index + fpr.size).contentEquals(fpr)) return true
    }
    return false
}

fun getESTKmeInfo(iface: ApduInterface): ESTKmeInfo? {
    if (!isESTKmeATR(iface)) return null
    fun decode(b: ByteArray): String? {
        if (b.size < 2) return null
        if (b[b.size - 2] != 0x90.toByte() || b[b.size - 1] != 0x00.toByte()) return null
        return b.sliceArray(0 until b.size - 2).decodeToString()
    }
    return try {
        iface.withLogicalChannel("A06573746B6D65FFFFFFFFFFFF6D6774".decodeHex()) { transmit ->
            fun invoke(p1: Byte) = decode(transmit(byteArrayOf(0x00, 0x00, p1, 0x00, 0x00)))
            ESTKmeInfo(
                invoke(0x00), // serial number
                invoke(0x01), // bootloader version
                invoke(0x02), // firmware version
                invoke(0x03), // sku name
            )
        }
    } catch (e: Exception) {
        Log.d(TAG, "Failed to get ESTKmeInfo", e)
        null
    }
}

