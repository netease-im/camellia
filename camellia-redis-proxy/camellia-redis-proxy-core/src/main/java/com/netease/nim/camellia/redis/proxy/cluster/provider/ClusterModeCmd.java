package com.netease.nim.camellia.redis.proxy.cluster.provider;

/**
 * cluster proxy_heartbeat cmd content
 * <p>
 * Created by caojiajun on 2024/6/18
 */
public enum ClusterModeCmd {

    send_get_slot_map_from_leader(1),//从leader节点获取slot-map
    send_heartbeat_to_leader(2),//follower发送心跳给leader
    send_heartbeat_to_follower(3),//leader发送心跳给slave，发送md5给follower，follower返回status
    send_slot_map_to_follower(4),//leader发送新的slot-map给follower

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
