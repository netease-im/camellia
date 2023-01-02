package com.netease.nim.camellia.redis.proxy.hbase;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.redis.proxy.hbase.conf.RedisHBaseConfiguration;
import com.netease.nim.camellia.redis.proxy.hbase.monitor.RedisHBaseMonitor;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.netease.nim.camellia.redis.proxy.hbase.util.RedisHBaseUtils.hbaseTableName;

/**
 *
 * Created by caojiajun on 2021/7/6
 */
public class HBaseAsyncWriteExecutor {

    private static final Logger logger = LoggerFactory.getLogger(HBaseAsyncWriteExecutor.class);

    private final List<HBaseAsyncWriteExecutor.HBaseAsyncWriteThread> threadList = new ArrayList<>();

    public HBaseAsyncWriteExecutor(CamelliaHBaseTemplate hBaseTemplate, int poolSize, int queueSize) {
        for (int i=0; i<poolSize; i++) {
            HBaseAsyncWriteExecutor.HBaseAsyncWriteThread thread = new HBaseAsyncWriteExecutor.HBaseAsyncWriteThread(hBaseTemplate, queueSize);
            thread.start();
            threadList.add(thread);
        }
    }

    public boolean submit(HBaseAsyncWriteTask task) {
        byte[] key = task.getKey();
        int index = Math.abs(Arrays.hashCode(key)) % threadList.size();
        return threadList.get(index).submit(task);
    }

    public static class HBaseAsyncWriteTask {
        private byte[] key;
        private List<Put> puts;
        private List<Delete> deletes;

        public byte[] getKey() {
            return key;
        }

        public void setKey(byte[] key) {
            this.key = key;
        }

        public List<Put> getPuts() {
            return puts;
        }

        public void setPuts(List<Put> puts) {
            this.puts = puts;
        }

        public List<Delete> getDeletes() {
            return deletes;
        }

        public void setDeletes(List<Delete> deletes) {
            this.deletes = deletes;
        }
    }

    private static class HBaseAsyncWriteThread extends Thread {
        private static final AtomicLong id = new AtomicLong();
        private final LinkedBlockingQueue<HBaseAsyncWriteTask> queue;
        private final CamelliaHBaseTemplate hBaseTemplate;

        public HBaseAsyncWriteThread(CamelliaHBaseTemplate hBaseTemplate, int queueSize) {
            this.hBaseTemplate = hBaseTemplate;
            this.queue = new LinkedBlockingQueue<>(queueSize);
            setName("hbase-async-write-" + id.incrementAndGet());
            RedisHBaseMonitor.register(getName(), queue);
        }

        public boolean submit(HBaseAsyncWriteTask task) {
            return queue.offer(task);
        }

        @Override
        public void run() {
            List<Put> putBuffer = new ArrayList<>();
            List<Delete> deleteBuffer = new ArrayList<>();
            while (true) {
                try {
                    HBaseAsyncWriteTask task = queue.poll(1, TimeUnit.SECONDS);
                    if (task == null) {
                        if (!putBuffer.isEmpty()) {
                            flushPuts(putBuffer);
                        }
                        if (!deleteBuffer.isEmpty()) {
                            flushDelete(deleteBuffer);
                        }
                        continue;
                    }
                    List<Put> puts = task.getPuts();
                    if (puts != null) {
                        if (!deleteBuffer.isEmpty()) {
                            flushDelete(deleteBuffer);
                        }
                        putBuffer.addAll(puts);
                        if (putBuffer.size() >= RedisHBaseConfiguration.hbaseMaxBatch()) {
                            flushPuts(putBuffer);
                        }
                    }
                    List<Delete> deletes = task.getDeletes();
                    if (deletes != null) {
                        if (!putBuffer.isEmpty()) {
                            flushPuts(putBuffer);
                        }
                        deleteBuffer.addAll(deletes);
                        if (deleteBuffer.size() >= RedisHBaseConfiguration.hbaseMaxBatch()) {
                            flushDelete(deleteBuffer);
                        }
                    }
                } catch (Exception e) {
                    logger.error("hbase async write error", e);
                }
            }
        }

        private void flushPuts(List<Put> putBuffer) {
            int size = putBuffer.size();
            if (size <= 0) return;
            hBaseTemplate.put(hbaseTableName(), putBuffer);
            putBuffer.clear();
            if (logger.isDebugEnabled()) {
                logger.debug("flush hbase of put, size = {}", size);
            }
        }

        private void flushDelete(List<Delete> deleteBuffer) {
            int size = deleteBuffer.size();
            if (size <= 0) return;
            hBaseTemplate.delete(hbaseTableName(), deleteBuffer);
            deleteBuffer.clear();
            if (logger.isDebugEnabled()) {
                logger.debug("flush hbase of delete, size = {}", size);
            }
        }
    }
}
