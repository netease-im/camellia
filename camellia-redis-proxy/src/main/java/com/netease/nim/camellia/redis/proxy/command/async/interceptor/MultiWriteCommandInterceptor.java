package com.netease.nim.camellia.redis.proxy.command.async.interceptor;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.*;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 一个用于自定义实现双写策略的拦截器
 * 可以到key级别，即某些key需要双写，某些key不需要双写，某些key双写到redisA，某些key双写到redisB
 * Created by caojiajun on 2021/7/22
 */
public class MultiWriteCommandInterceptor implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MultiWriteCommandInterceptor.class);
    private final MultiWriteFunc multiWriteFunc;

    public MultiWriteCommandInterceptor() {
        String className = ProxyDynamicConf.getString("multi.write.func.class.name", null);
        if (className == null) {
            throw new CamelliaRedisException("multi.write.func.class.name not found from ProxyDynamicConf");
        }
        try {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (ClassNotFoundException e) {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
            multiWriteFunc = (MultiWriteFunc) clazz.newInstance();
            logger.info("MultiWriteFunc init success, class = {}", className);
        } catch (Exception e) {
            logger.error("MultiWriteFunc init error, class = {}", className, e);
            throw new CamelliaRedisException(e);
        }
    }

    @Override
    public CommandInterceptResponse check(Command command) {
        try {
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
            RedisCommand.CommandKeyType commandKeyType = redisCommand.getCommandKeyType();
            Long bid = command.getChannelInfo().getBid();
            String bgroup = command.getChannelInfo().getBgroup();
            KeyContext keyContext = new KeyContext(redisCommand, bid, bgroup);
            if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_SINGLE) {
                List<byte[]> keys = command.getKeys();
                if (!keys.isEmpty()) {
                    byte[] key = keys.get(0);
                    doMultiWrite(key, keyContext, command);
                }
            } else if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_MULTI) {
                List<byte[]> keys = command.getKeys();
                if (keys != null && !keys.isEmpty()) {
                    for (byte[] key : keys) {
                        doMultiWrite(key, keyContext, new Command(new byte[][]{redisCommand.raw(), key}));
                    }
                }
            } else if (commandKeyType == RedisCommand.CommandKeyType.COMPLEX) {
                if (redisCommand == RedisCommand.MSET) {
                    byte[][] objects = command.getObjects();
                    if (objects.length >= 3 && objects.length % 2 == 1) {
                        for (int i = 1; i < objects.length; i += 2) {
                            byte[] key = objects[i];
                            byte[] value = objects[i + 1];
                            doMultiWrite(key, keyContext, new Command(new byte[][]{RedisCommand.SET.raw(), key, value}));
                        }
                    }
                } else if (redisCommand == RedisCommand.XGROUP) {
                    if (command.getObjects().length >= 3) {
                        byte[] key = command.getObjects()[2];
                        doMultiWrite(key, keyContext, command);
                    }
                }
            }
            return CommandInterceptResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(MultiWriteCommandInterceptor.class, "check error", e);
            return CommandInterceptResponse.SUCCESS;
        }
    }

    private void doMultiWrite(byte[] key, KeyContext keyContext, Command command) {
        try {
            MultiWriteInfo multiWriteInfo = multiWriteFunc.multiWriteInfo(new KeyInfo(key, keyContext));
            if (multiWriteInfo != null && multiWriteInfo.isMultiWriteEnable()) {
                List<String> urls = multiWriteInfo.getUrls();
                if (urls != null && !urls.isEmpty()) {
                    for (String url : urls) {
                        try {
                            AsyncClient client = AsyncNettyClientFactory.DEFAULT.get(url);
                            client.sendCommand(Collections.singletonList(command),
                                    Collections.singletonList(new CompletableFuture<>()));
                        } catch (Exception e) {
                            ErrorLogCollector.collect(MultiWriteCommandInterceptor.class,
                                    "multi write error, url = " + url, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(MultiWriteCommandInterceptor.class,
                    "multi write error, key = " + Utils.bytesToString(key), e);
        }
    }

    public static interface MultiWriteFunc {
        /**
         * 用于判断一个key的双写策略
         * @param keyInfo key的上下文
         * @return 是否需要双写以及双写到哪里
         */
        MultiWriteInfo multiWriteInfo(KeyInfo keyInfo);
    }

    public final static class KeyContext {
        private final RedisCommand redisCommand;
        private final Long bid;
        private final String bgroup;

        public KeyContext(RedisCommand redisCommand, Long bid, String bgroup) {
            this.redisCommand = redisCommand;
            this.bid = bid;
            this.bgroup = bgroup;
        }

        public RedisCommand getRedisCommand() {
            return redisCommand;
        }

        public Long getBid() {
            return bid;
        }

        public String getBgroup() {
            return bgroup;
        }
    }

    public final static class KeyInfo {
        private final byte[] key;
        private final KeyContext keyContext;

        public KeyInfo(byte[] key, KeyContext keyContext) {
            this.key = key;
            this.keyContext = keyContext;
        }

        public byte[] getKey() {
            return key;
        }

        public KeyContext getKeyContext() {
            return keyContext;
        }
    }

    public final static class MultiWriteInfo {
        public static final MultiWriteInfo SKIP_MULTI_WRITE = new MultiWriteInfo(false, (List<String>) null);

        private boolean multiWriteEnable;
        private List<String> urls;

        public MultiWriteInfo() {
        }

        public MultiWriteInfo(boolean multiWriteEnable, String url) {
            this.multiWriteEnable = multiWriteEnable;
            if (url != null) {
                this.urls = Collections.singletonList(url);
            }
        }

        public MultiWriteInfo(boolean multiWriteEnable, List<String> urls) {
            this.multiWriteEnable = multiWriteEnable;
            this.urls = urls;
        }

        public boolean isMultiWriteEnable() {
            return multiWriteEnable;
        }

        public void setMultiWriteEnable(boolean multiWriteEnable) {
            this.multiWriteEnable = multiWriteEnable;
        }

        public List<String> getUrls() {
            return urls;
        }

        public void setUrls(List<String> urls) {
            this.urls = urls;
        }
    }
}
