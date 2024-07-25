package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.alibaba.fastjson.JSONObject;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.cluster.*;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.util.ConcurrentHashSet;
import com.netease.nim.camellia.redis.proxy.util.ConfigInitUtil;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by caojiajun on 2024/6/18
 */
public class ConsensusProxyClusterModeProvider extends AbstractProxyClusterModeProvider {

    private static final Logger logger = LoggerFactory.getLogger(ConsensusProxyClusterModeProvider.class);

    private final ReentrantLock lock = new ReentrantLock();
    private ConsensusLeaderSelector leaderSelector;
    private volatile ProxyNode leader;
    private volatile ProxyClusterSlotMap slotMap;

    private final ConcurrentHashSet<ProxyNode> pendingNodes = new ConcurrentHashSet<>();

    @Override
    public void init() {
        ClusterModeStatus.setStatus(ClusterModeStatus.Status.ONLINE);
        //init nodes
        initNodes(false);
        current();
        //init leader selector
        String className = ProxyDynamicConf.getString("cluster.mode.consensus.leader.selector.class.name", RedisConsensusLeaderSelector.class.getName());
        leaderSelector = ConfigInitUtil.initConsensusLeaderSelector(className);
        leaderSelector.init(current(), new ArrayList<>(initNodes(false)));
        //get leader
        long sleepMs = 10;
        while (true) {
            leader = leaderSelector.getLeader();
            if (leader != null) {
                logger.info("leader = {}", leader);
                break;
            }
            logger.warn("leader is null, waiting...");
            sleep(sleepMs);
            sleepMs = Math.min(sleepMs * 2, 500);
        }
        //add leader change listener
        addLeaderChangeListener();
        //init
        if (currentNodeLeader()) {
            initLeader();
        } else {
            initFollower();
        }
        //heartbeat to follower if current node is leader
        startHeartbeatToSlave();
        //heartbeat to leader if current node is follower
        startHeartbeatToMaster();
    }

    private void initLeader() {
        logger.info("current node is leader");
        ProxyClusterSlotMap oldSlotMap = leaderSelector.getSlotMap();
        logger.info("get slot map from leader selector storage, slot-map = {}", oldSlotMap);
        ProxyClusterSlotMap newSlotMap;
        if (oldSlotMap == null) {
            newSlotMap = initSlotMap();
            logger.info("init slot map, slot-map = {}", newSlotMap);
        } else {
            newSlotMap = checkAndUpdateSlotMap(oldSlotMap);
            logger.info("check slot map from leader selector storage, slot-map = {}", oldSlotMap);
        }
        updateSlotMap(oldSlotMap, newSlotMap, "initLeader");
    }

