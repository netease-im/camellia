package com.netease.nim.camellia.redis.proxy.cluster.provider;

/**
 * cluster proxy_heartbeat cmd content
 * <p>
 * Created by caojiajun on 2024/6/18
 */
public enum ClusterModeCmd {

    send_get_slot_map_from_master(1),//从master节点获取slot-map
    send_heartbeat_to_master(2),//slave发送心跳给master
    send_heartbeat_to_slave(3),//master发送心跳给slave，发送md5给slave，slave返回status
    send_slot_map_to_slave(4),//master发送新的slot-map给slave

    ;

    private final int value;

    ClusterModeCmd(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ClusterModeCmd getByValue(int value) {
        for (ClusterModeCmd cmd : ClusterModeCmd.values()) {
            if (cmd.value == value) {
                return cmd;
            }
        }
        return null;
    }
}
