package com.netease.nim.camellia.tools.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * Created by hzcaojiajun on 2020/08/12.
 */
public class InetUtils {

    private static final Logger logger = LoggerFactory.getLogger(InetUtils.class);

    public static List<String> ignoredInterfaces = new ArrayList<>();

    public static List<String> preferredNetworks = new ArrayList<>();

    public static InetAddress findFirstNonLoopbackAddress() {
        return findFirstNonLoopbackAddress(ignoredInterfaces, preferredNetworks);
    }

    public static InetAddress findFirstNonLoopbackAddress(String ignoredInterfaces, String preferredNetworks) {
        return findFirstNonLoopbackAddress(strToList(ignoredInterfaces), strToList(preferredNetworks));
    }

    public static InetAddress findFirstNonLoopbackAddress(List<String> ignoredInterfaces, List<String> preferredNetworks) {
        InetAddress result = null;
        try {
            int lowest = Integer.MAX_VALUE;
            for (Enumeration<NetworkInterface> nics = NetworkInterface
                    .getNetworkInterfaces(); nics.hasMoreElements();) {
                NetworkInterface ifc = nics.nextElement();
                if (ifc.isUp()) {
                    if (ifc.getIndex() < lowest || result == null) {
                        lowest = ifc.getIndex();
                    } else {
                        continue;
                    }

                    if (!ignoreInterface(ignoredInterfaces, ifc.getDisplayName())) {
                        for (Enumeration<InetAddress> addrs = ifc.getInetAddresses(); addrs.hasMoreElements();) {
                            InetAddress address = addrs.nextElement();
                            if (address instanceof Inet4Address
                                    && !address.isLoopbackAddress()
                                    && !ignoreAddress(preferredNetworks, address)) {
                                result = address;
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("Cannot get first non-loopback address", ex);
        }

        if (result != null) {
            return result;
        }

        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            logger.warn("Unable to retrieve localhost");
        }

        return null;
    }

    private static boolean ignoreInterface(List<String> ignoredInterfaces, String interfaceName) {
        for (String regex : ignoredInterfaces) {
            if (interfaceName.matches(regex)) {
                logger.trace("Ignoring interface: {}", interfaceName);
                return true;
            }
        }
        return false;
    }

    private static boolean ignoreAddress(List<String> preferredNetworks, InetAddress address) {
        for (String regex : preferredNetworks) {
            if (!address.getHostAddress().matches(regex) && !address.getHostAddress().startsWith(regex)) {
                logger.trace("Ignoring address: {}", address.getHostAddress());
                return true;
            }
        }
        return false;
    }

    private static List<String> strToList(String str) {
        List<String> list = new ArrayList<>();
        if (str != null) {
            String[] split = str.split(",");
            for (String string : split) {
                if (string == null) {
                    continue;
                }
                string = string.trim();
                if (string.isEmpty()) {
                    continue;
                }
                list.add(string);
            }
        }
        return list;
    }
}
