package com.netease.nim.camellia.redis.proxy.upstream.cluster;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedisClusterSlavesResource;
import com.netease.nim.camellia.redis.base.resource.RedissClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedissClusterSlavesResource;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionStatus;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2019/12/18.
 */
public class RedisClusterSlotInfo {

    private static final Logger logger = LoggerFactory.getLogger(RedisClusterSlotInfo.class);

    public static final int SLOT_SIZE = 16384;

    //slot -> master redis node
    private volatile Node[] masterSlotArray = new Node[SLOT_SIZE];
    private volatile Set<Node> masterNodeSet = new TreeSet<>(Comparator.comparing(o -> o.getAddr().getUrl()));
    private volatile List<Node> masterNodeList = new ArrayList<>();

    private volatile Map<Node, List<Node>> masterSlaveMap = new HashMap<>();
    private volatile Set<Node> slaveNodeSet = new HashSet<>();
    private volatile Set<Node> masterSlaveNodeSet = new HashSet<>();
    private volatile NodeWithSlaves[] nodeWithSlavesArray = new NodeWithSlaves[SLOT_SIZE];

    private final RedisClusterClient  redisClusterClient;
    private final Resource resource;
    private final Type type;
    private final String maskUrl;
    private final List<RedisClusterResource.Node> nodes;
    private final String userName;
    private final String password;


    public RedisClusterSlotInfo(RedisClusterResource resource, RedisClusterClient  redisClusterClient) {
        if (resource == null) {
            throw new CamelliaRedisException("resource is null");
        }
        this.redisClusterClient = redisClusterClient;
        this.resource = resource;
        this.maskUrl = PasswordMaskUtils.maskResource(resource.getUrl());
        this.nodes = resource.getNodes();
        this.password = resource.getPassword();
        this.userName = resource.getUserName();
        this.type = Type.MASTER_ONLY;
    }

    public RedisClusterSlotInfo(RedissClusterResource resource, RedisClusterClient  redisClusterClient) {
        if (resource == null) {
            throw new CamelliaRedisException("resource is null");
        }
        this.redisClusterClient = redisClusterClient;
        this.resource = resource;
        this.maskUrl = PasswordMaskUtils.maskResource(resource.getUrl());
        this.nodes = resource.getNodes();
        this.password = resource.getPassword();
        this.userName = resource.getUserName();
        this.type = Type.MASTER_ONLY;
    }

    public RedisClusterSlotInfo(RedisClusterSlavesResource resource, RedisClusterClient  redisClusterClient) {
        if (resource == null) {
            throw new CamelliaRedisException("resource is null");
        }
        this.redisClusterClient = redisClusterClient;
        this.resource = resource;
        this.maskUrl = PasswordMaskUtils.maskResource(resource.getUrl());
        this.nodes = resource.getNodes();
        this.password = resource.getPassword();
        this.userName = resource.getUserName();
        if (resource.isWithMaster()) {
            this.type = Type.MASTER_SLAVE;
        } else {
            this.type = Type.SLAVE_ONLY;
        }
    }

