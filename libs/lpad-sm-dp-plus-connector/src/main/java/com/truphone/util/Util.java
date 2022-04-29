package com.truphone.util;

public class Util {
    public static String byteToHexString(byte b) {
        StringBuffer s = new StringBuffer();

        if ((b & 0xFF) < 16)
            s.append("0");
        s.append(Integer.toHexString(b & 0xFF).toUpperCase());
        return s.toString();
    }

    public static String byteArrayToHexString(byte[] buffer, String separator) {
        StringBuffer s = new StringBuffer();
        int i = 0;

        for (i = 0; i < buffer.length; i++) {
            s.append(byteToHexString(buffer[i]) + separator);
        }

        if (s.length() > 0) {
            s.delete(s.length() - separator.length(), s.length());
        }

        return s.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        if (s == null)
            return null;
        s = s.replaceAll(" ", "").replaceAll(":", "").replaceAll("0x", "").replaceAll("0X", "");
        if (s.length() % 2 != 0)
            throw new IllegalArgumentException("The length cannot be odd.");
        byte[] output = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2)
            output[(i / 2)] = ((byte) Integer.parseInt(s.substring(i, i + 2), 16));
        return output;
    }


    public static String ASCIIToHex(String s) {
        String ret = "";

        if (s != null) {
            byte[] buffer = s.getBytes();

            ret = byteArrayToHexString(buffer, "");
        }

        return ret;
    }
}
