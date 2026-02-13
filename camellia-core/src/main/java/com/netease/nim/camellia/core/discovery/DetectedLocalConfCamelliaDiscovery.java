package com.netease.nim.camellia.core.discovery;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/10/17
 */
public class DetectedLocalConfCamelliaDiscovery extends AbstractCamelliaDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(DetectedLocalConfCamelliaDiscovery.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SysUtils.getCpuNum(),
            new CamelliaThreadFactory("detected-local-discovery"));

    private final AtomicBoolean running = new AtomicBoolean(false);

    private List<ServerNode> originalList;
    private List<ServerNode> aliveList = new ArrayList<>();

    public DetectedLocalConfCamelliaDiscovery(List<ServerNode> list) {
        init(list, 10);
    }

    public DetectedLocalConfCamelliaDiscovery(List<ServerNode> list, int detectIntervalSeconds) {
        init(list, detectIntervalSeconds);
    }

    private void init(List<ServerNode> list, int detectIntervalSeconds) {
        this.originalList = list;
        checkAndUpdate();
        if (aliveList.isEmpty()) {
            throw new IllegalArgumentException("all node is not alive");
        }
        scheduler.scheduleAtFixedRate(this::checkAndUpdate, detectIntervalSeconds, detectIntervalSeconds, TimeUnit.SECONDS);
    }

    private void checkAndUpdate() {
        if (running.compareAndSet(false, true)) {
            try {
                List<ServerNode> aliveList = new ArrayList<>();
                for (ServerNode node : originalList) {
                    if (checkAlive(node)) {
                        aliveList.add(node);
                    }
                }

                if (aliveList.isEmpty()) {
                    logger.warn("all node is not alive, node.list = {}", originalList);
                    return;
                }

                Set<ServerNode> newSet = new HashSet<>(aliveList);
                Set<ServerNode> oldSet = new HashSet<>(this.aliveList);

                //new - old = add
                Set<ServerNode> addSet = new HashSet<>(newSet);
                addSet.removeAll(oldSet);
                if (!addSet.isEmpty()) {
                    List<ServerNode> added = new ArrayList<>(addSet);
                    Collections.shuffle(added);
                    //callback add
                    for (ServerNode node : added) {
                        try {
                            invokeAddCallback(node);
                        } catch (Exception e) {
                            logger.error("callback add error", e);
                        }
                    }
                }

                //old - new = remove
                Set<ServerNode> removeSet = new HashSet<>(oldSet);
                removeSet.removeAll(newSet);
                if (!removeSet.isEmpty()) {
                    List<ServerNode> removed = new ArrayList<>(removeSet);
                    Collections.shuffle(removed);
                    //callback remove
                    for (ServerNode node : removed) {
                        try {
                            invokeRemoveCallback(node);
                        } catch (Exception e) {
                            logger.error("callback remove error", e);
                        }
                    }
                }

                this.aliveList = new ArrayList<>(aliveList);
            } catch (Exception e) {
                logger.error("schedule error", e);
            } finally {
                running.compareAndSet(true, false);
            }
        }
    }

    private boolean checkAlive(ServerNode node) {
        try (Socket socket = new Socket()) {
            try {
                socket.connect(new InetSocketAddress(node.getHost(), node.getPort()), 200);
                return true;
            } catch (Exception e) {
                logger.warn("check {} error, ex = {}", node, e.toString());
                return false;
            }
        } catch (Exception e) {
            logger.error("close error", e);
            return false;
        }
    }


    @Override
    public List<ServerNode> findAll() {
        return new ArrayList<>(aliveList);
    }

}
