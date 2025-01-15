package com.netease.nim.camellia.redis.proxy.test;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLex;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLimit;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetScore;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetTuple;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by caojiajun on 2024/12/13
 */
public class RedisZSetLRUTest {

    private static final BytesKey a = new BytesKey(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    private static final BytesKey b = new BytesKey(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    private static final BytesKey c = new BytesKey(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    private static final BytesKey d = new BytesKey(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    private static final BytesKey e = new BytesKey(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    private static final BytesKey f = new BytesKey(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

    @Test
    public void test() {
        System.out.println("a=" + Utils.bytesToString(a.getKey()));
        System.out.println("b=" + Utils.bytesToString(b.getKey()));
        System.out.println("c=" + Utils.bytesToString(c.getKey()));
        System.out.println("d=" + Utils.bytesToString(d.getKey()));
        System.out.println("e=" + Utils.bytesToString(e.getKey()));
        System.out.println("f=" + Utils.bytesToString(f.getKey()));

        Map<BytesKey, Double> map = new HashMap<>();
        map.put(a, 1.0);
        map.put(b, 2.0);
        map.put(c, 3.0);
        RedisZSet redisZSet = new RedisZSet(map);

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(c, 4.0);
            map1.put(d, 5.0);
            Map<BytesKey, Double> zadd = redisZSet.zadd(map1);
            assertEquals(zadd.size(), 1);
            assertTrue(zadd.containsKey(c));
        }

        {
            List<ZSetTuple> zrange = redisZSet.zrange(0, -1);
            assertEquals(zrange.size(), 4);
            assertEquals(zrange.get(0).getMember(), a);
            assertEquals(zrange.get(0).getScore(), 1.0, 0.00001);
            assertEquals(zrange.get(1).getMember(), b);
            assertEquals(zrange.get(1).getScore(), 2.0, 0.00001);
            assertEquals(zrange.get(2).getMember(), c);
            assertEquals(zrange.get(2).getScore(), 4.0, 0.00001);
            assertEquals(zrange.get(3).getMember(), d);
            assertEquals(zrange.get(3).getScore(), 5.0, 0.00001);
        }


        {
            List<ZSetTuple> zrange = redisZSet.zrange(1, 3);
            assertEquals(zrange.size(), 3);

            assertEquals(zrange.get(0).getMember(), b);
            assertEquals(zrange.get(0).getScore(), 2.0, 0.00001);
            assertEquals(zrange.get(1).getMember(), c);
            assertEquals(zrange.get(1).getScore(), 4.0, 0.00001);
            assertEquals(zrange.get(2).getMember(), d);
            assertEquals(zrange.get(2).getScore(), 5.0, 0.00001);
        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(a, 1.0);
            map1.put(b, 2.0);
            map1.put(c, 3.0);
            map1.put(d, 4.0);
            map1.put(e, 5.0);
            map1.put(f, 6.0);
            redisZSet.zadd(map1);

            List<ZSetTuple> zrange = redisZSet.zrange(2, 4);
            assertEquals(zrange.size(), 3);
            assertEquals(zrange.get(0).getMember(), c);
            assertEquals(zrange.get(0).getScore(), 3.0, 0.00001);
            assertEquals(zrange.get(1).getMember(), d);
            assertEquals(zrange.get(1).getScore(), 4.0, 0.00001);
            assertEquals(zrange.get(2).getMember(), e);
            assertEquals(zrange.get(2).getScore(), 5.0, 0.00001);

            List<ZSetTuple> zrange1 = redisZSet.zrange(2, 10);

            assertEquals(zrange1.size(), 4);

            assertEquals(zrange1.get(0).getMember(), c);
            assertEquals(zrange1.get(0).getScore(), 3.0, 0.00001);
            assertEquals(zrange1.get(1).getMember(), d);
            assertEquals(zrange1.get(1).getScore(), 4.0, 0.00001);
            assertEquals(zrange1.get(2).getMember(), e);
            assertEquals(zrange1.get(2).getScore(), 5.0, 0.00001);
            assertEquals(zrange1.get(3).getMember(), f);
            assertEquals(zrange1.get(3).getScore(), 6.0, 0.00001);

            List<ZSetTuple> zrevrange = redisZSet.zrevrange(2, 4);
            assertEquals(zrevrange.size(), 3);
            assertEquals(zrevrange.get(0).getMember(), d);
            assertEquals(zrevrange.get(0).getScore(), 4.0, 0.00001);
            assertEquals(zrevrange.get(1).getMember(), c);
            assertEquals(zrevrange.get(1).getScore(), 3.0, 0.00001);
            assertEquals(zrevrange.get(2).getMember(), b);
            assertEquals(zrevrange.get(2).getScore(), 2.0, 0.00001);


            List<ZSetTuple> zrevrange1 = redisZSet.zrevrange(2, -1);
            assertEquals(zrevrange1.size(), 4);
            assertEquals(zrevrange1.get(0).getMember(), d);
            assertEquals(zrevrange1.get(0).getScore(), 4.0, 0.00001);
            assertEquals(zrevrange1.get(1).getMember(), c);
            assertEquals(zrevrange1.get(1).getScore(), 3.0, 0.00001);
            assertEquals(zrevrange1.get(2).getMember(), b);
            assertEquals(zrevrange1.get(2).getScore(), 2.0, 0.00001);
            assertEquals(zrevrange1.get(3).getMember(), a);
            assertEquals(zrevrange1.get(3).getScore(), 1.0, 0.00001);


        }
        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("1.0")), ZSetScore.fromBytes(Utils.stringToBytes("3.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 3);
            assertEquals(zrangebyscore.get(0).getMember(), a);
            assertEquals(zrangebyscore.get(0).getScore(), 1.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), b);
            assertEquals(zrangebyscore.get(1).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(2).getMember(), c);
            assertEquals(zrangebyscore.get(2).getScore(), 3.0, 0.00001);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("(4.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 2);
            assertEquals(zrangebyscore.get(0).getMember(), b);
            assertEquals(zrangebyscore.get(0).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), c);
            assertEquals(zrangebyscore.get(1).getScore(), 3.0, 0.00001);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 5);
            assertEquals(zrangebyscore.get(0).getMember(), b);
            assertEquals(zrangebyscore.get(0).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), c);
            assertEquals(zrangebyscore.get(1).getScore(), 3.0, 0.00001);
            assertEquals(zrangebyscore.get(2).getMember(), d);
            assertEquals(zrangebyscore.get(2).getScore(), 4.0, 0.00001);
            assertEquals(zrangebyscore.get(3).getMember(), e);
            assertEquals(zrangebyscore.get(3).getScore(), 5.0, 0.00001);
            assertEquals(zrangebyscore.get(4).getMember(), f);
            assertEquals(zrangebyscore.get(4).getScore(), 6.0, 0.00001);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("-inf")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 6);
            assertEquals(zrangebyscore.get(0).getMember(), a);
            assertEquals(zrangebyscore.get(0).getScore(), 1.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), b);
            assertEquals(zrangebyscore.get(1).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(2).getMember(), c);
            assertEquals(zrangebyscore.get(2).getScore(), 3.0, 0.00001);
            assertEquals(zrangebyscore.get(3).getMember(), d);
            assertEquals(zrangebyscore.get(3).getScore(), 4.0, 0.00001);
            assertEquals(zrangebyscore.get(4).getMember(), e);
            assertEquals(zrangebyscore.get(4).getScore(), 5.0, 0.00001);
            assertEquals(zrangebyscore.get(5).getMember(), f);
            assertEquals(zrangebyscore.get(5).getScore(), 6.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("4.0")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 3);
            assertEquals(list.get(0).getMember(), d);
            assertEquals(list.get(0).getScore(), 4.0, 0.00001);
            assertEquals(list.get(1).getMember(), c);
            assertEquals(list.get(1).getScore(), 3.0, 0.00001);
            assertEquals(list.get(2).getMember(), b);
            assertEquals(list.get(2).getScore(), 2.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 5);
            assertEquals(list.get(0).getMember(), f);
            assertEquals(list.get(0).getScore(), 6.0, 0.00001);
            assertEquals(list.get(1).getMember(), e);
            assertEquals(list.get(1).getScore(), 5.0, 0.00001);
            assertEquals(list.get(2).getMember(), d);
            assertEquals(list.get(2).getScore(), 4.0, 0.00001);
            assertEquals(list.get(3).getMember(), c);
            assertEquals(list.get(3).getScore(), 3.0, 0.00001);
            assertEquals(list.get(4).getMember(), b);
            assertEquals(list.get(4).getScore(), 2.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.fromBytes(new byte[][]{Utils.stringToBytes("LIMIT"), Utils.stringToBytes("2"), Utils.stringToBytes("2")}, 0));
            assertEquals(list.size(), 2);
            assertEquals(list.get(0).getMember(), d);
            assertEquals(list.get(0).getScore(), 4.0, 0.00001);
            assertEquals(list.get(1).getMember(), c);
            assertEquals(list.get(1).getScore(), 3.0, 0.00001);
        }
        {
            List<ZSetTuple> zrange = redisZSet.zrange(3, 2);
            assertEquals(zrange.size(), 0);

            List<ZSetTuple> zrevrange = redisZSet.zrevrange(3, 2);
            assertEquals(zrevrange.size(), 0);

            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("4.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("2.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 0);

            List<ZSetTuple> zrevrangeByScore = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("4.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("2.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrevrangeByScore.size(), 0);

        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(new BytesKey("a".getBytes(StandardCharsets.UTF_8)), 1.0);
            map1.put(new BytesKey("b".getBytes(StandardCharsets.UTF_8)), 2.0);
            map1.put(new BytesKey("c".getBytes(StandardCharsets.UTF_8)), 3.0);
            map1.put(new BytesKey("d".getBytes(StandardCharsets.UTF_8)), 4.0);
            map1.put(new BytesKey("e".getBytes(StandardCharsets.UTF_8)), 5.0);
            map1.put(new BytesKey("f".getBytes(StandardCharsets.UTF_8)), 6.0);
            redisZSet.zadd(map1);
            redisZSet = new RedisZSet(map1);
        }

        {
            List<ZSetTuple> list = redisZSet.zrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("(a")), ZSetLex.fromLex(Utils.stringToBytes("[d")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 3);
            assertEquals(list.get(0).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(list.get(0).getScore(), 2.0, 0.00001);
            assertEquals(list.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(list.get(1).getScore(), 3.0, 0.00001);
            assertEquals(list.get(2).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(list.get(2).getScore(), 4.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("(a")), ZSetLex.fromLex(Utils.stringToBytes("+")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 5);
            assertEquals(list.get(0).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(list.get(0).getScore(), 2.0, 0.00001);
            assertEquals(list.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(list.get(1).getScore(), 3.0, 0.00001);
            assertEquals(list.get(2).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(list.get(2).getScore(), 4.0, 0.00001);
            assertEquals(list.get(3).getMember(), new BytesKey(Utils.stringToBytes("e")));
            assertEquals(list.get(3).getScore(), 5.0, 0.00001);
            assertEquals(list.get(4).getMember(), new BytesKey(Utils.stringToBytes("f")));
            assertEquals(list.get(4).getScore(), 6.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("-")), ZSetLex.fromLex(Utils.stringToBytes("+")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 6);
            assertEquals(list.get(0).getMember(), new BytesKey(Utils.stringToBytes("a")));
            assertEquals(list.get(0).getScore(), 1.0, 0.00001);
            assertEquals(list.get(1).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(list.get(1).getScore(), 2.0, 0.00001);
            assertEquals(list.get(2).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(list.get(2).getScore(), 3.0, 0.00001);
            assertEquals(list.get(3).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(list.get(3).getScore(), 4.0, 0.00001);
            assertEquals(list.get(4).getMember(), new BytesKey(Utils.stringToBytes("e")));
            assertEquals(list.get(4).getScore(), 5.0, 0.00001);
            assertEquals(list.get(5).getMember(), new BytesKey(Utils.stringToBytes("f")));
            assertEquals(list.get(5).getScore(), 6.0, 0.00001);
        }
        {
            int zcard = redisZSet.zcard();
            assertEquals(zcard, 6);

            int zcount = redisZSet.zcount(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("5.0")));
            assertEquals(zcount, 4);

            int zlexcount = redisZSet.zlexcount(ZSetLex.fromLex(Utils.stringToBytes("(a")), ZSetLex.fromLex(Utils.stringToBytes("[d")));
            assertEquals(zlexcount, 3);
        }
        {
            Set<BytesKey> rem = new HashSet<>();
            rem.add(new BytesKey(Utils.stringToBytes("b")));
            rem.add(new BytesKey(Utils.stringToBytes("h")));
            Map<BytesKey, Double> b = redisZSet.zrem(rem);
            assertEquals(b.size(), 1);
            for (Map.Entry<BytesKey, Double> entry : b.entrySet()) {
                assertEquals(entry.getKey(), new BytesKey(Utils.stringToBytes("b")));
                assertEquals(entry.getValue(), 2.0, 0.00001);
            }

            List<ZSetTuple> zrange = redisZSet.zrange(0, -1);
            assertEquals(zrange.size(), 5);
            for (ZSetTuple zSetTuple : zrange) {
                Assert.assertNotEquals(zSetTuple.getMember(), new BytesKey(Utils.stringToBytes("b")));
            }

            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("0.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("1000")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 5);
            for (ZSetTuple zSetTuple : zrangebyscore) {
                Assert.assertNotEquals(zSetTuple.getMember(), new BytesKey(Utils.stringToBytes("b")));
            }

            List<ZSetTuple> zrangeByLex = redisZSet.zrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("-")), ZSetLex.fromLex(Utils.stringToBytes("+")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangeByLex.size(), 5);
            for (ZSetTuple zSetTuple : zrange) {
                Assert.assertNotEquals(zSetTuple.getMember(), new BytesKey(Utils.stringToBytes("b")));
            }
        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(a, 1.0);
            map1.put(b, 2.0);
            map1.put(c, 3.0);
            map1.put(d, 4.0);
            map1.put(e, 5.0);
            map1.put(f, 6.0);
            redisZSet = new RedisZSet(map1);

            Map<BytesKey, Double> zremrangeByRank = redisZSet.zremrangeByRank(2, 4);
            assertEquals(zremrangeByRank.size(), 3);
            assertTrue(zremrangeByRank.containsKey(c));
            assertTrue(zremrangeByRank.containsKey(d));
            assertTrue(zremrangeByRank.containsKey(e));
        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(a, 1.0);
            map1.put(b, 2.0);
            map1.put(c, 3.0);
            map1.put(d, 4.0);
            map1.put(e, 5.0);
            map1.put(f, 6.0);
            redisZSet.zadd(map1);

            Map<BytesKey, Double> zremrangeByScore = redisZSet.zremrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("4.0")));
            assertEquals(zremrangeByScore.size(), 3);
            assertTrue(zremrangeByScore.containsKey(b));
            assertTrue(zremrangeByScore.containsKey(c));
            assertTrue(zremrangeByScore.containsKey(d));

        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(new BytesKey(Utils.stringToBytes("a")), 1.0);
            map1.put(new BytesKey(Utils.stringToBytes("b")), 2.0);
            map1.put(new BytesKey(Utils.stringToBytes("c")), 3.0);
            map1.put(new BytesKey(Utils.stringToBytes("d")), 4.0);
            map1.put(new BytesKey(Utils.stringToBytes("e")), 5.0);
            map1.put(new BytesKey(Utils.stringToBytes("f")), 6.0);
            redisZSet = new RedisZSet(map1);

            Map<BytesKey, Double> zremrangeByLex = redisZSet.zremrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("(b")), ZSetLex.fromLex(Utils.stringToBytes("[e")));
            assertEquals(zremrangeByLex.size(), 3);
            assertTrue(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("c"))));
            assertTrue(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("d"))));
            assertTrue(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("e"))));

        }
    }
}
