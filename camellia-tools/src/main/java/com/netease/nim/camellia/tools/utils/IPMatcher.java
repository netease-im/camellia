package com.netease.nim.camellia.tools.utils;


/**
 *
 * Created by caojiajun on 2020/12/16
 */
public class IPMatcher {

    private static final int IPSEC = 256;
    private static final int MASKDEF = 0xFFFFFFFF;

    private final int iip;
    private final int imask;

    public IPMatcher(String ip, String mask) {
        imask = mask2int(mask);
        iip = ip2int(ip) & imask;
    }

    public int mask2int(String mask) {
        int imask;
        int bits = 32 - Integer.parseInt(mask);
        if (bits < 0) {
            bits = 0;
        }
        imask = MASKDEF >> bits << bits;
        return imask;
    }

    public int ip2int(String ip) {
        int value = 0;
        String[] arr = ip.split("\\.");
        for (String t : arr) {
            int v = Integer.parseInt(t);
            if (v < 0 || v > 255) {
                return 0;
            }
            value = value * IPSEC + v;
        }
        return value;
    }

    public boolean match(String ip) {
        int targetIp = ip2int(ip);
        return (iip == (targetIp & imask));
    }
}