    public RedisClusterSlotInfo(RedissClusterSlavesResource resource, RedisClusterClient  redisClusterClient) {
        if (resource == null) {
            throw new CamelliaRedisException("resource is null");
        }
        this.redisClusterClient = redisClusterClient;
        this.resource = resource;
        this.maskUrl = PasswordMaskUtils.maskResource(resource.getUrl());
        this.nodes = resource.getNodes();
        this.password = resource.getPassword();
        this.userName = resource.getUserName();
        if (resource.isWithMaster()) {
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
        return resource;
    }

    /**
     * get connection by slot
     *
     * @param slot slot
     * @return connection
     */
    public RedisConnection getConnection(int slot) {
        Node node = getNode(slot);
        if (node == null) return null;
        return RedisConnectionHub.getInstance().get(redisClusterClient, node.getAddr());
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
                if (nodeWithSlaves == null) {
                    return null;
                }
                List<Node> slaves = nodeWithSlaves.getSlaves();
                return selectSlavesNode(slaves);
            } else if (type == Type.MASTER_SLAVE) {
                try {
                    NodeWithSlaves nodeWithSlaves = nodeWithSlavesArray[slot];
                    if (nodeWithSlaves == null) {
                        return masterSlotArray[slot];
                    } else {
                        return selectMasterSlavesNode(nodeWithSlaves.getMaster(), nodeWithSlaves.getSlaves());
                    }
                } catch (Exception e) {
                    return masterSlotArray[slot];
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisClusterSlotInfo.class, "getNode error, resource = " + maskUrl + ", slot = " + slot, e);
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
     * @param index index
     * @return RedisClient
     */
    public RedisConnection getConnectionByIndex(int index) {
        try {
            Node master = this.masterNodeList.get(index);
            if (master == null) return null;
            if (type == Type.MASTER_ONLY) {
                return RedisConnectionHub.getInstance().get(redisClusterClient, master.getAddr());
            } else if (type == Type.SLAVE_ONLY) {
                List<Node> slaves = masterSlaveMap.get(master);
                try {
                    if (slaves == null || slaves.isEmpty()) {
                        return null;
                    }
                    if (slaves.size() == 1) {
                        Node slave = slaves.getFirst();
                        return RedisConnectionHub.getInstance().get(redisClusterClient, slave.getAddr());
                    } else {
                        int i = ThreadLocalRandom.current().nextInt(slaves.size());
                        Node slave = slaves.get(i);
                        return RedisConnectionHub.getInstance().get(redisClusterClient, slave.getAddr());
                    }
                } catch (Exception e) {
                    Node slave = slaves.getFirst();
                    return RedisConnectionHub.getInstance().get(redisClusterClient, slave.getAddr());
                }
            } else if (type == Type.MASTER_SLAVE) {
                try {
                    List<Node> slaves = masterSlaveMap.get(master);
                    if (slaves == null || slaves.isEmpty()) {
                        return RedisConnectionHub.getInstance().get(redisClusterClient, master.getAddr());
                    }
                    int i = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
                    if (i == 0) {
                        return RedisConnectionHub.getInstance().get(redisClusterClient, master.getAddr());
                    }
                    Node slave = slaves.get(i - 1);
                    return RedisConnectionHub.getInstance().get(redisClusterClient, slave.getAddr());
                } catch (Exception e) {
                    return RedisConnectionHub.getInstance().get(redisClusterClient, master.getAddr());
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisClusterSlotInfo.class,
                    "getClientByIndex error, resource = " + maskUrl + ", index = " + index, e);
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

    /**
     * renew node list
     *
     * @return success/fail
     */
    public boolean renew() {
        try {
            boolean success = false;
            List<RedisClusterResource.Node> initNodes = new ArrayList<>(this.nodes);
            Collections.shuffle(initNodes);
            for (RedisClusterResource.Node node : initNodes) {
                success = tryRenew(node.getHost(), node.getPort(), userName, password);
                if (success) break;
            }
            if (!success) {
                List<Node> masterNodes = new ArrayList<>(this.masterNodeList);
                Collections.shuffle(masterNodes);
                for (Node node : masterNodes) {
                    success = tryRenew(node.getHost(), node.getPort(), userName, password);
                    if (success) break;
                }
            }
            if (!success) {
                List<Node> slaveNodes = new ArrayList<>(this.slaveNodeSet);
                Collections.shuffle(slaveNodes);
                for (Node node : slaveNodes) {
                    success = tryRenew(node.getHost(), node.getPort(), userName, password);
                    if (success) break;
                }
            }
            if (success) {
                logger.info("renew success, resource = {}, master-slave-info:\r\n{}", maskUrl, masterSlaveInfo());
            } else {
                ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew fail, resource = " + maskUrl);
            }
            return success;
        } catch (Exception e) {
            ErrorLogCollector.collect(RedisClusterSlotInfo.class, "renew error, resource = " + maskUrl, e);
            return false;
        }
    }

    public boolean isValid() {
        Map<Node, List<Node>> masterSlaveMap = new HashMap<>(this.masterSlaveMap);
        if (type == Type.MASTER_ONLY) {
            for (Node node : masterSlaveMap.keySet()) {
                if (!checkValid(node)) {
                    return false;
                }
            }
        } else if (type == Type.SLAVE_ONLY) {
            for (List<Node> list : masterSlaveMap.values()) {
                if (list == null || list.isEmpty()) {
                    return false;
                }
                boolean allDown = true;
                for (Node node : list) {
                    if (checkValid(node)) {
                        allDown = false;
                        break;
                    }
                }
                if (allDown) {
                    return false;
                }
            }
        } else if (type == Type.MASTER_SLAVE) {
            for (Map.Entry<Node, List<Node>> entry : masterSlaveMap.entrySet()) {
                Node master = entry.getKey();
                if (!checkValid(master)) {
                    List<Node> list = entry.getValue();
                    if (list == null || list.isEmpty()) {
                        return false;
                    }
                    boolean allDown = true;
                    for (Node slave : list) {
                        if (checkValid(slave)) {
                            allDown = false;
                            break;
                        }
                    }
                    if (allDown) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String masterSlaveInfo() {
        Map<Node, List<Node>> masterSlaveMap = new HashMap<>(this.masterSlaveMap);
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Node, List<Node>> entry : masterSlaveMap.entrySet()) {
            Node master = entry.getKey();
            builder.append("master=").append(master.getHost()).append(":").append(master.getPort());
            List<Node> slaves = entry.getValue();
            if (slaves != null && !slaves.isEmpty()) {
                int i = 0;
                for (Node slave : slaves) {
                    builder.append(",slave").append(i).append("=").append(slave.getHost()).append(":").append(slave.getPort());
                    i ++;
                }
            }
            builder.append("\r\n");
        }
        return builder.toString();
    }

    private boolean tryRenew(String host, int port, String userName, String password) {
        RedisConnection connection = null;
        try {
            connection = RedisConnectionHub.getInstance().newConnection(getResource(), host, port, userName, password);
            if (connection == null || !connection.isValid()) return false;
            CompletableFuture<Reply> future = connection.sendCommand(RedisCommand.CLUSTER.raw(), Utils.stringToBytes("slots"));
            logger.info("tryRenew, connection = {}, resource = {}", connection.getConnectionName(), maskUrl);
            Reply reply = future.get(10000, TimeUnit.MILLISECONDS);
            return clusterNodes(reply);
        } catch (Exception e) {
            logger.error("tryRenew error, host = {}, port = {}, resource = {}", host, port, maskUrl, e);
            return false;
        } finally {
            if (connection != null) {
                connection.stop(true);
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
                logger.debug("node.size = {}, resource = {}", masterNodeSet.size(), maskUrl);
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
                logger.error("slot size is {}, not {}, resource = {}", size, SLOT_SIZE, maskUrl);
            }
            return success;
        } catch (CamelliaRedisException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaRedisException(e);
        }
    }

    private Node selectMasterSlavesNode(Node masterNode, List<Node> slaves) {
        if (slaves == null || slaves.isEmpty()) return masterNode;
        try {
            if (masterNode == null) {
                return selectSlavesNode(slaves);
            }
            int maxLoop = slaves.size() + 1;
            int index = ThreadLocalRandom.current().nextInt(maxLoop);
            for (int i=0; i<maxLoop; i++) {
                try {
                    Node node;
                    if (index == 0) {
                        node = masterNode;
                    } else {
                        node = slaves.get(index - 1);
                    }
                    if (checkValid(node)) {
                        return node;
                    }
                    index = index + 1;
                    if (index == slaves.size() + 1) {
                        index = 0;
                    }
                } catch (Exception e) {
                    index = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
                }
            }
            index = ThreadLocalRandom.current().nextInt(slaves.size() + 1);
            if (index == 0) {
                return masterNode;
            }
            return slaves.get(index - 1);
        } catch (Exception e) {
            try {
                if (masterNode != null) {
                    return masterNode;
                } else if (!slaves.isEmpty()) {
                    return slaves.getFirst();
                }
                return null;
            } catch (Exception ex) {
                ErrorLogCollector.collect(RedisClusterSlotInfo.class, "selectMasterSlavesNode error, resource = " + maskUrl, ex);
                return null;
            }
        }
    }

    private Node selectSlavesNode(List<Node> slaves) {
        if (slaves == null || slaves.isEmpty()) return null;
        if (slaves.size() == 1) return slaves.getFirst();
        try {
            int maxLoop = slaves.size();
            int index = ThreadLocalRandom.current().nextInt(maxLoop);
            for (int i=0; i<maxLoop; i++) {
                try {
                    Node node = slaves.get(index);
                    if (checkValid(node)) {
                        return node;
                    }
                    index = index + 1;
                    if (index == slaves.size()) {
                        index = 0;
                    }
                } catch (Exception e) {
                    index = ThreadLocalRandom.current().nextInt(slaves.size());
                }
            }
            index = ThreadLocalRandom.current().nextInt(slaves.size());
            return slaves.get(index);
        } catch (Exception e) {
            try {
                return slaves.getFirst();
            } catch (Exception ex) {
                ErrorLogCollector.collect(RedisClusterSlotInfo.class, "selectSlavesNode error, resource = " + maskUrl, ex);
                return null;
            }
        }
    }

    private boolean checkValid(Node node) {
        if (node == null) return false;
        RedisConnection redisConnection = RedisConnectionHub.getInstance().get(redisClusterClient, node.getAddr());
        if (redisConnection == null) {
            return false;
        }
        return redisConnection.getStatus() == RedisConnectionStatus.VALID;
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
        private final RedisConnectionAddr addr;

        public Node(String host, int port, String userName, String password, boolean readonly) {
            this.host = host;
            this.port = port;
            this.userName = userName;
            this.password = password;
            this.readonly = readonly;
            this.addr = new RedisConnectionAddr(host, port, userName, password, readonly);
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

        public RedisConnectionAddr getAddr() {
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
