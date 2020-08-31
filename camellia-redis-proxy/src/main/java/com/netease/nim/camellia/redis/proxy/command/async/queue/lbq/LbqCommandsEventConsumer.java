package com.netease.nim.camellia.redis.proxy.command.async.queue.lbq;

import com.netease.nim.camellia.redis.proxy.command.async.AsyncCamelliaRedisTemplateChooser;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.queue.AbstractCommandsEventConsumer;
import com.netease.nim.camellia.redis.proxy.command.async.queue.CommandsEvent;
import io.netty.util.concurrent.FastThreadLocalThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/8/27
 */
public class LbqCommandsEventConsumer extends AbstractCommandsEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(LbqCommandsEventConsumer.class);
    private static final AtomicLong id = new AtomicLong(0);

    private final LinkedBlockingQueue<CommandsEvent> queue;
    private final int commandPipelineFlushThreshold;
    private boolean start = true;


    public LbqCommandsEventConsumer(LinkedBlockingQueue<CommandsEvent> queue, AsyncCamelliaRedisTemplateChooser chooser, CommandInterceptor commandInterceptor, int commandPipelineFlushThreshold, boolean commandSpendTimeMonitorEnable, long slowCommandThresholdMillisTime) {
        super(chooser, commandInterceptor, commandPipelineFlushThreshold, commandSpendTimeMonitorEnable, slowCommandThresholdMillisTime);
        this.commandPipelineFlushThreshold = commandPipelineFlushThreshold;
        this.queue = queue;
    }

    public void start() {
        new FastThreadLocalThread(() -> {
            List<CommandsEvent> buffer = new ArrayList<>();
            while (start) {
                try {
                    if (buffer.isEmpty()) {
                        CommandsEvent event = queue.poll(10, TimeUnit.SECONDS);
                        if (event != null) {
                            buffer.add(event);
                        }
                    } else {
                        queue.drainTo(buffer, commandPipelineFlushThreshold);
                        for (int i=0; i<buffer.size(); i++) {
                            onEvent(buffer.get(i), i == buffer.size() - 1);
                        }
                        buffer.clear();
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }, "lbq-commands-event-" + id.incrementAndGet()).start();
    }

    public void stop() {
        start = false;
    }
}
