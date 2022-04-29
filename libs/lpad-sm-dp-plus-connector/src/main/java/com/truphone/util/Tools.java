package com.truphone.util;

public class Tools {

    public static String itoa(int value, int len) {
        String result = Integer.toHexString(value).toUpperCase();
        int rLen = result.length();
        len = 2 * len;
        if (rLen > len) {
            return result.substring(rLen - len, rLen);
        }
        if (rLen == len) {
            return result;
        }
        StringBuffer strBuff = new StringBuffer(result);
        for (int i = 0; i < len - rLen; i++) {
            strBuff.insert(0, '0');
        }
        return strBuff.toString();
    }

    public static String toHex(String num) {
        String hex = Integer.toHexString(Integer.valueOf(num));
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }
        return hex.toUpperCase();
    }
}
