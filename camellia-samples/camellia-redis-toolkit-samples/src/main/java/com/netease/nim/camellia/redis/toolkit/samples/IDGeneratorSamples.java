package com.netease.nim.camellia.redis.toolkit.samples;

import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.toolkit.id.CamelliaIDGenerator;
import com.netease.nim.camellia.redis.toolkit.id.IDLoader;
import com.netease.nim.camellia.redis.toolkit.id.IDRange;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Created by caojiajun on 2020/4/14.
 */
public class IDGeneratorSamples {

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://abc@127.0.0.1:6379");

        CamelliaIDGenerator<Long> idGenerator = new CamelliaIDGenerator.Builder<Long>()
                .idLoader(new DbIDLoader())//从db里获取一段id
                .redisTemplate(template)
                .tagToCacheKey(tag -> "id_" + tag)//tag转成缓存key
                .defaultStep(() -> 5)//默认步长（每次从db获取一段id的数量的默认值，也是最小步长）
                .maxStep(() -> 1000)//最大步长（从db获取一段id的数量的最大值）
                .cacheHoldSeconds(() -> 600)//如果从db获取的一段id在这个时间段内就触发了loadFromDb，则调整步长（小于则增大步长，大于则减少步长）
                .expireSeconds(() -> 7*24*3600)//id在redis里缓存的时间
                .build();

        Long tag = 1L;
        for (int i=0; i<20; i++) {
            System.out.println(idGenerator.generate(tag));
        }
    }

    private static class DbIDLoader implements IDLoader<Long> {

        private static ConcurrentHashMap<Long, AtomicLong> db = new ConcurrentHashMap<>();

        @Override
        public IDRange load(Long tag, int step) {
            AtomicLong count = db.computeIfAbsent(tag, k -> new AtomicLong(0));
            long start = count.getAndAdd(step);
            IDRange idRange = new IDRange(start + 1, start + 1 + step);
            System.out.println("hit to db, return [" + idRange.getStart() + "," + idRange.getEnd() + "]");
            return idRange;
        }
    }
}
