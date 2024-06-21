package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.cluster.*;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyClusterSlotMap;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnection;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionHub;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/6/18
 */
public class ConsensusProxyClusterModeProvider extends AbstractProxyClusterModeProvider {

    private static final Logger logger = LoggerFactory.getLogger(ConsensusProxyClusterModeProvider.class);

    private ConsensusMasterSelector masterSelector;
    private volatile ProxyNode master;

    @Override
    public void init() {
        while (true) {
            master = masterSelector.getMaster();
            if (master != null) {
                break;
            }
            logger.warn("master is null, waiting...");
            try {
                TimeUnit.MILLISECONDS.sleep(10);
            } catch (InterruptedException e) {
                throw new CamelliaRedisException(e);
            }
        }
        schedule.scheduleAtFixedRate(this::sendHeartbeatToMaster,
                0, 10, TimeUnit.SECONDS);
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
            if (current().equals(master)) {
                return new ErrorReply("ERR target not slave");
            }
            if (cmd == ClusterModeCmd.send_heartbeat_to_slave) {
                return StatusReply.OK;
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
            if (!current().equals(master)) {
                return new ErrorReply("ERR target not master");
            }
            if (cmd == ClusterModeCmd.send_heartbeat_to_master) {
                ClusterModeStatus.Status status = ClusterModeStatus.Status.getByValue(data.getIntValue("status"));
                ProxyClusterSlotMap slotMap = masterSelector.getSlotMap();
                if (slotMap.contains(source) && status == ClusterModeStatus.Status.ONLINE) {
                    return StatusReply.OK;
                }
                if (!slotMap.contains(source) && status != ClusterModeStatus.Status.ONLINE) {
                    return StatusReply.OK;
                }
                if (slotMap.contains(source) && status != ClusterModeStatus.Status.ONLINE) {
                    //todo prepare offline
                    return StatusReply.OK;
                }
                if (!slotMap.contains(source) && status == ClusterModeStatus.Status.ONLINE) {
                    //todo prepare online
                    return StatusReply.OK;
                }
            }
            if (cmd == ClusterModeCmd.send_prepare_ok_to_master) {
                //todo
                return StatusReply.OK;
            }
            if (cmd == ClusterModeCmd.send_commit_ok_to_master) {
                //todo
                return StatusReply.OK;
            }
        }
        logger.error("unknown cmd = {}", cmd);
        return ErrorReply.SYNTAX_ERROR;
    }

    @Override
    public ProxyClusterSlotMap load() {
        return null;
    }

    private void sendHeartbeatToMaster() {
        if (!master.equals(current())) {
            //send to master
            JSONObject data = new JSONObject();
            data.put("status", ClusterModeStatus.getStatus().getValue());
            try {
                Reply reply = sendCmd(master, ClusterModeCmd.send_heartbeat_to_master, data.toString());
                if (reply instanceof ErrorReply) {
                    logger.error("send heartbeat to master error, error = {}", ((ErrorReply) reply).getError());
                }
            } catch (Exception e) {
                logger.error("send heartbeat to master error", e);
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

}
