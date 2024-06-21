package com.netease.nim.camellia.redis.proxy.cluster.provider;

/**
 * cluster proxy_heartbeat cmd content
 * <p>
 * Created by caojiajun on 2024/6/18
 */
public enum ClusterModeCmd {

    send_get_master_from_config_nodes(1),//获取master节点
    send_get_slot_map_from_master(2),//从master节点获取slot-map
    send_heartbeat_to_master(3),//slave发送心跳给master
    send_heartbeat_to_slave(4),//master发送心跳给slave
    send_prepare_to_slave(5),//master发送prepare指令给slave
    send_prepare_ok_to_master(6),//slave发送prepare_ok指令给master
    send_commit_to_slave(7),//master发送commit指令给slave
    send_commit_ok_to_master(8),//slave发送commit_ok指令给slave

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
