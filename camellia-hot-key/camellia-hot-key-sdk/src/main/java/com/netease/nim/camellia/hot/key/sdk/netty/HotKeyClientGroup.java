package com.netease.nim.camellia.hot.key.sdk.netty;

import com.netease.nim.camellia.hot.key.common.netty.HotKeyPackConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2023/5/17
 */
public class HotKeyClientGroup {

    private final List<HotKeyClient> clientList = new ArrayList<>();
    private final int connectNum;
    private final HotKeyServerAddr addr;
    private final HotKeyPackConsumer consumer;

    public HotKeyClientGroup(HotKeyServerAddr addr, HotKeyPackConsumer consumer, int connectNum) {
        this.addr = addr;
        this.consumer = consumer;
        this.connectNum = connectNum;
        addIfNotFull();
    }

    public synchronized void stop() {
        for (HotKeyClient client : clientList) {
            client.stop();
        }
    }

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

    public List<HotKeyClient> getClientList() {
        return new ArrayList<>(clientList);
    }

    public synchronized void remove(HotKeyClient client) {
        clientList.remove(client);
    }

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

    public HotKeyClient select() {
        if (clientList.isEmpty()) {
            return null;
        }
        try {
            int retry = 3;
            while (retry-- > 0) {
                try {
                    int index = ThreadLocalRandom.current().nextInt(clientList.size());
                    HotKeyClient client = clientList.get(index);
                    if (client != null && client.isValid()) {
                        return client;
                    }
                    if (client != null && !client.isValid()) {
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
