package com.netease.nim.camellia.tools.sys;

import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2023/12/21
 */
public class CpuUsageCollector {

    private static final Logger logger = LoggerFactory.getLogger(CpuUsageCollector.class);

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory("cpu-ratio-calculate"));

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer() ;
    private ObjectName operatingSystemBean = null;

    private int processorCount = 1;
    private Long lastProcessCpuTime;
    private Long lastSystemTime;
    private double ratio;

    public static CpuUsageCollector INSTANCE = new CpuUsageCollector();

    public CpuUsageCollector(int intervalSeconds) {
        init(intervalSeconds);
    }

    public CpuUsageCollector() {
        init(60);
    }

    private void init(int intervalSeconds) {
        try {
            processorCount = osBean.getAvailableProcessors();
            ObjectName objName = new ObjectName("java.lang:type=OperatingSystem");
            Set<ObjectName> nn = server.queryNames(objName, null);
            if (nn != null && !nn.isEmpty()) {
                for (ObjectName on : nn) {
                    operatingSystemBean = on;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        calc();
        scheduler.scheduleAtFixedRate(this::calc, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public CpuUsage getCpuUsageInfo() {
        CpuUsage info = new CpuUsage();
        info.setRatio(ratio);
        info.setCpuNum(processorCount);
        return info;
    }

    private void calc() {
        if (operatingSystemBean == null) {
            return;
        }
        long currentProcessTime;
        try {
            Object o = server.getAttribute(operatingSystemBean, "ProcessCpuTime");
            if (o != null) {
                try {
                    currentProcessTime = Long.parseLong(o.toString());
                } catch (Exception e) {
                    return;
                }
            } else {
                return;
            }
        } catch (Exception e) {
            return;
        }
        if (lastProcessCpuTime == null) {
            lastProcessCpuTime = currentProcessTime;
            lastSystemTime = System.nanoTime();
            return;
        }

        long currentSystemTime = System.nanoTime();
        long cpuTimeInterval = currentProcessTime - lastProcessCpuTime;
        long sysTimeInterval = currentSystemTime - lastSystemTime;

        lastProcessCpuTime = currentProcessTime;
        lastSystemTime = currentSystemTime;

        this.ratio = ((double) cpuTimeInterval / sysTimeInterval) * 100;
    }
}
