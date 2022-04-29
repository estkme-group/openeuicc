package com.truphone.util;

public class TextUtil {
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Converts the given byte array to its hex representation.
     *
     * @param data The byte array to convert.
     * @return Hex-encoded data as a string.
     * @see #toHexString(byte[], int, int)
     */
    public static String toHexString(byte[] data) {
        return data == null ? null : toHexString(data, 0, data.length);
    }

    /**
     * Converts the given byte array slice to its hex representation.
     *
     * @param data   The byte array to convert.
     * @param offset Slice start.
     * @param length Slice length.
     * @return Hex-encoded data as a string.
     */
    public static String toHexString(final byte[] data, int offset, int length) {
        final char[] result = new char[length << 1];
        length += offset;
        for (int i = 0; offset < length; ++offset) {
            result[i++] = HEX_DIGITS[(data[offset] >>> 4) & 0x0F];
            result[i++] = HEX_DIGITS[data[offset] & 0x0F];
        }
        return new String(result);
    }

}
