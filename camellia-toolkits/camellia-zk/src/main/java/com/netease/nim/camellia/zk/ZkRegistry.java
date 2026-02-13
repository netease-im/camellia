package com.netease.nim.camellia.zk;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.discovery.ServerNode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2020/8/12
 */
public class ZkRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZkRegistry.class);

    private final String zkUrl;
    private int sessionTimeoutMs = ZkConstants.sessionTimeoutMs;
    private int connectionTimeoutMs = ZkConstants.connectionTimeoutMs;
    private int baseSleepTimeMs = ZkConstants.baseSleepTimeMs;
    private int maxRetries = ZkConstants.maxRetries;

    private final String basePath;
    private final String applicationName;
    private final ServerNode serverNode;
    private final String id = UUID.randomUUID().toString().replaceAll("-", "");

    private CuratorFramework client;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean registerOk;
    private boolean deregister = false;

    public ZkRegistry(String zkUrl, String basePath, String applicationName, ServerNode serverNode) {
        this.zkUrl = zkUrl;
        this.basePath = basePath;
        this.applicationName = applicationName;
        this.serverNode = serverNode;
        init();
    }

    public ZkRegistry(String zkUrl, int sessionTimeoutMs, int connectionTimeoutMs,
                      int baseSleepTimeMs, int maxRetries, String basePath, String applicationName, ServerNode serverNode) {
        this.zkUrl = zkUrl;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.baseSleepTimeMs = baseSleepTimeMs;
        this.maxRetries = maxRetries;
        this.basePath = basePath;
        this.applicationName = applicationName;
        this.serverNode = serverNode;
        init();
    }

    private void init() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(zkUrl)
                .sessionTimeoutMs(sessionTimeoutMs)
                .connectionTimeoutMs(connectionTimeoutMs)
                .retryPolicy(new ExponentialBackoffRetry(baseSleepTimeMs, maxRetries))
                .build();
        client.start();
        this.client = client;
        client.getConnectionStateListenable().addListener((curatorFramework, connectionState) -> {
            if (connectionState == ConnectionState.LOST) {
                logger.warn("zk connectionState LOST");
                while (true) {
                    try {
                        if (curatorFramework.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
                            if (!deregister) {
                                registerOk = false;
                                register();
                            }
                            break;
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                deregister();
            } catch (Exception e) {
                logger.error("deregister error, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(),  serverNode);
            }
        }));
    }

    private String registerPath() {
        return basePath + "/" + applicationName + "/" + id;
    }

    public void register() {
        if (running.compareAndSet(false, true)) {
            try {
                deregister = false;
                if (registerOk) return;
                while (true) {
                    try {
                        byte[] data = InstanceInfoSerializeUtil.serialize(serverNode);
                        client.create().creatingParentContainersIfNeeded()
                                .withMode(CreateMode.EPHEMERAL).forPath(registerPath(), data);
                        registerOk = true;
                        logger.info("register to zk success, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(serverNode));
                        break;
                    } catch (KeeperException.NodeExistsException e) {
                        try {
                            byte[] data = client.getData().forPath(registerPath());
                            ServerNode node = InstanceInfoSerializeUtil.deserialize(data);
                            if (node != null) {
                                if (Objects.equals(node, serverNode)) {
                                    logger.info("has register to zk, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(serverNode));
                                    break;
                                }
                            }
                            client.delete().forPath(registerPath());
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    } catch (Exception e) {
                        throw new ZkRegistryException(e);
                    }
                }
            } finally {
                running.compareAndSet(true, false);
            }
        }
    }

    public void deregister() {
        if (running.compareAndSet(false, true)) {
            try {
                deregister = true;
                if (!registerOk) return;
                client.delete().forPath(registerPath());
                registerOk = false;
                logger.info("deregister to zk, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(serverNode));
            } catch (KeeperException.NoNodeException e) {
                registerOk = false;
                logger.info("not register to zk, skip deregister, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(serverNode));
            } catch (Exception e) {
                throw new ZkRegistryException(e);
            } finally {
                running.compareAndSet(true, false);
            }
        }
    }

    public boolean isRegisterOk() {
        return registerOk;
    }

    public ServerNode getServerNode() {
        return serverNode;
    }
}
