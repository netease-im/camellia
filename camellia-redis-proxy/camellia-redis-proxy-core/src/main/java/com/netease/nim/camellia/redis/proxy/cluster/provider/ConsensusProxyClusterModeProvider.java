package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.cluster.*;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.command.Command;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/6/18
 */
public class ConsensusProxyClusterModeProvider extends AbstractProxyClusterModeProvider {

    private static final Logger logger = LoggerFactory.getLogger(ConsensusProxyClusterModeProvider.class);

    private ConsensusMasterSelector masterSelector;
    private ProxyNode master;
    private ProxyClusterSlotMap slotMap;

    private final ConcurrentHashSet<ProxyNode> nodes = new ConcurrentHashSet<>();

    @Override
    public void init() {
        //init nodes
        initNodes();
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
        notifySlotMapChange(oldSlotMap, newSlotMap);
    }

    private void initSlave() {
        logger.info("current node is slave");
        while (true) {
            slotMap = getSlotMapFromMaster();
            if (slotMap == null) {
                logger.warn("get slot map from master null, waiting");
                sleep10ms();
                continue;
            }
            logger.info("get slot map from master, slot-map = {}", slotMap);
            nodeChangeNotify();
            break;
        }
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
        if (master.equals(current())) {
            return masterSelector.getSlotMap();
        }
        Reply reply = sendCmd(master, ClusterModeCmd.send_get_slot_map_from_master, "{}");
        if (reply instanceof BulkReply) {
            String data = Utils.bytesToString(((BulkReply) reply).getRaw());
            return ProxyClusterSlotMap.parseString(data);
        }
        throw new CamelliaRedisException("error " + reply);
    }

    private void notifySlotMapChange(ProxyClusterSlotMap oldSlotMap, ProxyClusterSlotMap newSlotMap) {
        this.slotMap = newSlotMap;
        this.masterSelector.saveSlotMap(slotMap);
        //todo
        nodeChangeNotify();
    }

    private ProxyClusterSlotMap initSlotMap() {
        return null;
    }

    private ProxyClusterSlotMap checkAndUpdateSlotMap(ProxyClusterSlotMap slotMap) {
        return null;
    }

    private boolean currentNodeMaster() {
        return current().equals(master);
    }

    private static final Set<ClusterModeCmd> slaveCmd = new HashSet<>();
    static {
        slaveCmd.add(ClusterModeCmd.send_heartbeat_to_slave);
        slaveCmd.add(ClusterModeCmd.send_prepare_to_slave);
        slaveCmd.add(ClusterModeCmd.send_commit_to_slave);
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
                return receiveMasterHeartbeat(data);
            }
            if (cmd == ClusterModeCmd.send_prepare_to_slave) {
                //todo
                return StatusReply.OK;
            }
            if (cmd == ClusterModeCmd.send_commit_to_slave) {
                //todo
                return StatusReply.OK;
            }
        } else {
            //master要处理的
            if (!currentNodeMaster()) {
                return new ErrorReply("ERR target not master");
            }
            if (cmd == ClusterModeCmd.send_heartbeat_to_master) {
                return receiveSlaveHeartbeat(source, data);
            }
            if (cmd == ClusterModeCmd.send_prepare_ok_to_master) {
                //todo
                return StatusReply.OK;
            }
            if (cmd == ClusterModeCmd.send_commit_ok_to_master) {
                //todo
                return StatusReply.OK;
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

    private Reply receiveMasterHeartbeat(JSONObject data) {
        String md5 = data.getString("md5");
        if (!slotMap.getMd5().equals(md5)) {
            slotMap = getSlotMapFromMaster();
        }
        JSONObject json = new JSONObject();
        json.put("status", ClusterModeStatus.getStatus().getValue());
        return new BulkReply(Utils.stringToBytes(json.toJSONString()));
    }

    private Reply receiveSlaveHeartbeat(ProxyNode source, JSONObject data) {
        ClusterModeStatus.Status status = ClusterModeStatus.Status.getByValue(data.getIntValue("status"));
        if (slotMap.contains(source) && status == ClusterModeStatus.Status.ONLINE) {
            return StatusReply.OK;
        }
        if (!slotMap.contains(source) && status != ClusterModeStatus.Status.ONLINE) {
            return StatusReply.OK;
        }
        if (slotMap.contains(source) && status != ClusterModeStatus.Status.ONLINE) {
            //todo slot-map refresh
            return StatusReply.OK;
        }
        if (!slotMap.contains(source) && status == ClusterModeStatus.Status.ONLINE) {
            //todo slot-map refresh
            return StatusReply.OK;
        }
        return ErrorReply.SYNTAX_ERROR;
    }

    private void sendHeartbeatToSlave0() {
        try {
            if (!currentNodeMaster()) return;
            List<ProxyNode> onlineNodes = slotMap.getOnlineNodes();
            for (ProxyNode node : onlineNodes) {
                if (node.equals(current())) {
                    continue;
                }
                boolean success = true;
                boolean online = true;
                JSONObject data = new JSONObject();
                data.put("md5", slotMap.getMd5());
                try {
                    Reply reply = sendCmd(node, ClusterModeCmd.send_heartbeat_to_slave, data.toString());
                    if (reply instanceof ErrorReply) {
                        logger.error("send heartbeat to slave error, slave = {}, error = {}", node, ((ErrorReply) reply).getError());
                        success = false;
                    }
                    if (reply instanceof BulkReply) {
                        JSONObject json = JSONObject.parseObject(Utils.bytesToString(((BulkReply) reply).getRaw()));
                        ClusterModeStatus.Status status = ClusterModeStatus.Status.getByValue(json.getInteger("status"));
                        online = status == ClusterModeStatus.Status.ONLINE;
                    }
                } catch (Exception e) {
                    logger.error("send heartbeat to slave error, slave = {}", node, e);
                    success = false;
                }
                if (!success) {
                    //todo slot-map refresh
                }
                if (!online) {
                    //todo slot-map refresh
                }
            }
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

}
