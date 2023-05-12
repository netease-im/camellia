package com.netease.nim.camellia.zk;

import com.alibaba.fastjson.JSONObject;
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
public class ZkRegistry<T> {

    private static final Logger logger = LoggerFactory.getLogger(ZkRegistry.class);

    private final String zkUrl;
    private int sessionTimeoutMs = ZkConstants.sessionTimeoutMs;
    private int connectionTimeoutMs = ZkConstants.connectionTimeoutMs;
    private int baseSleepTimeMs = ZkConstants.baseSleepTimeMs;
    private int maxRetries = ZkConstants.maxRetries;

    private final String basePath;
    private final String applicationName;
    private final T instance;
    private final String id = UUID.randomUUID().toString().replaceAll("-", "");

    private CuratorFramework client;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private boolean registerOk;
    private boolean deregister = false;
    private InstanceInfo<T> instanceInfo;

    public ZkRegistry(String zkUrl, String basePath, String applicationName, T instance) {
        this.zkUrl = zkUrl;
        this.basePath = basePath;
        this.applicationName = applicationName;
        this.instance = instance;
        init();
    }

    public ZkRegistry(String zkUrl, int sessionTimeoutMs, int connectionTimeoutMs,
                      int baseSleepTimeMs, int maxRetries, String basePath, String applicationName, T instance) {
        this.zkUrl = zkUrl;
        this.sessionTimeoutMs = sessionTimeoutMs;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.baseSleepTimeMs = baseSleepTimeMs;
        this.maxRetries = maxRetries;
        this.basePath = basePath;
        this.applicationName = applicationName;
        this.instance = instance;
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
        this.instanceInfo = new InstanceInfo<>();
        instanceInfo.setInstance(instance);
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
                logger.error("deregister error, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(),  JSONObject.toJSONString(instanceInfo));
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
                        instanceInfo.setRegisterTime(System.currentTimeMillis());
                        byte[] data = InstanceInfoSerializeUtil.serialize(instanceInfo);
                        client.create().creatingParentContainersIfNeeded()
                                .withMode(CreateMode.EPHEMERAL).forPath(registerPath(), data);
                        registerOk = true;
                        logger.info("register to zk success, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(instanceInfo));
                        break;
                    } catch (KeeperException.NodeExistsException e) {
                        try {
                            byte[] data = client.getData().forPath(registerPath());
                            InstanceInfo<?> instanceInfo = InstanceInfoSerializeUtil.deserialize(data);
                            if (instanceInfo != null) {
                                if (Objects.equals(instanceInfo.getInstance(), this.instanceInfo.getInstance())) {
                                    this.instanceInfo.setRegisterTime(instanceInfo.getRegisterTime());
                                    logger.info("has register to zk, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(instanceInfo));
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
                instanceInfo.setRegisterTime(-1);
                logger.info("deregister to zk, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(instanceInfo));
            } catch (KeeperException.NoNodeException e) {
                registerOk = false;
                instanceInfo.setRegisterTime(-1);
                logger.info("not register to zk, skip deregister, zk = {}, path = {}, instanceInfo = {}", zkUrl, registerPath(), JSONObject.toJSONString(instanceInfo));
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

    public InstanceInfo<T> getInstanceInfo() {
        return instanceInfo;
    }
}
