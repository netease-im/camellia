package com.netease.nim.camellia.redis.proxy.mq.common;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyBeanFactory;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPlugin;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyPluginResponse;
import com.netease.nim.camellia.redis.proxy.plugin.ProxyRequest;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqMultiWriteProducerProxyPlugin implements ProxyPlugin {

    private static final Logger logger = LoggerFactory.getLogger(MqMultiWriteProducerProxyPlugin.class);

    private static final String CONF_ENABLE = "mq.send.multi.write.enable";
    private static final String CONF_INTERRUPT = "mq.send.fail.interrupt.command";
    private static final ProxyPluginResponse MQ_FAIL = new ProxyPluginResponse(false, "mq send fail");

    private MqPackSender mqSender;
    private boolean skipDb;

    @Override
    public void init(ProxyBeanFactory factory) {
        String className = BeanInitUtils.getClassName("mq.multi.write.sender", null);
        this.mqSender = (MqPackSender) factory.getBean(BeanInitUtils.parseClass(className));
        logger.info("mqSender init success, className = {}", className);

        this.skipDb = ProxyDynamicConf.getBoolean("mq.multi.write.plugin.skip.db.enable", false);
        ProxyDynamicConf.registerCallback(() -> skipDb = ProxyDynamicConf.getBoolean("mq.multi.write.plugin.skip.db.enable", false));
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            Command command = request.getCommand();
            Long bid = command.getCommandContext().getBid();
            String bgroup = command.getCommandContext().getBgroup();
            if (!ProxyDynamicConf.getBoolean(CONF_ENABLE, bid, bgroup, true)) {
                return ProxyPluginResponse.SUCCESS;
            }
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == null) {
                return ProxyPluginResponse.SUCCESS;
            }
            //限制性命令不支持
            if (redisCommand.getSupportType() != RedisCommand.CommandSupportType.FULL_SUPPORT) {
                return ProxyPluginResponse.SUCCESS;
            }
            //只处理写命令
            RedisCommand.Type type = redisCommand.getType();
            if (type != RedisCommand.Type.WRITE) {
                return ProxyPluginResponse.SUCCESS;
            }
            //阻塞性命令不支持
            if (command.isBlocking()) {
                return ProxyPluginResponse.SUCCESS;
            }
            MqPack mqPack = new MqPack();
            mqPack.setCommand(command);
            mqPack.setBid(bid);
            mqPack.setBgroup(bgroup);
            if (!skipDb) {
                mqPack.setDb(request.getDb());
            }
            boolean send = mqSender.send(mqPack);
            if (!send && ProxyDynamicConf.getBoolean(CONF_INTERRUPT, false)) {
                return MQ_FAIL;
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(MqMultiWriteProducerProxyPlugin.class, "mq multi write error", e);
            if (ProxyDynamicConf.getBoolean(CONF_INTERRUPT, false)) {
                return MQ_FAIL;
            }
            return ProxyPluginResponse.SUCCESS;
        }
    }
}
