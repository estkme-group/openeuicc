package com.truphone.util

import java.io.InputStream
import java.lang.NumberFormatException
import java.lang.StringBuilder

object TextUtil {
    private val HEX_DIGITS = charArrayOf(
        '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    /**
     * Converts the given byte array to its hex representation.
     *
     * @param data The byte array to convert.
     * @return Hex-encoded data as a string.
     * @see .toHexString
     */
    @JvmStatic
    fun toHexString(data: ByteArray?): String? {
        return if (data == null) null else toHexString(data, 0, data.size)
    }

    /**
     * Converts the given byte array slice to its hex representation.
     *
     * @param data   The byte array to convert.
     * @param offset Slice start.
     * @param length Slice length.
     * @return Hex-encoded data as a string.
     */
    @JvmStatic
    fun toHexString(data: ByteArray, offset: Int, length: Int): String {
        var offset = offset
        var length = length
        val result = CharArray(length shl 1)
        length += offset
        var i = 0
        while (offset < length) {
            result[i++] = HEX_DIGITS[data[offset].toInt() ushr 4 and 0x0F]
            result[i++] = HEX_DIGITS[data[offset].toInt() and 0x0F]
            ++offset
        }
        return String(result)
    }

    /*
     * Decodes a hex string into a byte array
     * Adapted from <https://stackoverflow.com/questions/66613717/kotlin-convert-hex-string-to-bytearray>
     */
    @JvmStatic
    fun decodeHex(str: String): ByteArray {
        if (str.length % 2 != 0) {
            throw NumberFormatException("Must have an even length")
        }

        return str.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * Converts a big-endian representation of ICCID into little-endian
     * Big-endian representation is used internally in communication with the SIM.
     *
     * @param iccid The ICCID to be converted
     */
    @JvmStatic
    fun iccidBigToLittle(iccid: String): String {
        val builder = StringBuilder()
        for (i in 0 until iccid.length / 2) {
            builder.append(iccid[i * 2 + 1])
            if (iccid[i * 2] != 'F') builder.append(iccid[i * 2])
        }
        return builder.toString()
    }

    /**
     * Converts a little-endian representation of ICCID into big-endian
     *
     * @param iccid The ICCID to be converted
     */
    @JvmStatic
    fun iccidLittleToBig(iccidLittle: String): String {
        val builder = StringBuilder()
        for (i in 0 until iccidLittle.length / 2) {
            builder.append(iccidLittle[i * 2 + 1])
            builder.append(iccidLittle[i * 2])
        }
        if (iccidLittle.length % 2 == 1) {
            builder.append('F')
            builder.append(iccidLittle[iccidLittle.length - 1])
        }
        return builder.toString()
    }

    /*
     * Read an InputStream into a ByteArray
     * This is exposed to the Java side as a convenience
     * TODO: Remove when we migrate the full code base to Kotlin
     */
    @JvmStatic
    fun readInputStream(i: InputStream): ByteArray = i.readBytes()

    /*
     * TODO: Remove after Kotlin migration
     */
    @JvmStatic
    fun isNotBlank(str: String?): Boolean = !isBlank(str)

    /*
     * TODO: Remove after Kotlin migration
     */
    @JvmStatic
    fun isBlank(str: String?): Boolean = str?.isBlank() ?: true

    /*
     * TODO: Remove after Kotlin migration
     */
    @JvmStatic
    fun isNotEmpty(str: String?): Boolean = str?.isNotEmpty() ?: false
}