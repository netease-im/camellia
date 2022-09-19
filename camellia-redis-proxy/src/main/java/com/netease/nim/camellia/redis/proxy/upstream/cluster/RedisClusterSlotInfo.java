package com.netease.nim.camellia.redis.proxy.upstream.cluster;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.util.SysUtils;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientAddr;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClientHub;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.redis.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.resource.RedisClusterSlavesResource;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClusterSlotInfo {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterSlotInfo.class);

    private static final ExecutorService redisClusterRenewExec = new ThreadPoolExecutor(SysUtils.getCpuNum(), SysUtils.getCpuNum(), 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024), new DefaultThreadFactory("redis-cluster-renew"), new ThreadPoolExecutor.AbortPolicy());

    public static final int SLOT_SIZE = 16384;

    //slot -> master redis node
    private volatile Node[] masterSlotArray = new Node[SLOT_SIZE];
    private volatile Set<Node> masterNodeSet = new TreeSet<>(Comparator.comparing(o -> o.getAddr().getUrl()));
    private volatile List<Node> masterNodeList = new ArrayList<>();

    private volatile Map<Node, List<Node>> masterSlaveMap = new HashMap<>();
    private volatile Set<Node> slaveNodeSet = new HashSet<>();
    private volatile Set<Node> masterSlaveNodeSet = new HashSet<>();
    private volatile NodeWithSlaves[] nodeWithSlavesArray = new NodeWithSlaves[SLOT_SIZE];

    private final Type type;

    private RedisClusterResource redisClusterResource;
    private RedisClusterSlavesResource redisClusterSlavesResource;
    private final String maskUrl;
    private final List<RedisClusterResource.Node> nodes;
    private final String userName;
    private final String password;

    public RedisClusterSlotInfo(RedisClusterResource redisClusterResource) {
        if (redisClusterResource == null) {
            throw new CamelliaRedisException("redisClusterResource is null");
        }
        this.redisClusterResource = redisClusterResource;
        this.maskUrl = PasswordMaskUtils.maskResource(redisClusterResource.getUrl());
        this.nodes = redisClusterResource.getNodes();
        this.password = redisClusterResource.getPassword();
        this.userName = redisClusterResource.getUserName();
        this.type = Type.MASTER_ONLY;
    }

    public RedisClusterSlotInfo(RedisClusterSlavesResource redisClusterSlavesResource) {
        if (redisClusterSlavesResource == null) {
            throw new CamelliaRedisException("redisClusterSlavesResource is null");
        }
        this.redisClusterSlavesResource = redisClusterSlavesResource;
        this.maskUrl = PasswordMaskUtils.maskResource(redisClusterSlavesResource.getUrl());
        this.nodes = redisClusterSlavesResource.getNodes();
        this.password = redisClusterSlavesResource.getPassword();
        this.userName = redisClusterSlavesResource.getUserName();
        if (redisClusterSlavesResource.isWithMaster()) {
            this.type = Type.MASTER_SLAVE;
        } else {
            this.type = Type.SLAVE_ONLY;
        }
    }

    private enum Type {
        MASTER_ONLY,
        SLAVE_ONLY,
        MASTER_SLAVE,
        ;
    }

    /**
     * get resource
     * @return resource
     */
    public Resource getResource() {
        if (redisClusterResource != null) return redisClusterResource;
        if (redisClusterSlavesResource != null) return redisClusterSlavesResource;
        return null;
    }

    /**
     * get client by slot
     *
     * @param slot slot
     * @return client
     */
    public RedisClient getClient(int slot) {
        Node node = getNode(slot);
        if (node == null) return null;
        return RedisClientHub.get(node.getAddr());
    }

    /**
     * get node by slot
     *
     * @param slot slot
     * @return node
     */
    public Node getNode(int slot) {
        try {
            if (type == Type.MASTER_ONLY) {
                return masterSlotArray[slot];
            } else if (type == Type.SLAVE_ONLY) {
                NodeWithSlaves nodeWithSlaves = nodeWithSlavesArray[slot];
                if (nodeWithSlaves == null) return null;
                List<Node> slaves = nodeWithSlaves.getSlaves();
                if (slaves == null || slaves.isEmpty()) {
                    return null;
                }
                if (slaves.size() == 1) {
                    return slaves.get(0);
                } else {
                    try {
                        int i = ThreadLocalRandom.current().nextInt(slaves.size());
                        return slaves.get(i);
                    } catch (Exception e) {
                        return slaves.get(0);
                    }
                }
            } else if (type == Type.MASTER_SLAVE) {
                try {
                    NodeWithSlaves nodeWithSlaves = nodeWithSlavesArray[slot];
                    if (nodeWithSlaves == null) {
                        return masterSlotArray[slot];
                    } else {
                        Node master = nodeWithSlaves.getMaster();
                        List<Node> slaves = nodeWithSlaves.getSlaves();
                        Node node;
                        if (slaves == null || slaves.isEmpty()) {
                            node = master;
                        } else {
                            if (master == null) {
                                if (slaves.size() == 1) {
                                    node = slaves.get(0);
                                } else {
                                    int i = ThreadLocalRandom.current().nextInt(slaves.size());
                                    node = slaves.get(i);
                                }
                            } else {
                                int i = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
                                if (i == 0) {
                                    node = master;
                                } else {
                                    node = slaves.get(i - 1);
                                }
                            }
                        }
                        return node;
                    }
                } catch (Exception e) {
                    return masterSlotArray[slot];
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisClusterSlotInfo.class,
                    "getNode error, url = " + maskUrl + ", slot = " + slot, e);
            return null;
        }
    }

    /**
     * get all nodes
     *
     * @return nodes
     */
    public Set<Node> getNodes() {
        if (type == Type.MASTER_ONLY) {
            return masterNodeSet;
        } else if (type == Type.SLAVE_ONLY) {
            return slaveNodeSet;
        } else if (type == Type.MASTER_SLAVE) {
            return masterSlaveNodeSet;
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * get masterSlaveMap
     *
     * @return masterSlaveMap
     */
    public Map<Node, List<Node>> getMasterSlaveMap() {
        return masterSlaveMap;
    }

    /**
     * get client by index, node list is sorted by url.
     *
     * @return RedisClient
     */
    public RedisClient getClientByIndex(int index) {
        try {
            Node master = this.masterNodeList.get(index);
            if (master == null) return null;
            if (type == Type.MASTER_ONLY) {
                return RedisClientHub.get(master.getAddr());
            } else if (type == Type.SLAVE_ONLY) {
                List<Node> slaves = masterSlaveMap.get(master);
                try {
                    if (slaves == null || slaves.isEmpty()) {
                        return null;
                    }
                    if (slaves.size() == 1) {
                        Node slave = slaves.get(0);
                        return RedisClientHub.get(slave.getAddr());
                    } else {
                        int i = ThreadLocalRandom.current().nextInt(slaves.size());
                        Node slave = slaves.get(i);
                        return RedisClientHub.get(slave.getAddr());
                    }
                } catch (Exception e) {
                    Node slave = slaves.get(0);
                    return RedisClientHub.get(slave.getAddr());
                }
            } else if (type == Type.MASTER_SLAVE) {
                try {
                    List<Node> slaves = masterSlaveMap.get(master);
                    if (slaves == null || slaves.isEmpty()) {
                        return RedisClientHub.get(master.getAddr());
                    }
                    int i = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
                    if (i == 0) {
                        return RedisClientHub.get(master.getAddr());
                    }
                    Node slave = slaves.get(i - 1);
                    return RedisClientHub.get(slave.getAddr());
                } catch (Exception e) {
                    return RedisClientHub.get(master.getAddr());
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisClusterSlotInfo.class,
                    "getClientByIndex error, url = " + maskUrl + ", index = " + index, e);
            return null;
        }
    }

    /**
     * get nodes size
     *
     * @return size
     */
    public Integer getNodesSize() {
        return masterNodeList.size();
    }

    private long lastRenewTimestamp = 0L;
    private final AtomicBoolean renew = new AtomicBoolean(false);

    /**
     * renew node list
     *
     * @return success/fail
     */
    public Future<Boolean> renew() {
        //限制1s内最多renew一次
        if (TimeCache.currentMillis - lastRenewTimestamp < 1000) {
            return null;
        }
        if (renew.compareAndSet(false, true)) {
            try {
                return redisClusterRenewExec.submit(() -> {
                    try {
                        boolean success = false;
                        for (Node node : masterNodeSet) {
                            success = tryRenew(node.getHost(), node.getPort(), userName, password);
                            if (success) break;
                        }
                        if (!success) {
                            for (RedisClusterResource.Node node : nodes) {
                                success = tryRenew(node.getHost(), node.getPort(), userName, password);
                                if (success) break;
                            }
                        }
                        if (success) {
                            logger.info("renew success, url = {}", maskUrl);
                        } else {
                            ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew fail, url = " + maskUrl);
                        }
                        lastRenewTimestamp = TimeCache.currentMillis;
                        return success;
                    } catch (Exception e) {
                        ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew error, url = " + maskUrl, e);
                        return false;
                    } finally {
                        renew.set(false);
                    }
                });
            } catch (Exception e) {
                ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew error, url = " + maskUrl, e);
                renew.set(false);
            }
        }
        return null;
    }

    private boolean tryRenew(String host, int port, String userName, String password) {
        RedisClient client = null;
        try {
            client = RedisClientHub.newClient(host, port, userName, password);
            if (client == null || !client.isValid()) return false;
            CompletableFuture<Reply> future = client.sendCommand(RedisCommand.CLUSTER.raw(), Utils.stringToBytes("slots"));
            logger.info("tryRenew, client = {}, url = {}", client.getClientName(), maskUrl);
            Reply reply = future.get(10000, TimeUnit.MILLISECONDS);
            return clusterNodes(reply);
        } catch (Exception e) {
            logger.error("tryRenew error, host = {}, port = {}, url = {}", host, port, maskUrl, e);
            return false;
        } finally {
            if (client != null) {
                client.stop(true);
            }
        }
    }

    private boolean clusterNodes(Reply reply) {
        try {
            Node[] masterSlotArray = new Node[SLOT_SIZE];
            NodeWithSlaves[] nodeWithSlavesArray = new NodeWithSlaves[SLOT_SIZE];
            Set<Node> masterNodeSet = new TreeSet<>(Comparator.comparing(o -> o.getAddr().getUrl()));
            Map<Node, List<Node>> masterSlaveMap = new HashMap<>();

            int size = 0;
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                for (Reply reply1 : replies) {

                    MultiBulkReply reply2 = (MultiBulkReply) reply1;
                    Reply[] replies1 = reply2.getReplies();
                    IntegerReply slotStart = (IntegerReply) replies1[0];
                    IntegerReply slotEnd = (IntegerReply) replies1[1];
                    MultiBulkReply master = (MultiBulkReply) replies1[2];
                    Reply[] replies2 = master.getReplies();
                    BulkReply host = (BulkReply) replies2[0];
                    IntegerReply port = (IntegerReply) replies2[1];
                    Node masterNode = new Node(Utils.bytesToString(host.getRaw()), port.getInteger().intValue(), userName, password, false);
                    masterNodeSet.add(masterNode);

                    List<Node> slaveNodeList = new ArrayList<>();
                    if (replies1.length > 3) {
                        for (int i=3; i<replies1.length; i++) {
                            MultiBulkReply slave = (MultiBulkReply) replies1[i];
                            Reply[] replies3 = slave.getReplies();
                            BulkReply slaveHost = (BulkReply) replies3[0];
                            IntegerReply slavePort = (IntegerReply) replies3[1];
                            Node slaveNode = new Node(Utils.bytesToString(slaveHost.getRaw()), slavePort.getInteger().intValue(), userName, password, true);
                            slaveNodeList.add(slaveNode);
                        }
                    }
                    masterSlaveMap.put(masterNode, slaveNodeList);
                    NodeWithSlaves nodeWithSlaves = new NodeWithSlaves(masterNode, slaveNodeList);

                    for (long i = slotStart.getInteger(); i <= slotEnd.getInteger(); i++) {
                        masterSlotArray[(int) i] = masterNode;
                        nodeWithSlavesArray[(int) i] = nodeWithSlaves;
                        size++;
                    }
                }
            } else if (reply instanceof ErrorReply) {
                throw new CamelliaRedisException(((ErrorReply) reply).getError());
            } else {
                throw new CamelliaRedisException("decode clusterNodes error");
            }
            boolean success = size == SLOT_SIZE;
            if (logger.isDebugEnabled()) {
                logger.debug("node.size = {}, url = {}", masterNodeSet.size(), maskUrl);
            }
            if (!masterNodeSet.isEmpty()) {
                this.masterNodeSet = masterNodeSet;
                this.masterNodeList = new ArrayList<>(masterNodeSet);
            }
            if (!masterSlaveMap.isEmpty()) {
                this.masterSlaveMap = masterSlaveMap;
                Set<Node> slaveNodeSet = new HashSet<>();
                Set<Node> masterSlaveNodeSet = new HashSet<>();
                for (Map.Entry<Node, List<Node>> entry : masterSlaveMap.entrySet()) {
                    masterSlaveNodeSet.add(entry.getKey());
                    if (!entry.getValue().isEmpty()) {
                        slaveNodeSet.addAll(entry.getValue());
                        masterSlaveNodeSet.addAll(entry.getValue());
                    }
                }
                this.slaveNodeSet = slaveNodeSet;
                this.masterSlaveNodeSet = masterSlaveNodeSet;
            }
            if (size > 0) {
                this.masterSlotArray = masterSlotArray;
                this.nodeWithSlavesArray = nodeWithSlavesArray;
            }
            if (!success) {
                logger.error("slot size is {}, not {}, url = {}", size, SLOT_SIZE, maskUrl);
            }
            return success;
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    public static class NodeWithSlaves {
        private final Node master;
        private final List<Node> slaves;

        public NodeWithSlaves(Node master, List<Node> slaves) {
            this.master = master;
            this.slaves = slaves;
        }

        public Node getMaster() {
            return master;
        }

        public List<Node> getSlaves() {
            return slaves;
        }
    }

    public static class Node {
        private final String host;
        private final int port;
        private final String password;
        private final String userName;
        private final boolean readonly;
        private final RedisClientAddr addr;

        public Node(String host, int port, String userName, String password, boolean readonly) {
            this.host = host;
            this.port = port;
            this.userName = userName;
            this.password = password;
            this.readonly = readonly;
            this.addr = new RedisClientAddr(host, port, userName, password, readonly);
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getPassword() {
            return password;
        }

        public RedisClientAddr getAddr() {
            return addr;
        }

        public String getUserName() {
            return userName;
        }

        public boolean isReadonly() {
            return readonly;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(addr, node.addr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(addr);
        }

        @Override
        public String toString() {
            return addr.getUrl();
        }
    }
}
