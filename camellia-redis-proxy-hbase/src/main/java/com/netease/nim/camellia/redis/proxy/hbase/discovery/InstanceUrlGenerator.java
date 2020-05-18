package com.netease.nim.camellia.redis.proxy.hbase.discovery;

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 *
 * Created by caojiajun on 2020/5/14.
 */
public interface InstanceUrlGenerator {

    String instanceUrl();

    public static class Default implements InstanceUrlGenerator {

        @Override
        public String instanceUrl() {
            try {
                return Inet4Address.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return null;
            }
        }
    }
}
