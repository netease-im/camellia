package com.netease.nim.camellia.redis.proxy.plugin.misc;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.plugin.*;
import com.netease.nim.camellia.redis.proxy.plugin.hotkeycache.PrefixMatchHotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClientTemplateFactory;
import com.netease.nim.camellia.redis.proxy.upstream.IUpstreamClient;
import com.netease.nim.camellia.redis.proxy.upstream.RedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.util.BeanInitUtils;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 个用于自定义实现双写策略的插件
 * 可以到key级别，即某些key需要双写，某些key不需要双写，某些key双写到redisA，某些key双写到redisB
 * Created by caojiajun on 2022/9/14
 */
public class MultiWriteProxyPlugin implements ProxyPlugin {

    private ThreadPoolExecutor executor;
    private MultiWriteFunc multiWriteFunc;
    private boolean skipDb;

    @Override
    public void init(ProxyBeanFactory factory) {
        String multiWriteFuncClassName = ProxyDynamicConf.getString("multi.write.func.className", PrefixMatchHotKeyCacheKeyChecker.class.getName());
        this.multiWriteFunc = (MultiWriteFunc) factory.getBean(BeanInitUtils.parseClass(multiWriteFuncClassName));
        this.skipDb = ProxyDynamicConf.getBoolean("multi.write.plugin.skip.db.enable", false);
        int poolSize = ProxyDynamicConf.getInt("multi.write.executor.pool.size", Runtime.getRuntime().availableProcessors());
        int queueSize = ProxyDynamicConf.getInt("multi.write.executor.queue.size", 10000);
        this.executor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, new LinkedBlockingDeque<>(queueSize), new DefaultThreadFactory("multi-write"));
        ProxyDynamicConf.registerCallback(() -> skipDb = ProxyDynamicConf.getBoolean("multi.write.plugin.skip.db.enable", false));
    }

    @Override
    public ProxyPluginOrder order() {
        return new ProxyPluginOrder() {
            @Override
            public int request() {
                return BuildInProxyPluginEnum.MULTI_WRITE_PLUGIN.getRequestOrder();
            }

            @Override
            public int reply() {
                return BuildInProxyPluginEnum.MULTI_WRITE_PLUGIN.getReplyOrder();
            }
        };
    }

    @Override
    public ProxyPluginResponse executeRequest(ProxyRequest request) {
        try {
            int db = request.getDb();
            if (skipDb) {
                db = -1;
            }
            Command command = request.getCommand();
            IUpstreamClientTemplateFactory factory = request.getClientTemplateFactory();
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
            RedisCommand.CommandKeyType commandKeyType = redisCommand.getCommandKeyType();
            Long bid = command.getChannelInfo().getBid();
            String bgroup = command.getChannelInfo().getBgroup();
            KeyContext keyContext = new KeyContext(redisCommand, bid, bgroup);
            if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_SINGLE) {
                List<byte[]> keys = command.getKeys();
                if (!keys.isEmpty()) {
                    byte[] key = keys.get(0);
                    doMultiWrite(key, keyContext, db, command, factory);
                }
            } else if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_MULTI) {
                List<byte[]> keys = command.getKeys();
                if (keys != null && !keys.isEmpty()) {
                    for (byte[] key : keys) {
                        doMultiWrite(key, keyContext, db, new Command(new byte[][]{redisCommand.raw(), key}), factory);
                    }
                }
            } else if (commandKeyType == RedisCommand.CommandKeyType.COMPLEX) {
                if (redisCommand == RedisCommand.MSET) {
                    byte[][] objects = command.getObjects();
                    if (objects.length >= 3 && objects.length % 2 == 1) {
                        for (int i = 1; i < objects.length; i += 2) {
                            byte[] key = objects[i];
                            byte[] value = objects[i + 1];
                            doMultiWrite(key, keyContext, db, new Command(new byte[][]{RedisCommand.SET.raw(), key, value}), factory);
                        }
                    }
                } else if (redisCommand == RedisCommand.XGROUP) {
                    if (command.getObjects().length >= 3) {
                        byte[] key = command.getObjects()[2];
                        doMultiWrite(key, keyContext, db, command, factory);
                    }
                }
            }
            return ProxyPluginResponse.SUCCESS;
        } catch (Exception e) {
            ErrorLogCollector.collect(MultiWriteProxyPlugin.class, "multi write error", e);
            return ProxyPluginResponse.SUCCESS;
        }
    }

    private void doMultiWrite(byte[] key, KeyContext keyContext, int db, Command command, IUpstreamClientTemplateFactory factory) {
        try {
            RedisProxyEnv redisProxyEnv = factory.getEnv();
            if (redisProxyEnv == null) {
                ErrorLogCollector.collect(MultiWriteProxyPlugin.class, "multi write fail for redisProxyEnv is null");
                return;
            }
            MultiWriteInfo multiWriteInfo = multiWriteFunc.multiWriteInfo(new KeyInfo(key, keyContext));
            if (multiWriteInfo != null && multiWriteInfo.isMultiWriteEnable()) {
                List<String> urls = multiWriteInfo.getUrls();
                if (urls != null && !urls.isEmpty()) {
                    for (String url : urls) {
                        try {
                            executor.submit(() -> {
                                try {
                                    IUpstreamClient client = redisProxyEnv.getClientFactory().get(url);
                                    client.sendCommand(db, Collections.singletonList(command),
                                            Collections.singletonList(new CompletableFuture<>()));
                                } catch (Exception e) {
                                    ErrorLogCollector.collect(MultiWriteProxyPlugin.class, "multi write error, url = " + url, e);
                                }
                            });
                        } catch (Exception e) {
                            ErrorLogCollector.collect(MultiWriteProxyPlugin.class, "submit multi write error, url = " + url, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ErrorLogCollector.collect(MultiWriteProxyPlugin.class,
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
