package com.netease.nim.camellia.delayqueue.server.springboot;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.core.util.CamelliaThreadFactory;
import com.netease.nim.camellia.delayqueue.common.conf.CamelliaDelayQueueConstants;
import com.netease.nim.camellia.delayqueue.common.domain.CamelliaDelayMsgPullResponse;
import com.netease.nim.camellia.delayqueue.server.CamelliaDelayQueueServer;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by caojiajun on 2022/9/1
 */
public class CamelliaDelayQueueLongPollingTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaDelayQueueLongPollingTaskExecutor.class);

    private final HashedWheelTimer timer = new HashedWheelTimer();
    private final ThreadPoolExecutor executor;
    private final ThreadPoolExecutor scheduleExecutor;
    private final ThreadPoolExecutor clearExecutor;
    private final int taskQueueSize;
    private final long longPollingTimeoutMillis;

    private final ConcurrentHashMap<String, AtomicBoolean> callbackStatusMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LinkedBlockingQueue<CamelliaDelayQueueLongPollingTask>> map = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicBoolean> lockMap = new ConcurrentHashMap<>();

    private final CamelliaDelayQueueServer server;

    public CamelliaDelayQueueLongPollingTaskExecutor(CamelliaDelayQueueServer server, int taskQueueSize,
                                                     int scheduledThreadSize, int longPollingScheduledQueueSize, int msgReadyCallbackThreadSize,
                                                     int msgReadyCallbackQueueSize, long longPollingTimeoutMillis) {
        this.server = server;
        this.server.addMsgReadyCallback(this::msgReadyCallback);
        this.scheduleExecutor = new ThreadPoolExecutor(scheduledThreadSize, scheduledThreadSize, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(longPollingScheduledQueueSize), new CamelliaThreadFactory("long-polling-schedule-executor"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.clearExecutor = new ThreadPoolExecutor(scheduledThreadSize, scheduledThreadSize, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(longPollingScheduledQueueSize), new CamelliaThreadFactory("long-polling-clear-executor"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.executor = new ThreadPoolExecutor(msgReadyCallbackThreadSize, msgReadyCallbackThreadSize,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(msgReadyCallbackQueueSize), new CamelliaThreadFactory("msg-ready-callback"),
                new ThreadPoolExecutor.AbortPolicy());
        this.taskQueueSize = taskQueueSize;
        this.longPollingTimeoutMillis = longPollingTimeoutMillis;
        ScheduledExecutorService clearTimer = new ScheduledThreadPoolExecutor(1);
        clearTimer.scheduleAtFixedRate(this::clearExpireTask, 1, 1, TimeUnit.SECONDS);
        logger.info("CamelliaDelayQueueLongPollingTaskExecutor start success, taskQueueSize = {}, scheduledThreadSize = {}, longPollingScheduledQueueSize = {}, " +
                "msgReadyCallbackThreadSize = {}, msgReadyCallbackQueueSize = {}, longPollingTimeoutMillis = {}",
                taskQueueSize, scheduledThreadSize, longPollingScheduledQueueSize, msgReadyCallbackQueueSize, msgReadyCallbackQueueSize, longPollingTimeoutMillis);
    }

    public CamelliaDelayQueueLongPollingTaskExecutor(CamelliaDelayQueueServer server) {
        this(server, CamelliaDelayQueueConstants.longPollingTaskQueueSize,
                CamelliaDelayQueueConstants.longPollingScheduledThreadSize, CamelliaDelayQueueConstants.longPollingScheduledQueueSize,
                CamelliaDelayQueueConstants.longPollingMsgReadyCallbackThreadSize, CamelliaDelayQueueConstants.longPollingMsgReadyCallbackQueueSize,
                CamelliaDelayQueueConstants.longPollingTimeoutMillis);
    }

    /**
     * 提交一个长轮询的任务
     */
    public void submit(CamelliaDelayQueueLongPollingTask task) {
        LinkedBlockingQueue<CamelliaDelayQueueLongPollingTask> queue = CamelliaMapUtils.computeIfAbsent(map,
                task.getRequest().getTopic(), topic -> new LinkedBlockingQueue<>(taskQueueSize));
        boolean offer = queue.offer(task);
        if (!offer) {
            notify(task);
            return;
        }
        long longPollingTimeoutMillis = task.getLongPollingTimeoutMillis();
        if (longPollingTimeoutMillis <= 0) {
            longPollingTimeoutMillis = this.longPollingTimeoutMillis;
        }
        Timeout timeout = timer.newTimeout(t -> scheduleExecutor.submit(() -> CamelliaDelayQueueLongPollingTaskExecutor.this.notify(task)), longPollingTimeoutMillis, TimeUnit.MILLISECONDS);
        task.setCancelCallback(timeout::cancel);
    }

    //清理掉已经完成的任务，清理空闲无用的topicq
    private void clearExpireTask() {
        try {
            for (Map.Entry<String, LinkedBlockingQueue<CamelliaDelayQueueLongPollingTask>> entry : map.entrySet()) {
                String topic = entry.getKey();
                LinkedBlockingQueue<CamelliaDelayQueueLongPollingTask> queue = entry.getValue();
                if (queue == null || queue.isEmpty()) {
                    LinkedBlockingQueue<CamelliaDelayQueueLongPollingTask> remove = map.remove(topic);
                    lockMap.remove(topic);
                    if (remove != null && !remove.isEmpty()) {
                        LinkedBlockingQueue<CamelliaDelayQueueLongPollingTask> newQueue = CamelliaMapUtils.computeIfAbsent(map,
                                topic, t -> new LinkedBlockingQueue<>(taskQueueSize));
                        while (!remove.isEmpty()) {
                            CamelliaDelayQueueLongPollingTask task = remove.poll();
                            if (task == null) {
                                break;
                            }
                            if (task.isDone()) {
                                continue;
                            }
                            boolean offer = newQueue.offer(task);
                            if (!offer) {
                                notify(task);
                            }
                        }
                    }
                    continue;
                }
                AtomicBoolean lock = CamelliaMapUtils.computeIfAbsent(lockMap, topic, k -> new AtomicBoolean(false));
                if (lock.compareAndSet(false, true)) {
                    clearExecutor.submit(() -> {
                        try {
                            queue.removeIf(CamelliaDelayQueueLongPollingTask::isDone);
                        } catch (Exception e) {
                            logger.error("clearExpireTask error, topic = {}", topic, e);
                        } finally {
                            lock.compareAndSet(true, false);
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("clearExpireTask error", e);
        }
    }

    //消息就绪的回调，长轮询提前返回
    private void msgReadyCallback(String topic) {
        AtomicBoolean status = CamelliaMapUtils.computeIfAbsent(this.callbackStatusMap, topic, k -> new AtomicBoolean(false));
        if (status.compareAndSet(false, true)) {
            try {
                executor.submit(() -> {
                    try {
                        LinkedBlockingQueue<CamelliaDelayQueueLongPollingTask> queue = map.get(topic);
                        if (queue == null || queue.isEmpty()) return;
                        while (!queue.isEmpty()) {
                            if (server.hasReadyMsg(topic)) {
                                CamelliaDelayQueueLongPollingTask task = queue.poll();
                                if (task == null) {
                                    break;
                                }
                                //长轮询提前返回
                                CamelliaDelayQueueLongPollingTaskExecutor.this.notify(task);
                                //此时，延迟任务可以取消掉了
                                task.cancel();
                            } else {
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("callback error, topic = {}", topic, e);
                    } finally {
                        status.compareAndSet(true, false);
                    }
                });
            } catch (Exception e) {
                logger.error("submit callback error, topic = {}", topic, e);
                status.compareAndSet(true, false);
            }
        }
    }

    private void notify(CamelliaDelayQueueLongPollingTask task) {
        try {
            if (task.tryLock()) {
                CamelliaDelayMsgPullResponse response = server.pullMsg(task.getRequest());
                HttpServletResponse httpServletResponse = (HttpServletResponse) task.getAsyncContext().getResponse();
                httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                httpServletResponse.getWriter().println(JSONObject.toJSONString(response));
                task.getAsyncContext().complete();
            }
        } catch (Exception e) {
            logger.error("camellia delay queue long polling runnable error, topic = {}", task.getRequest().getTopic(), e);
            task.getAsyncContext().complete();
        }
    }
}
