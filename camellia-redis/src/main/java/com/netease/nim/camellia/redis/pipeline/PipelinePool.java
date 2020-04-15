package com.netease.nim.camellia.redis.pipeline;

import com.netease.nim.camellia.redis.CamelliaRedisEnv;

import java.util.concurrent.*;

/**
 * PipelinePool，从而可以复用pipeline，减少重新生成pipeline代理对象的开销
 * Created by caojiajun on 2019/8/8.
 */
public class PipelinePool {

    private LinkedBlockingQueue<Item> queue;

    public PipelinePool(CamelliaRedisEnv env) {
        this.queue = new LinkedBlockingQueue<>(env.getPipelinePoolSize());
    }

    /**
     * 获取一个空闲的pipeline实例
     * @return pipeline实例
     */
    public CamelliaRedisPipeline get() {
        Item item = queue.poll();
        if (item == null) return null;
        if (item.openCallback != null) {
            //如果设置了open回调，则先执行一下回调
            item.openCallback.run();
        }
        return item.pipeline;
    }

    /**
     * 把一个pipeline实例放到pool中
     * @param pipeline pipeline实例
     * @param openCallback pipeline实例打开的回调
     * @return 成功/失败
     */
    public boolean set(CamelliaRedisPipeline pipeline, Runnable openCallback) {
        return pipeline != null && queue.offer(new Item(pipeline, openCallback));
    }

    private static class Item {
        CamelliaRedisPipeline pipeline;
        Runnable openCallback;

        Item(CamelliaRedisPipeline pipeline, Runnable openCallback) {
            this.pipeline = pipeline;
            this.openCallback = openCallback;
        }
    }
}
