package com.netease.nim.camellia.hbase.conf;

/**
 *
 * Created by caojiajun on 2020/3/23.
 */
public class HBaseConstants {

    public static final String ZK = "hbase.zookeeper.quorum";
    public static final String ZK_PARENT = "zookeeper.znode.parent";
    public static final String USER_NAME = "hbase.client.username";
    public static final String PASSWORD = "hbase.client.password";
    public static final String HBASE_CLIENT_CONNECTION_IMPL = "hbase.client.connection.impl";
    public static final String HBASE_CLIENT_CONNECTION_LINDORM_IMPL = "org.apache.hadoop.hbase.client.AliHBaseUEClusterConnection";
}
