package com.netease.nim.camellia.tools.utils;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * base on key/secret/sha1
 * Created by caojiajun on 2025/9/9
 */
public class CheckSumUtils {

    public static Map<String, String> authHeaders(String key, String secret) {
        String nonce = UUID.randomUUID().toString().replaceAll("-", "");
        String curTime = String.valueOf(System.currentTimeMillis() / 1000);
        Map<String, String> headers = new HashMap<>();
        headers.put("AppKey", key);
        headers.put("Nonce", nonce);
        headers.put("CurTime", curTime);
        headers.put("CheckSum", getCheckSum(secret, nonce, curTime));
        return headers;
    }

    public static boolean check(String secret, String nonce, String curTime, String checksum) {
        long time;
        try {
            time = Long.parseLong(curTime);
        } catch (Exception e) {
            return false;
        }
        if (Math.abs(System.currentTimeMillis()/1000 - time) > 60) {
            return false;
        }
        return Objects.equals(getCheckSum(secret, nonce, curTime), checksum);
    }

    public static String getCheckSum(String secret, String nonce, String curTime) {
        return sha1(secret + nonce + curTime);
    }

    private static String sha1(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("sha1");
            messageDigest.update(value.getBytes());
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        for (byte b : bytes) {
            buf.append(HEX_DIGITS[(b >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[b & 0x0f]);
        }
        return buf.toString();
    }
}
