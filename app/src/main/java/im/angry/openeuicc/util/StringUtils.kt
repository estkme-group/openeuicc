package im.angry.openeuicc.util

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    val decodedLength = length / 2
    val out = ByteArray(decodedLength)
    for (i in 0 until decodedLength) {
        val i2 = i * 2
        out[i] = substring(i2, i2 + 2).toInt(16).toByte()
    }
    return out
}

fun ByteArray.encodeHex(): String {
    val sb = StringBuilder()
    val length = size
    for (i in 0 until length) {
        sb.append(String.format("%02X", this[i]))
    }
    return sb.toString()
}