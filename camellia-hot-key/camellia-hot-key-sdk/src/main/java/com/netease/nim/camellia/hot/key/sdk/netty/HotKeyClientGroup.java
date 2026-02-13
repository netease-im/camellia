package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2023/5/17
 */
public class HotKeyClientGroup {

    private final List<HotKeyClient> clientList = new CopyOnWriteArrayList<>();
    private final int connectNum;
    private final ServerNode addr;
    private final HotKeyPackConsumer consumer;

    public HotKeyClientGroup(ServerNode addr, HotKeyPackConsumer consumer, int connectNum) {
        this.addr = addr;
        this.consumer = consumer;
        this.connectNum = connectNum;
        addIfNotFull();
    }

    /**
     * 关闭所有连接
     */
    public synchronized void stop() {
        for (HotKeyClient client : clientList) {
            client.stop();
        }
    }

    /**
     * 只要有一个有效连接，就认为可用
     * @return 是否有效
     */
    public boolean isValid() {
        if (clientList.isEmpty()) {
            return false;
        }
        for (HotKeyClient client : clientList) {
            if (client.isValid()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端列表
     * @return 列表
     */
    public List<HotKeyClient> getClientList() {
        return new ArrayList<>(clientList);
    }

    /**
     * 移除一个客户端
     * @param client 客户端
     */
    public synchronized void remove(HotKeyClient client) {
        clientList.remove(client);
    }

    /**
     * 如果连接数不足，则补足
     */
    public synchronized void addIfNotFull() {
        clientList.removeIf(client -> !client.isValid());
        if (clientList.size() < connectNum) {
            int addNum = connectNum - clientList.size();
            for (int i=0; i<addNum; i++) {
                HotKeyClient client = new HotKeyClient(addr, consumer);
                if (client.isValid()) {
                    clientList.add(client);
                }
            }
        }
    }

    /**
     * 选择一个可用的连接
     * @return 客户端
     */
    public HotKeyClient select() {
        if (clientList.isEmpty()) {
            return null;
        }
        try {
            int retry = 3;
            while (retry-- > 0) {
                try {
                    //随机选择
                    int index = ThreadLocalRandom.current().nextInt(clientList.size());
                    HotKeyClient client = clientList.get(index);
                    if (client != null && client.isValid()) {
                        return client;
                    }
                    if (client != null && !client.isValid()) {
                        //如果连接不可用，则移除掉
                        client.stop();
                        remove(client);
                    }
                } catch (Exception e) {
                    return clientList.get(0);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
