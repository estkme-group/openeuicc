package im.angry.openeuicc.util

fun hexStringToByteArray(str: String): ByteArray {
    val length = str.length / 2
    val out = ByteArray(length)
    for (i in 0 until length) {
        val i2 = i * 2
        out[i] = str.substring(i2, i2 + 2).toInt(16).toByte()
    }
    return out
}
fun byteArrayToHex(arr: ByteArray): String {
    val sb = StringBuilder()
    val length = arr.size
    for (i in 0 until length) {
        sb.append(String.format("%02X", arr[i]))
    }
    return sb.toString()
}