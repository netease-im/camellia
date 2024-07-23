package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.alibaba.fastjson.JSONObject;
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
    private ConsensusMasterSelector masterSelector;
    private ProxyNode master;
    private ProxyClusterSlotMap slotMap;

    private final ConcurrentHashSet<ProxyNode> nodes = new ConcurrentHashSet<>();
    private final ConcurrentHashSet<ProxyNode> pendingNodes = new ConcurrentHashSet<>();

    @Override
    public void init() {
        ClusterModeStatus.setStatus(ClusterModeStatus.Status.ONLINE);
        //init nodes
        nodes.addAll(initNodes());
        nodes.add(current());
        //init master selector
        masterSelector = new RedisConsensusMasterSelector(current());
        //get master
        while (true) {
            master = masterSelector.getMaster();
            if (master != null) {
                logger.info("master = {}", master);
                break;
            }
            logger.warn("master is null, waiting...");
            sleep10ms();
        }
        //init
        if (currentNodeMaster()) {
            initMaster();
        } else {
            initSlave();
        }
        //add master change listener
        addMasterChangeListener();
        //heartbeat to slave if current node is master
        startHeartbeatToSlave();
        //heartbeat to master if current node is slave
        startHeartbeatToMaster();
    }

    private void initMaster() {
        logger.info("current node is master");
        ProxyClusterSlotMap oldSlotMap = masterSelector.getSlotMap();
        logger.info("get slot map from master selector storage, slot-map = {}", oldSlotMap);
        ProxyClusterSlotMap newSlotMap;
        if (oldSlotMap == null) {
            newSlotMap = initSlotMap();
            logger.info("init slot map, slot-map = {}", newSlotMap);
        } else {
            newSlotMap = checkAndUpdateSlotMap(oldSlotMap);
            logger.info("check slot map from master selector storage, slot-map = {}", oldSlotMap);
        }
        updateSlotMap(oldSlotMap, newSlotMap, "initMaster");
    }

    private void initSlave() {
        logger.info("current node is slave");
        ProxyClusterSlotMap newSlotMap;
        while (true) {
            try {
                newSlotMap = getSlotMapFromMaster();
            } catch (Exception e) {
                logger.error("getSlotMapFromMaster error", e);
                sleep10ms();
                continue;
            }
            break;
        }
        updateSlotMap(this.slotMap, newSlotMap, "initSlave");
    }

    private void sleep10ms() {
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            logger.error(e.toString(), e);
        }
    }

    private void addMasterChangeListener() {
        masterSelector.addConsensusMasterChangeListener(() -> {
            master = masterSelector.getMaster();
            if (currentNodeMaster()) {
                initMaster();
            } else {
                initSlave();
            }
        });
    }

    private void startHeartbeatToSlave() {
        int intervalSeconds = ClusterModeConfig.clusterModeHeartbeatIntervalSeconds();
        schedule.scheduleAtFixedRate(this::sendHeartbeatToSlave0, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private void startHeartbeatToMaster() {
        int intervalSeconds = ClusterModeConfig.clusterModeHeartbeatIntervalSeconds();
        schedule.scheduleAtFixedRate(this::sendHeartbeatToMaster0, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    private ProxyClusterSlotMap getSlotMapFromMaster() {
        Reply reply;
        try {
            if (master.equals(current())) {
                return ProxyClusterSlotMapUtils.localSlotMap(current(), masterSelector.getSlotMap());
            }
            reply = sendCmd(master, ClusterModeCmd.send_get_slot_map_from_master, "{}");
            if (reply instanceof BulkReply) {
                String data = Utils.bytesToString(((BulkReply) reply).getRaw());
                return ProxyClusterSlotMapUtils.localSlotMap(current(), ProxyClusterSlotMap.parseString(data));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new CamelliaRedisException("getSlotMapFromMaster error", e);
        }
        throw new CamelliaRedisException("error " + reply);
    }

    private ProxyClusterSlotMap initSlotMap() {
        List<ProxyNode> onlineNodes = new ArrayList<>();
        for (ProxyNode node : nodes) {
            if (checkNode(node, true)) {
                onlineNodes.add(node);
            }
        }
        return ProxyClusterSlotMapUtils.uniformDistribution(current(), onlineNodes);
    }

    private ProxyClusterSlotMap checkAndUpdateSlotMap(ProxyClusterSlotMap slotMap) {
        List<ProxyNode> currentOnlineNodes = slotMap.getOnlineNodes();
        List<ProxyNode> onlineNodes = new ArrayList<>();
        for (ProxyNode node : currentOnlineNodes) {
            if (checkNode(node, true)) {
                onlineNodes.add(node);
            }
        }
        return ProxyClusterSlotMapUtils.uniformDistribution(current(), onlineNodes);
    }

    private boolean currentNodeMaster() {
        return current().equals(master);
    }

    private static final Set<ClusterModeCmd> slaveCmd = new HashSet<>();
    static {
        slaveCmd.add(ClusterModeCmd.send_heartbeat_to_slave);
        slaveCmd.add(ClusterModeCmd.send_slot_map_to_slave);
    }

    @Override
    public Reply proxyHeartbeat(Command command) {
        byte[][] objects = command.getObjects();
        ProxyNode source = ProxyNode.parseString(Utils.bytesToString(objects[2]));
        ClusterModeCmd cmd = ClusterModeCmd.getByValue((int)Utils.bytesToNum(objects[3]));
        JSONObject data = JSONObject.parseObject(Utils.bytesToString(objects[4]));

        if (slaveCmd.contains(cmd)) {
            //slave要处理的
            if (currentNodeMaster()) {
                return new ErrorReply("ERR target not slave");
            }
            if (cmd == ClusterModeCmd.send_heartbeat_to_slave) {
                return slaveReceiveMasterHeartbeat(source, data);
            }
            if (cmd == ClusterModeCmd.send_slot_map_to_slave) {
                return slaveReceiveNewSlotMap(source, data);
            }
        } else {
            //master要处理的
            if (!currentNodeMaster()) {
                return new ErrorReply("ERR target not master");
            }
            if (cmd == ClusterModeCmd.send_heartbeat_to_master) {
                return masterReceiveSlaveHeartbeat(source, data);
            }
            if (cmd == ClusterModeCmd.send_get_slot_map_from_master) {
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

    //=====slave=====

    private Reply slaveReceiveNewSlotMap(ProxyNode master, JSONObject data) {
        String string = data.getString("slotMap");
        String requestId = data.getString("requestId");
        ProxyClusterSlotMap slotMap = ProxyClusterSlotMap.parseString(string);
        ProxyClusterSlotMap newSlotMap = ProxyClusterSlotMapUtils.localSlotMap(current(), slotMap);
        String reason = "master-notify-new-slot-map,master=" + master + ",requestId=" + requestId;
        updateSlotMap(slotMap, newSlotMap, reason);
        return StatusReply.OK;
    }

    private Reply slaveReceiveMasterHeartbeat(ProxyNode source, JSONObject data) {
        String md5 = data.getString("md5");
        if (md5 != null && slotMap != null && !slotMap.getMd5().equals(md5)) {
            try {
                executor.submit(() -> {
                    ProxyClusterSlotMap newSlotMap = getSlotMapFromMaster();
                    updateSlotMap(slotMap, newSlotMap, "heartbeat-md5-check,master=" + source);
                });
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        JSONObject json = new JSONObject();
        json.put("status", ClusterModeStatus.getStatus().getValue());
        return new BulkReply(Utils.stringToBytes(json.toJSONString()));
    }

    //=====master=====

    private Reply masterReceiveSlaveHeartbeat(ProxyNode source, JSONObject data) {
        ClusterModeStatus.Status status = ClusterModeStatus.Status.getByValue(data.getIntValue("status"));
        nodes.add(source);
        if (slotMap.contains(source) && status == ClusterModeStatus.Status.ONLINE) {
            return StatusReply.OK;
        }
        if (!slotMap.contains(source) && status != ClusterModeStatus.Status.ONLINE) {
            return StatusReply.OK;
        }
        if (slotMap.contains(source) && status != ClusterModeStatus.Status.ONLINE) {
            pendingNodes.add(source);
            return StatusReply.OK;
        }
        if (!slotMap.contains(source) && status == ClusterModeStatus.Status.ONLINE) {
            pendingNodes.add(source);
            return StatusReply.OK;
        }
        logger.error("illegal slave heartbeat, source = {}, data = {}", source, data);
        return ErrorReply.SYNTAX_ERROR;
    }

    //====heartbeat=====

    private final ConcurrentHashMap<ProxyNode, AtomicLong> failedNodes = new ConcurrentHashMap<>();

    private void sendHeartbeatToSlave0() {
        try {
            if (!currentNodeMaster()) return;
            List<ProxyNode> currentOnlineNodes = slotMap.getOnlineNodes();

            Set<ProxyNode> checkNodes = new HashSet<>();
            checkNodes.addAll(currentOnlineNodes);
            checkNodes.addAll(pendingNodes);

            List<ProxyNode> offlineNodes = new ArrayList<>();
            List<ProxyNode> onlineNodes = new ArrayList<>();

            for (ProxyNode node : checkNodes) {
                if (!checkNode(node, false)) {
                    offlineNodes.add(node);
                } else {
                    onlineNodes.add(node);
                }
            }
            refreshSlotMap(slotMap, onlineNodes, offlineNodes);
        } catch (Exception e) {
            logger.error("sendHeartbeatToSlave0 error", e);
        }
    }

    private void sendHeartbeatToMaster0() {
        try {
            if (currentNodeMaster()) return;
            //send to master
            ProxyNode targetMaster = master;
            if (targetMaster.equals(current())) {
                return;
            }
            JSONObject data = new JSONObject();
            data.put("status", ClusterModeStatus.getStatus().getValue());
            Reply reply = sendCmd(targetMaster, ClusterModeCmd.send_heartbeat_to_master, data.toString());
            if (reply instanceof ErrorReply) {
                logger.error("send heartbeat to master error, master = {}, error = {}", targetMaster, ((ErrorReply) reply).getError());
            }
        } catch (Exception e) {
            logger.error("sendHeartbeatToMaster0 error", e);
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
            Reply reply = sendCmd(node, ClusterModeCmd.send_heartbeat_to_slave, data.toString());
            if (reply instanceof ErrorReply) {
                logger.error("send heartbeat to slave error, slave = {}, error = {}", node, ((ErrorReply) reply).getError());
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
            logger.error("send heartbeat to slave error, slave = {}", node, e);
        }
        AtomicLong count = CamelliaMapUtils.computeIfAbsent(failedNodes, node, k -> new AtomicLong());
        count.incrementAndGet();
        if (strict) {
            return false;
        }
        int threshold = ProxyDynamicConf.getInt("consensus.cluster.mode.heartbeat.max.fail.count", 3);
        return count.get() < threshold;
    }

    private void refreshSlotMap(ProxyClusterSlotMap slotMap, List<ProxyNode> onlineNodes, List<ProxyNode> offlineNodes) {
        ProxyClusterSlotMap newSlotMap = ProxyClusterSlotMapUtils.optimizeBalance(slotMap, current(), onlineNodes, offlineNodes);
        if (newSlotMap == null) {
            return;
        }

        updateSlotMap(this.slotMap, newSlotMap, "newSlotMap");

        String requestId = UUID.randomUUID().toString();

        Set<ProxyNode> notifyNodes = new HashSet<>();
        notifyNodes.addAll(nodes);
        notifyNodes.addAll(slotMap.getOnlineNodes());
        for (ProxyNode node : notifyNodes) {
            try {
                if (node.equals(current())) {
                    continue;
                }
                JSONObject data = new JSONObject();
                data.put("requestId", requestId);
                data.put("md5", newSlotMap.getMd5());
                data.put("slotMap", slotMap.toString());
                logger.info("send_slot_map_to_slave, target = {}, md5 = {}, requestId = {}", node, newSlotMap.getMd5(), requestId);
                sendCmd(node, ClusterModeCmd.send_slot_map_to_slave, data.toString());
            } catch (Exception e) {
                logger.error("send_slot_map_to_slave error, target = {}, md5 = {}, requestId = {}", node, newSlotMap.getMd5(), requestId, e);
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
            if (currentNodeMaster()) {
                masterSelector.saveSlotMap(slotMap);
            }
            logger.info("slot-map change, reason = {}, old-slot-map = {}\n, new-slot-map = {}\n",
                    reason, oldSlotMap == null ? null : oldSlotMap.toString(), newSlotMap);
            nodeChangeNotify();
        } finally {
            lock.unlock();
        }
    }

}
