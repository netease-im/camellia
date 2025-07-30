package com.netease.nim.camellia.hbase.connection;

import com.netease.nim.camellia.hbase.conf.CamelliaHBaseConf;
import com.netease.nim.camellia.hbase.conf.HBaseConstants;
import com.netease.nim.camellia.hbase.exception.CamelliaHBaseException;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2020/3/20.
 */
public class CamelliaHBaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaHBaseConnection.class);

    private final HBaseResource hBaseResource;
    private final CamelliaHBaseConf hBaseConf;

    private Configuration configuration;

    private Connection connection;

    public CamelliaHBaseConnection(HBaseResource hBaseResource, CamelliaHBaseConf hBaseConf) {
        this.hBaseResource = hBaseResource;
        this.hBaseConf = hBaseConf;
        init();
    }

    private void init() {
        try {
            Configuration configuration = new Configuration();
            if (hBaseConf != null) {
                for (Map.Entry<String, String> entry : hBaseConf.getConfMap().entrySet()) {
                    configuration.set(entry.getKey(), entry.getValue());
                }
            }
            if (hBaseResource.isObkv()) {
                configuration.set(HBaseConstants.HBASE_OCEANBASE_FULL_USER_NAME, hBaseResource.getObkvFullUserName());
                configuration.set(HBaseConstants.HBASE_OCEANBASE_PASSWORD, hBaseResource.getObkvPassword());
                configuration.set(HBaseConstants.HBASE_OCEANBASE_PARAM_URL, hBaseResource.getObkvParamUrl());
                configuration.set(HBaseConstants.HBASE_OCEANBASE_SYS_USER_NAME, hBaseResource.getObkvSysUserName());
                configuration.set(HBaseConstants.HBASE_OCEANBASE_SYS_PASSWORD, hBaseResource.getObkvSysPassword());
                configuration.set(HBaseConstants.HBASE_CLIENT_CONNECTION_IMPL, HBaseConstants.HBASE_CLIENT_CONNECTION_OBKV_IMPL);
            } else {
                configuration.set(HBaseConstants.ZK, hBaseResource.getZk());
                configuration.set(HBaseConstants.ZK_PARENT, hBaseResource.getZkParent());
                if (hBaseResource.getUserName() != null) {
                    configuration.set(HBaseConstants.USER_NAME, hBaseResource.getUserName());
                }
                if (hBaseResource.getPassword() != null) {
                    configuration.set(HBaseConstants.PASSWORD, hBaseResource.getPassword());
                }
                if (hBaseResource.isLindorm()) {
                    if (hBaseConf == null || !hBaseConf.getConfMap().containsKey(HBaseConstants.HBASE_CLIENT_CONNECTION_IMPL)) {
                        configuration.set(HBaseConstants.HBASE_CLIENT_CONNECTION_IMPL, HBaseConstants.HBASE_CLIENT_CONNECTION_LINDORM_IMPL);
                    }
                }
                Map<String, String> configMap = hBaseResource.getConfigMap();
                if (configMap != null && !configMap.isEmpty()) {
                    for (Map.Entry<String, String> entry : configMap.entrySet()) {
                        configuration.set(entry.getKey(), entry.getValue());
                    }
                }
            }
            this.configuration = configuration;
            this.connection = ConnectionFactory.createConnection(configuration);
        } catch (IOException e) {
            throw new CamelliaHBaseException(e);
        }
    }

    public Table getTable(String tableName) {
        try {
            return connection.getTable(TableName.valueOf(tableName));
        } catch (IOException e) {
            logger.error("getTable error, hbaseResource = {}, tableName = {}", hBaseResource.getUrl(), tableName, e);
            onException();
            throw new CamelliaHBaseException("get table = " + tableName + " error", e);
        }
    }

    private final AtomicBoolean replaceConnection = new AtomicBoolean(false);
    private void onException() {
        if (!replaceConnection.compareAndSet(false, true)) return;
        try {
            logger.warn("try reconnect...");
            Connection newConnection = ConnectionFactory.createConnection(configuration);
            Connection oldConnection = connection;
            logger.warn("new connect ok");
            connection = newConnection;
            if (oldConnection != null && !oldConnection.isClosed()) {
                oldConnection.close();
                logger.warn("old connect close");
            }
        } catch (Exception e) {
            logger.error("create new Connection error", e);
        } finally {
            replaceConnection.compareAndSet(true, false);
        }
    }

}
