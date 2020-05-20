package com.netease.nim.camellia.hbase.connection;

import com.netease.nim.camellia.hbase.conf.CamelliaHBaseConf;
import com.netease.nim.camellia.hbase.resource.HBaseResource;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public interface CamelliaHBaseConnectionFactory {

    /**
     * 获取HBaseConnection
     * @param hBaseResource hbase地址
     * @return HBaseConnection对象
     */
    CamelliaHBaseConnection getHBaseConnection(HBaseResource hBaseResource);

    /**
     * 一个默认实现
     */
    CamelliaHBaseConnectionFactory DEFAULT = new DefaultHBaseConnectionFactory();

    public static class DefaultHBaseConnectionFactory implements CamelliaHBaseConnectionFactory {

        private CamelliaHBaseConf hBaseConf = new CamelliaHBaseConf();
        private final ConcurrentHashMap<String, CamelliaHBaseConnection> map = new ConcurrentHashMap<>();

        private final Object lock = new Object();

        public DefaultHBaseConnectionFactory() {
        }

        public DefaultHBaseConnectionFactory(CamelliaHBaseConf hBaseConf) {
            this.hBaseConf = hBaseConf;
        }

        @Override
        public CamelliaHBaseConnection getHBaseConnection(HBaseResource hBaseResource) {
            String url = hBaseResource.getUrl();
            CamelliaHBaseConnection hBaseConnection = map.get(url);
            if (hBaseConnection == null) {
                synchronized (lock) {
                    hBaseConnection = map.get(url);
                    if (hBaseConnection == null) {
                        hBaseConnection = new CamelliaHBaseConnection(hBaseResource, hBaseConf);
                        map.put(url, hBaseConnection);
                    }
                }
            }
            return hBaseConnection;
        }
    }
}
