package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;


import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.MpscSlotHashExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2025/1/3
 */
public class LocalStorageExecutors {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageExecutors.class);

    private static volatile LocalStorageExecutors INSTANCE;

    private final MpscSlotHashExecutor commandExecutor;

    private LocalStorageExecutors() {
        int threads = ProxyDynamicConf.getInt("local.storage.command.executor.threads", SysUtils.getCpuNum() * 4);
        int queueSize = ProxyDynamicConf.getInt("local.storage.command.executor.queue.size", 1024*128);
        commandExecutor = new MpscSlotHashExecutor("local-storage-command-executor", threads, queueSize, new MpscSlotHashExecutor.AbortPolicy());
        logger.info("LocalStorageExecutors init success, threads = {}, queueSize = {}", threads, queueSize);
    }

    public static LocalStorageExecutors getInstance() {
        if (INSTANCE == null) {
            synchronized (LocalStorageExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LocalStorageExecutors();
                }
            }
        }
        return INSTANCE;
    }

    public MpscSlotHashExecutor getCommandExecutor() {
        return commandExecutor;
    }
}
