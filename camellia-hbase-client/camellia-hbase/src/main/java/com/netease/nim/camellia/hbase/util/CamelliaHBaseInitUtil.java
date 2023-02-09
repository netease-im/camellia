package com.netease.nim.camellia.hbase.util;

import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.hbase.CamelliaHBaseEnv;
import com.netease.nim.camellia.hbase.conf.CamelliaHBaseConf;
import com.netease.nim.camellia.hbase.conf.HBaseConstants;
import com.netease.nim.camellia.hbase.connection.CamelliaHBaseConnectionFactory;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;

import java.net.URL;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2020/3/23.
 */
public class CamelliaHBaseInitUtil {

    public static CamelliaHBaseEnv initHBaseEnvFromHBaseFile(String hbaseXmlFile) {
        return initHBaseEnvFromHBaseFile(hbaseXmlFile, ProxyEnv.defaultProxyEnv());
    }

    public static CamelliaHBaseEnv initHBaseEnvFromHBaseFile(String hbaseXmlFile, ProxyEnv proxyEnv) {
        CamelliaHBaseConf camelliaHBaseConf = initHBaseConfFromFile(hbaseXmlFile);
        CamelliaHBaseConnectionFactory connectionFactory = new CamelliaHBaseConnectionFactory.DefaultHBaseConnectionFactory(camelliaHBaseConf);
        return new CamelliaHBaseEnv.Builder()
                .connectionFactory(connectionFactory)
                .proxyEnv(proxyEnv)
                .build();
    }

    public static HBaseResource initHBaseResourceFromFile(String hbaseXmlFile) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(hbaseXmlFile);
        if (url == null) {
            throw new IllegalArgumentException(hbaseXmlFile + " not exists");
        }
        Configuration configuration = HBaseConfiguration.create();
        configuration.addResource(new Path(url.getPath()));
        CamelliaHBaseConf camelliaHBaseConf = new CamelliaHBaseConf();
        String zk = null;
        String zkParent = null;
        String userName = null;
        String password = null;
        Boolean lindorm = null;
        for (Map.Entry<String, String> entry : configuration) {
            if (entry.getKey().equalsIgnoreCase(HBaseConstants.ZK)) {
                zk = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase(HBaseConstants.ZK_PARENT)) {
                zkParent = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase(HBaseConstants.USER_NAME)) {
                userName = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase(HBaseConstants.PASSWORD)) {
                password = entry.getValue();
            } else if (entry.getKey().equalsIgnoreCase(HBaseConstants.HBASE_CLIENT_CONNECTION_IMPL)
                    && entry.getValue().equalsIgnoreCase(HBaseConstants.HBASE_CLIENT_CONNECTION_LINDORM_IMPL)) {
                lindorm = true;
            }
            else {
                camelliaHBaseConf.addConf(entry.getKey(), entry.getValue());
            }
        }
        if (zk == null) {
            throw new IllegalArgumentException("missing '" + HBaseConstants.ZK + "'");
        }
        if (zkParent == null) {
            throw new IllegalArgumentException("missing '" + HBaseConstants.ZK_PARENT + "'");
        }
        return new HBaseResource(zk, zkParent, userName, password, lindorm);
    }

    public static CamelliaHBaseConf initHBaseConfFromFile(String hbaseXmlFile) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(hbaseXmlFile);
        if (url == null) {
            return new CamelliaHBaseConf();
        }
        Configuration configuration = HBaseConfiguration.create();
        configuration.addResource(new Path(url.getPath()));
        CamelliaHBaseConf camelliaHBaseConf = new CamelliaHBaseConf();
        for (Map.Entry<String, String> entry : configuration) {
            if (!entry.getKey().equalsIgnoreCase(HBaseConstants.ZK) && !entry.getKey().equalsIgnoreCase(HBaseConstants.ZK_PARENT)
                    && !entry.getKey().equalsIgnoreCase(HBaseConstants.USER_NAME) && !entry.getKey().equalsIgnoreCase(HBaseConstants.PASSWORD)) {
                camelliaHBaseConf.addConf(entry.getKey(), entry.getValue());
            }
        }
        return camelliaHBaseConf;
    }
}
