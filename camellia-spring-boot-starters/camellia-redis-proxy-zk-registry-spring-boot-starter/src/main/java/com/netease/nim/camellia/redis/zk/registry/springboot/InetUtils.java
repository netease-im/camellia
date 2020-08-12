package com.netease.nim.camellia.redis.zk.registry.springboot;

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

                    if (!ignoreInterface(ifc.getDisplayName())) {
                        for (Enumeration<InetAddress> addrs = ifc.getInetAddresses(); addrs.hasMoreElements();) {
                            InetAddress address = addrs.nextElement();
                            if (address instanceof Inet4Address
                                    && !address.isLoopbackAddress()
                                    && !ignoreAddress(address)) {
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

    private static boolean ignoreInterface(String interfaceName) {
        for (String regex : ignoredInterfaces) {
            if (interfaceName.matches(regex)) {
                logger.trace("Ignoring interface: " + interfaceName);
                return true;
            }
        }
        return false;
    }

    private static boolean ignoreAddress(InetAddress address) {
        for (String regex : preferredNetworks) {
            if (!address.getHostAddress().matches(regex) && !address.getHostAddress().startsWith(regex)) {
                logger.trace("Ignoring address: " + address.getHostAddress());
                return true;
            }
        }
        return false;
    }

}
