package com.netease.nim.camellia.redis.proxy.mq.common;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptResponse;
import com.netease.nim.camellia.redis.proxy.command.async.interceptor.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MqMultiWriteCommandInterceptor implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MqMultiWriteCommandInterceptor.class);

    private static final String CONF_ENABLE = "mq.send.multi.write.enable";
    private static final String CONF_INTERRUPT = "mq.send.fail.interrupt.command";
    private static final CommandInterceptResponse MQ_FAIL = new CommandInterceptResponse(false, "mq send fail");

    private final MqPackSender mqSender;

    public MqMultiWriteCommandInterceptor() {
        String className = ProxyDynamicConf.getString("mq.multi.write.sender.class.name", null);
        if (className == null) {
            throw new CamelliaRedisException("mq.multi.write.sender.class.name not found from ProxyDynamicConf");
        }
        try {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
            mqSender = (MqPackSender) clazz.newInstance();
            logger.info("MqPackSender init success, class = {}", className);
        } catch (Exception e) {
            logger.error("MqPackSender init error, class = {}", className, e);
            throw new CamelliaRedisException(e);
        }
    }

    public MqMultiWriteCommandInterceptor(MqPackSender mqSender) {
        this.mqSender = mqSender;
        logger.info("MqPackSender init success, class = {}", mqSender.getClass().getName());
    }

    @Override
    public CommandInterceptResponse check(Command command) {
        try {
            Long bid = command.getCommandContext().getBid();
            String bgroup = command.getCommandContext().getBgroup();
            if (!ProxyDynamicConf.getBoolean(CONF_ENABLE, bid, bgroup, true)) {
                return CommandInterceptResponse.SUCCESS;
            }
            RedisCommand redisCommand = command.getRedisCommand();
            if (redisCommand == null) {
                return CommandInterceptResponse.SUCCESS;
            }
            //限制性命令不支持
            if (redisCommand.getSupportType() != RedisCommand.CommandSupportType.FULL_SUPPORT) {
                return CommandInterceptResponse.SUCCESS;
            }
            //只处理写命令
            RedisCommand.Type type = redisCommand.getType();
            if (type != RedisCommand.Type.WRITE) {
                return CommandInterceptResponse.SUCCESS;
            }
            MqPack mqPack = new MqPack();
            mqPack.setCommand(command);
            mqPack.setBid(bid);
            mqPack.setBgroup(bgroup);
            boolean send = mqSender.send(mqPack);
            if (!send && ProxyDynamicConf.getBoolean(CONF_INTERRUPT, false)) {
                return MQ_FAIL;
            }
            return CommandInterceptResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(MqMultiWriteCommandInterceptor.class, "mq multi write error", e);
            if (ProxyDynamicConf.getBoolean(CONF_INTERRUPT, false)) {
                return MQ_FAIL;
            }
            return CommandInterceptResponse.SUCCESS;
        }
    }
}
