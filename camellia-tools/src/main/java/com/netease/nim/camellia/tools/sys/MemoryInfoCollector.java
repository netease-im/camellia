package com.netease.nim.camellia.tools.sys;

import com.netease.nim.camellia.tools.utils.ReflectUtils;
import io.netty.util.internal.PlatformDependent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2023/11/29
 */
public class MemoryInfoCollector {

    private static final Logger logger = LoggerFactory.getLogger(MemoryInfoCollector.class);

    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private static AtomicLong nettyDirectMemory;
    static {
        try {
            Field filed = ReflectUtils.findField(PlatformDependent.class, "DIRECT_MEMORY_COUNTER");
            filed.setAccessible(true);
            nettyDirectMemory = (AtomicLong) filed.get(PlatformDependent.class);
            if (nettyDirectMemory == null) {
                nettyDirectMemory = new AtomicLong();
            }
        } catch (Throwable e) {
            logger.error("get netty direct memory error", e);
            nettyDirectMemory = new AtomicLong(0);
        }
    }

    public static MemoryInfo getMemoryInfo() {
        MemoryInfo memoryInfo = new MemoryInfo();
        memoryInfo.setFreeMemory(Runtime.getRuntime().freeMemory());
        memoryInfo.setTotalMemory(Runtime.getRuntime().totalMemory());
        memoryInfo.setMaxMemory(Runtime.getRuntime().maxMemory());
        memoryInfo.setHeapMemoryInit(memoryMXBean.getHeapMemoryUsage().getInit());
        memoryInfo.setHeapMemoryUsed(memoryMXBean.getHeapMemoryUsage().getUsed());
        memoryInfo.setHeapMemoryMax(memoryMXBean.getHeapMemoryUsage().getMax());
        memoryInfo.setHeapMemoryCommitted(memoryMXBean.getHeapMemoryUsage().getCommitted());
        memoryInfo.setNonHeapMemoryInit(memoryMXBean.getNonHeapMemoryUsage().getInit());
        memoryInfo.setNonHeapMemoryUsed(memoryMXBean.getNonHeapMemoryUsage().getUsed());
        memoryInfo.setNonHeapMemoryMax(memoryMXBean.getNonHeapMemoryUsage().getMax());
        memoryInfo.setNonHeapMemoryCommitted(memoryMXBean.getNonHeapMemoryUsage().getCommitted());
        memoryInfo.setNettyDirectMemory(nettyDirectMemory.get());
        return memoryInfo;
    }
}