    private void initFollower() {
        logger.info("current node is follower");
        ProxyClusterSlotMap newSlotMap;
        long sleepMs = 10;
        while (true) {
            try {
                newSlotMap = getSlotMapFromLeader();
            } catch (Exception e) {
                logger.warn("getSlotMapFromLeader error, waiting...");
                sleep(sleepMs);
                sleepMs = Math.min(sleepMs * 2, 1000);
                continue;
            }
            break;
        }
        updateSlotMap(this.slotMap, newSlotMap, "initFollower");
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            logger.error(e.toString(), e);
        }
    }

    private void addLeaderChangeListener() {
        leaderSelector.addConsensusLeaderChangeListener(() -> {
            leader = leaderSelector.getLeader();
            if (currentNodeLeader()) {
                initLeader();
            } else {
                initFollower();
            }
        });
    }

    private void startHeartbeatToSlave() {
        int intervalSeconds = ClusterModeConfig.clusterModeHeartbeatIntervalSeconds();
        schedule.scheduleAtFixedRate(this::sendHeartbeatToFollower0, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void startHeartbeatToMaster() {
        int intervalSeconds = ClusterModeConfig.clusterModeHeartbeatIntervalSeconds();
        schedule.scheduleAtFixedRate(this::sendHeartbeatToLeader0, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private ProxyClusterSlotMap getSlotMapFromLeader() {
        Reply reply;
        try {
            if (leader.equals(current())) {
                return ProxyClusterSlotMapUtils.localSlotMap(current(), leaderSelector.getSlotMap());
            }
            reply = sendCmd(leader, ClusterModeCmd.send_get_slot_map_from_leader, "{}");
            if (reply instanceof BulkReply) {
                String data = Utils.bytesToString(((BulkReply) reply).getRaw());
                return ProxyClusterSlotMapUtils.localSlotMap(current(), ProxyClusterSlotMap.parseString(data));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaRedisException("getSlotMapFromLeader error", e);
        }
        throw new CamelliaRedisException("error " + reply);
    }

    private ProxyClusterSlotMap initSlotMap() {
        List<ProxyNode> onlineNodes = new ArrayList<>();

        Set<ProxyNode> checkNodes = new HashSet<>(initNodes(false));
        checkNodes.add(current());
        checkNodes.addAll(pendingNodes);
        for (ProxyNode node : checkNodes) {
            if (checkNode(node, true)) {
                onlineNodes.add(node);
            }
        }
        return ProxyClusterSlotMapUtils.uniformDistribution(current(), onlineNodes);
    }

    private ProxyClusterSlotMap checkAndUpdateSlotMap(ProxyClusterSlotMap slotMap) {
        List<ProxyNode> currentOnlineNodes = slotMap.getOnlineNodes();
        List<ProxyNode> onlineNodes = new ArrayList<>();
        List<ProxyNode> offlineNodes = new ArrayList<>();
        for (ProxyNode node : currentOnlineNodes) {
            if (checkNode(node, true)) {
                onlineNodes.add(node);
            } else {
                offlineNodes.add(node);
            }
        }
        return ProxyClusterSlotMapUtils.optimizeBalance(slotMap, current(), onlineNodes, offlineNodes);
    }

    private boolean currentNodeLeader() {
        return current().equals(leader);
    }

    private static final Set<ClusterModeCmd> followerCmd = new HashSet<>();
    static {
        followerCmd.add(ClusterModeCmd.send_heartbeat_to_follower);
        followerCmd.add(ClusterModeCmd.send_slot_map_to_follower);
    }

    @Override
    public Reply proxyHeartbeat(Command command) {
        byte[][] objects = command.getObjects();
        ProxyNode source = ProxyNode.parseString(Utils.bytesToString(objects[2]));
        ClusterModeCmd cmd = ClusterModeCmd.getByValue((int)Utils.bytesToNum(objects[3]));
        JSONObject data = JSONObject.parseObject(Utils.bytesToString(objects[4]));

        if (followerCmd.contains(cmd)) {
            //follower要处理的
            if (currentNodeLeader()) {
                return new ErrorReply("ERR target not follower");
            }
            if (cmd == ClusterModeCmd.send_heartbeat_to_follower) {
                return followerReceiveLeaderHeartbeat(source, data);
            }
            if (cmd == ClusterModeCmd.send_slot_map_to_follower) {
                return followerReceiveNewSlotMap(source, data);
            }
        } else {
            //leader要处理的
            if (!currentNodeLeader()) {
                return new ErrorReply("ERR target not leader");
            }
            if (cmd == ClusterModeCmd.send_heartbeat_to_leader) {
                return leaderReceiveSlaveHeartbeat(source, data);
            }
            if (cmd == ClusterModeCmd.send_get_slot_map_from_leader) {
                String string = slotMap.toString();
                return new BulkReply(Utils.stringToBytes(string));
            }
        }
        logger.error("unknown cmd = {}", cmd);
        return ErrorReply.SYNTAX_ERROR;
    }

    @Override
    public ProxyClusterSlotMap load() {
        return slotMap;
    }

    //=====follower=====

    private Reply followerReceiveNewSlotMap(ProxyNode leader, JSONObject data) {
        String string = data.getString("slotMap");
        String requestId = data.getString("requestId");
        ProxyClusterSlotMap slotMap = ProxyClusterSlotMap.parseString(string);
        ProxyClusterSlotMap newSlotMap = ProxyClusterSlotMapUtils.localSlotMap(current(), slotMap);
        String reason = "leader-notify-new-slot-map|leader=" + leader + "|requestId=" + requestId;
        updateSlotMap(this.slotMap, newSlotMap, reason);
        return StatusReply.OK;
    }

    private Reply followerReceiveLeaderHeartbeat(ProxyNode leader, JSONObject data) {
        String md5 = data.getString("md5");
        if (md5 != null && slotMap != null && !slotMap.getMd5().equals(md5)) {
            try {
                executor.submit(() -> {
                    ProxyClusterSlotMap newSlotMap = getSlotMapFromLeader();
                    updateSlotMap(slotMap, newSlotMap, "heartbeat-md5-check|leader=" + leader);
                });
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        JSONObject json = new JSONObject();
        json.put("status", ClusterModeStatus.getStatus().getValue());
        return new BulkReply(Utils.stringToBytes(json.toJSONString()));
    }

    //=====leader=====

    private Reply leaderReceiveSlaveHeartbeat(ProxyNode follower, JSONObject data) {
        pendingNodes.add(follower);
        if (logger.isDebugEnabled()) {
            logger.debug("leader receive follower heartbeat, follower = {}, data = {}", follower, data);
        }
        return StatusReply.OK;
    }

    //====heartbeat=====

    private final ConcurrentLinkedHashMap<ProxyNode, AtomicLong> failedNodes = new ConcurrentLinkedHashMap.Builder<ProxyNode, AtomicLong>()
            .initialCapacity(1000)
            .maximumWeightedCapacity(10000)
            .build();

    private final AtomicLong step = new AtomicLong();
    private Set<ProxyNode> pendingNodes() {
        HashSet<ProxyNode> proxyNodes = new HashSet<>(pendingNodes);
        if (step.incrementAndGet() >= 10) {
            pendingNodes.clear();
            step.set(0);
        }
        return proxyNodes;
    }

    private void sendHeartbeatToFollower0() {
        try {
            if (!currentNodeLeader()) return;
            List<ProxyNode> currentOnlineNodes = slotMap.getOnlineNodes();

            Set<ProxyNode> checkNodes = new HashSet<>();
            checkNodes.addAll(currentOnlineNodes);
            checkNodes.addAll(pendingNodes());

            List<ProxyNode> offlineNodes = new ArrayList<>();
            List<ProxyNode> onlineNodes = new ArrayList<>();

            for (ProxyNode node : checkNodes) {
                if (!checkNode(node, false)) {
                    offlineNodes.add(node);
                } else {
                    onlineNodes.add(node);
                }
            }
            refreshSlotMap(slotMap, onlineNodes, offlineNodes, checkNodes);
        } catch (Exception e) {
            logger.error("sendHeartbeatToFollower0 error", e);
        }
    }

    private void sendHeartbeatToLeader0() {
        try {
            if (currentNodeLeader()) return;
            //send to leader
            ProxyNode targetLeader = leader;
            if (targetLeader.equals(current())) {
                return;
            }
            JSONObject data = new JSONObject();
            data.put("status", ClusterModeStatus.getStatus().getValue());
            Reply reply = sendCmd(targetLeader, ClusterModeCmd.send_heartbeat_to_leader, data.toString());
            if (reply instanceof ErrorReply) {
                logger.error("send heartbeat to leader error, leader = {}, error = {}", targetLeader, ((ErrorReply) reply).getError());
            }
        } catch (Exception e) {
            logger.error("sendHeartbeatToLeader0 error", e);
        }
    }

    //=================

    private boolean checkNode(ProxyNode node, boolean strict) {
        if (node.equals(current())) {
            return ClusterModeStatus.getStatus() == ClusterModeStatus.Status.ONLINE;
        }
        JSONObject data = new JSONObject();
        if (slotMap != null) {
            data.put("md5", slotMap.getMd5());
        }
        try {
            Reply reply = sendCmd(node, ClusterModeCmd.send_heartbeat_to_follower, data.toString());
            if (reply instanceof ErrorReply) {
                logger.error("send heartbeat to follower error, follower = {}, error = {}", node, ((ErrorReply) reply).getError());
            }
            if (reply instanceof BulkReply) {
                JSONObject json = JSONObject.parseObject(Utils.bytesToString(((BulkReply) reply).getRaw()));
                ClusterModeStatus.Status status = ClusterModeStatus.Status.getByValue(json.getInteger("status"));
                boolean online = status == ClusterModeStatus.Status.ONLINE;
                AtomicLong count = CamelliaMapUtils.computeIfAbsent(failedNodes, node, k -> new AtomicLong());
                count.set(0);
                return online;
            }
        } catch (Exception e) {
            logger.error("send heartbeat to follower error, follower = {}", node, e);
        }
        AtomicLong count = CamelliaMapUtils.computeIfAbsent(failedNodes, node, k -> new AtomicLong());
        count.incrementAndGet();
        if (strict) {
            return false;
        }
        int threshold = ProxyDynamicConf.getInt("consensus.cluster.mode.heartbeat.max.fail.count", 3);
        return count.get() < threshold;
    }

    private void refreshSlotMap(ProxyClusterSlotMap slotMap, List<ProxyNode> onlineNodes, List<ProxyNode> offlineNodes, Set<ProxyNode> checkNodes) {
        ProxyClusterSlotMap newSlotMap = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, current(), onlineNodes, offlineNodes);
        if (newSlotMap == null) {
            return;
        }

        updateSlotMap(this.slotMap, newSlotMap, "newSlotMap");

        String requestId = UUID.randomUUID().toString();

        Set<ProxyNode> notifyNodes = new HashSet<>();
        notifyNodes.addAll(slotMap.getOnlineNodes());
        notifyNodes.addAll(newSlotMap.getOnlineNodes());
        notifyNodes.addAll(checkNodes);
        for (ProxyNode node : notifyNodes) {
            try {
                if (node.equals(current())) {
                    continue;
                }
                JSONObject data = new JSONObject();
                data.put("requestId", requestId);
                data.put("md5", newSlotMap.getMd5());
                data.put("slotMap", newSlotMap.toString());
                logger.info("send_slot_map_to_follower, target = {}, md5 = {}, requestId = {}", node, newSlotMap.getMd5(), requestId);
                sendCmd(node, ClusterModeCmd.send_slot_map_to_follower, data.toString());
            } catch (Exception e) {
                logger.error("send_slot_map_to_follower error, target = {}, md5 = {}, requestId = {}", node, newSlotMap.getMd5(), requestId, e);
            }
        }
    }

    private Reply sendCmd(ProxyNode target, ClusterModeCmd cmd, String data) {
        try {
            byte[][] args = new byte[][]{RedisCommand.CLUSTER.raw(), RedisKeyword.PROXY_HEARTBEAT.getRaw(), Utils.stringToBytes(current().toString()),
                    Utils.stringToBytes(String.valueOf(cmd.getValue())), Utils.stringToBytes(data)};
            RedisConnection connection = RedisConnectionHub.getInstance().get(toAddr(target));
            CompletableFuture<Reply> future = connection.sendCommand(args);
            return sync(future);
        } catch (Exception e) {
            logger.error("send cmd error, target = {}, cmd = {}, data = {}", target, cmd, data, e);
            throw new CamelliaRedisException(e);
        }
    }

    private void updateSlotMap(ProxyClusterSlotMap oldSlotMap, ProxyClusterSlotMap newSlotMap, String reason) {
        lock.lock();
        try {
            if (newSlotMap == null) {
                logger.warn("new slot map null, skip update");
                return;
            }
            if (oldSlotMap != null && oldSlotMap.getMd5().equals(newSlotMap.getMd5())) {
                return;
            }
            this.slotMap = ProxyClusterSlotMapUtils.localSlotMap(current(), newSlotMap);
            if (currentNodeLeader()) {
                leaderSelector.saveSlotMap(slotMap);
            }
            logger.info("slot-map change, reason = {}, md5 = {} -> {}, size = {} -> {}\nold-slot-map = \n{}\nnew-slot-map = \n{}\n",
                    reason, oldSlotMap == null ? null : oldSlotMap.getMd5(), newSlotMap.getMd5(),
                    oldSlotMap == null ? 0 : oldSlotMap.getOnlineNodes().size(), newSlotMap.getOnlineNodes().size(),
                    oldSlotMap == null ? null : oldSlotMap.toString(), newSlotMap);
            slotMapChangeNotify();
        } finally {
            lock.unlock();
        }
    }

}
