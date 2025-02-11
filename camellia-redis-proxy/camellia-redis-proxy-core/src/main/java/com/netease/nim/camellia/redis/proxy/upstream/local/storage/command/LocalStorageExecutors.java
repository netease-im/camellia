package com.netease.nim.camellia.redis.proxy.upstream.local.storage.command;


import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.flush.FlushExecutor;
import com.netease.nim.camellia.redis.proxy.util.MpscSlotHashExecutor;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by caojiajun on 2025/1/3
 */
public class LocalStorageExecutors {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageExecutors.class);

    private static final int defaultThreads = Math.min(4, SysUtils.getCpuHalfNum());

    private static volatile LocalStorageExecutors INSTANCE;

    private final MpscSlotHashExecutor commandExecutor;
    private final FlushExecutor flushExecutor;
    private final ScheduledExecutorService flushScheduler;
    private final ScheduledExecutorService scheduler;
    private final int walThreads;
    private final boolean walAsync;
    private final int stringValueFileCount;

    private LocalStorageExecutors() {
        {
            walAsync = ProxyDynamicConf.getBoolean("local.storage.wal.async.enable", true);
            walThreads = ProxyDynamicConf.getInt("local.storage.wal.write.threads", defaultThreads);
        }
        {
            int threads = ProxyDynamicConf.getInt("local.storage.command.executor.threads", defaultThreads);
            if (threads <= 0) {
                threads = 4;
            }
            int queueSize = ProxyDynamicConf.getInt("local.storage.command.executor.queue.size", 1024 * 128);
            commandExecutor = new MpscSlotHashExecutor("local-storage-command-executor", threads, queueSize, new MpscSlotHashExecutor.AbortPolicy());
            logger.info("local storage command executor init success, threads = {}, queueSize = {}", threads, queueSize);
        }
        {
            int threads = ProxyDynamicConf.getInt("local.storage.flush.executor.threads", defaultThreads);
            if (threads <= 0) {
                threads = 1;
            }
            stringValueFileCount = threads;
            int queueSize = ProxyDynamicConf.getInt("local.storage.flush.executor.queue.size", 1024 * 128);
            boolean asyncEnable = ProxyDynamicConf.getBoolean("local.storage.flush.executor.async.enable", true);
            flushExecutor = new FlushExecutor(threads, queueSize, asyncEnable);
            logger.info("local storage flush executor init success, threads = {}, queueSize = {}, asyncEnable = {}", threads, queueSize, asyncEnable);
        }
        {
            flushScheduler = Executors.newScheduledThreadPool(1, new CamelliaThreadFactory("local-storage-flush-schedule"));
            logger.info("local storage flush scheduler init success");
        }
        {
            scheduler = Executors.newScheduledThreadPool(1, new CamelliaThreadFactory("local-storage-schedule"));
            logger.info("local storage scheduler init success");
        }
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

    public int getWalThreads() {
        return walThreads;
    }

    public boolean isWalAsync() {
        return walAsync;
    }

    public MpscSlotHashExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public FlushExecutor getFlushExecutor() {
        return flushExecutor;
    }

    public int getStringValueFileCount() {
        return stringValueFileCount;
    }

    public ScheduledExecutorService getFlushScheduler() {
        return flushScheduler;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
