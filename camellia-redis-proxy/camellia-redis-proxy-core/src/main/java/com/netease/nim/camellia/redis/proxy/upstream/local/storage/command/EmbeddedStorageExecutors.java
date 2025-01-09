package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;


import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.MpscSlotHashExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2025/1/3
 */
public class EmbeddedStorageExecutors {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedStorageExecutors.class);

    private static volatile EmbeddedStorageExecutors INSTANCE;

    private final MpscSlotHashExecutor commandExecutor;

    private EmbeddedStorageExecutors() {
        int threads = ProxyDynamicConf.getInt("embedded.storage.command.executor.threads", SysUtils.getCpuNum() * 4);
        int queueSize = ProxyDynamicConf.getInt("embedded.storage.command.executor.queue.size", 1024*128);
        commandExecutor = new MpscSlotHashExecutor("embedded-storage-command-executor", threads, queueSize, new MpscSlotHashExecutor.AbortPolicy());
        logger.info("EmbeddedStorageExecutors init success, threads = {}, queueSize = {}", threads, queueSize);
    }

    public static EmbeddedStorageExecutors getInstance() {
        if (INSTANCE == null) {
            synchronized (EmbeddedStorageExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EmbeddedStorageExecutors();
                }
            }
        }
        return INSTANCE;
    }

    public MpscSlotHashExecutor getCommandExecutor() {
        return commandExecutor;
    }
}
