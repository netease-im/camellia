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

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by caojiajun on 2024/12/13
 */
public class RedisZSetLRUTest {

    @Test
    public void test() {
        Map<BytesKey, Double> map = new HashMap<>();
        map.put(new BytesKey(Utils.stringToBytes("a")), 1.0);
        map.put(new BytesKey(Utils.stringToBytes("b")), 2.0);
        map.put(new BytesKey(Utils.stringToBytes("c")), 3.0);
        RedisZSet redisZSet = new RedisZSet(map);

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(new BytesKey(Utils.stringToBytes("c")), 4.0);
            map1.put(new BytesKey(Utils.stringToBytes("d")), 5.0);
            Map<BytesKey, Double> zadd = redisZSet.zadd(map1);
            assertEquals(zadd.size(), 1);
            assertTrue(zadd.containsKey(new BytesKey(Utils.stringToBytes("c"))));
        }

        {
            List<ZSetTuple> zrange = redisZSet.zrange(0, -1);
            assertEquals(zrange.size(), 4);
            assertEquals(zrange.get(0).getMember(), new BytesKey(Utils.stringToBytes("a")));
            assertEquals(zrange.get(0).getScore(), 1.0, 0.00001);
            assertEquals(zrange.get(1).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrange.get(1).getScore(), 2.0, 0.00001);
            assertEquals(zrange.get(2).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrange.get(2).getScore(), 4.0, 0.00001);
            assertEquals(zrange.get(3).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrange.get(3).getScore(), 5.0, 0.00001);
        }


        {
            List<ZSetTuple> zrange = redisZSet.zrange(1, 3);
            assertEquals(zrange.size(), 3);

            assertEquals(zrange.get(0).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrange.get(0).getScore(), 2.0, 0.00001);
            assertEquals(zrange.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrange.get(1).getScore(), 4.0, 0.00001);
            assertEquals(zrange.get(2).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrange.get(2).getScore(), 5.0, 0.00001);
        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(new BytesKey(Utils.stringToBytes("a")), 1.0);
            map1.put(new BytesKey(Utils.stringToBytes("b")), 2.0);
            map1.put(new BytesKey(Utils.stringToBytes("c")), 3.0);
            map1.put(new BytesKey(Utils.stringToBytes("d")), 4.0);
            map1.put(new BytesKey(Utils.stringToBytes("e")), 5.0);
            map1.put(new BytesKey(Utils.stringToBytes("f")), 6.0);
            redisZSet.zadd(map1);

            List<ZSetTuple> zrange = redisZSet.zrange(2, 4);
            assertEquals(zrange.size(), 3);
            assertEquals(zrange.get(0).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrange.get(0).getScore(), 3.0, 0.00001);
            assertEquals(zrange.get(1).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrange.get(1).getScore(), 4.0, 0.00001);
            assertEquals(zrange.get(2).getMember(), new BytesKey(Utils.stringToBytes("e")));
            assertEquals(zrange.get(2).getScore(), 5.0, 0.00001);

            List<ZSetTuple> zrange1 = redisZSet.zrange(2, 10);
            assertEquals(zrange1.size(), 4);
            assertEquals(zrange1.get(0).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrange1.get(0).getScore(), 3.0, 0.00001);
            assertEquals(zrange1.get(1).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrange1.get(1).getScore(), 4.0, 0.00001);
            assertEquals(zrange1.get(2).getMember(), new BytesKey(Utils.stringToBytes("e")));
            assertEquals(zrange1.get(2).getScore(), 5.0, 0.00001);
            assertEquals(zrange1.get(3).getMember(), new BytesKey(Utils.stringToBytes("f")));
            assertEquals(zrange1.get(3).getScore(), 6.0, 0.00001);

            List<ZSetTuple> zrevrange = redisZSet.zrevrange(2, 4);
            assertEquals(zrevrange.size(), 3);
            assertEquals(zrevrange.get(0).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrevrange.get(0).getScore(), 4.0, 0.00001);
            assertEquals(zrevrange.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrevrange.get(1).getScore(), 3.0, 0.00001);
            assertEquals(zrevrange.get(2).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrevrange.get(2).getScore(), 2.0, 0.00001);


            List<ZSetTuple> zrevrange1 = redisZSet.zrevrange(2, -1);
            assertEquals(zrevrange1.size(), 4);
            assertEquals(zrevrange1.get(0).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrevrange1.get(0).getScore(), 4.0, 0.00001);
            assertEquals(zrevrange1.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrevrange1.get(1).getScore(), 3.0, 0.00001);
            assertEquals(zrevrange1.get(2).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrevrange1.get(2).getScore(), 2.0, 0.00001);
            assertEquals(zrevrange1.get(3).getMember(), new BytesKey(Utils.stringToBytes("a")));
            assertEquals(zrevrange1.get(3).getScore(), 1.0, 0.00001);


        }
        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("1.0")), ZSetScore.fromBytes(Utils.stringToBytes("3.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 3);
            assertEquals(zrangebyscore.get(0).getMember(), new BytesKey(Utils.stringToBytes("a")));
            assertEquals(zrangebyscore.get(0).getScore(), 1.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrangebyscore.get(1).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(2).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrangebyscore.get(2).getScore(), 3.0, 0.00001);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("(4.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 2);
            assertEquals(zrangebyscore.get(0).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrangebyscore.get(0).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrangebyscore.get(1).getScore(), 3.0, 0.00001);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 5);
            assertEquals(zrangebyscore.get(0).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrangebyscore.get(0).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrangebyscore.get(1).getScore(), 3.0, 0.00001);
            assertEquals(zrangebyscore.get(2).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrangebyscore.get(2).getScore(), 4.0, 0.00001);
            assertEquals(zrangebyscore.get(3).getMember(), new BytesKey(Utils.stringToBytes("e")));
            assertEquals(zrangebyscore.get(3).getScore(), 5.0, 0.00001);
            assertEquals(zrangebyscore.get(4).getMember(), new BytesKey(Utils.stringToBytes("f")));
            assertEquals(zrangebyscore.get(4).getScore(), 6.0, 0.00001);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("-inf")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 6);
            assertEquals(zrangebyscore.get(0).getMember(), new BytesKey(Utils.stringToBytes("a")));
            assertEquals(zrangebyscore.get(0).getScore(), 1.0, 0.00001);
            assertEquals(zrangebyscore.get(1).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(zrangebyscore.get(1).getScore(), 2.0, 0.00001);
            assertEquals(zrangebyscore.get(2).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(zrangebyscore.get(2).getScore(), 3.0, 0.00001);
            assertEquals(zrangebyscore.get(3).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(zrangebyscore.get(3).getScore(), 4.0, 0.00001);
            assertEquals(zrangebyscore.get(4).getMember(), new BytesKey(Utils.stringToBytes("e")));
            assertEquals(zrangebyscore.get(4).getScore(), 5.0, 0.00001);
            assertEquals(zrangebyscore.get(5).getMember(), new BytesKey(Utils.stringToBytes("f")));
            assertEquals(zrangebyscore.get(5).getScore(), 6.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("4.0")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 3);
            assertEquals(list.get(0).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(list.get(0).getScore(), 4.0, 0.00001);
            assertEquals(list.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(list.get(1).getScore(), 3.0, 0.00001);
            assertEquals(list.get(2).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(list.get(2).getScore(), 2.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 5);
            assertEquals(list.get(0).getMember(), new BytesKey(Utils.stringToBytes("f")));
            assertEquals(list.get(0).getScore(), 6.0, 0.00001);
            assertEquals(list.get(1).getMember(), new BytesKey(Utils.stringToBytes("e")));
            assertEquals(list.get(1).getScore(), 5.0, 0.00001);
            assertEquals(list.get(2).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(list.get(2).getScore(), 4.0, 0.00001);
            assertEquals(list.get(3).getMember(), new BytesKey(Utils.stringToBytes("c")));
            assertEquals(list.get(3).getScore(), 3.0, 0.00001);
            assertEquals(list.get(4).getMember(), new BytesKey(Utils.stringToBytes("b")));
            assertEquals(list.get(4).getScore(), 2.0, 0.00001);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.fromBytes(new byte[][]{Utils.stringToBytes("LIMIT"), Utils.stringToBytes("2"), Utils.stringToBytes("2")}, 0));
            assertEquals(list.size(), 2);
            assertEquals(list.get(0).getMember(), new BytesKey(Utils.stringToBytes("d")));
            assertEquals(list.get(0).getScore(), 4.0, 0.00001);
            assertEquals(list.get(1).getMember(), new BytesKey(Utils.stringToBytes("c")));
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
            map1.put(new BytesKey(Utils.stringToBytes("a")), 1.0);
            map1.put(new BytesKey(Utils.stringToBytes("b")), 2.0);
            map1.put(new BytesKey(Utils.stringToBytes("c")), 3.0);
            map1.put(new BytesKey(Utils.stringToBytes("d")), 4.0);
            map1.put(new BytesKey(Utils.stringToBytes("e")), 5.0);
            map1.put(new BytesKey(Utils.stringToBytes("f")), 6.0);
            redisZSet.zadd(map1);

            Map<BytesKey, Double> zremrangeByRank = redisZSet.zremrangeByRank(2, 4);
            assertEquals(zremrangeByRank.size(), 3);
            assertTrue(zremrangeByRank.containsKey(new BytesKey(Utils.stringToBytes("c"))));
            assertTrue(zremrangeByRank.containsKey(new BytesKey(Utils.stringToBytes("d"))));
            assertTrue(zremrangeByRank.containsKey(new BytesKey(Utils.stringToBytes("e"))));
        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(new BytesKey(Utils.stringToBytes("a")), 1.0);
            map1.put(new BytesKey(Utils.stringToBytes("b")), 2.0);
            map1.put(new BytesKey(Utils.stringToBytes("c")), 3.0);
            map1.put(new BytesKey(Utils.stringToBytes("d")), 4.0);
            map1.put(new BytesKey(Utils.stringToBytes("e")), 5.0);
            map1.put(new BytesKey(Utils.stringToBytes("f")), 6.0);
            redisZSet.zadd(map1);

            Map<BytesKey, Double> zremrangeByScore = redisZSet.zremrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("4.0")));
            assertEquals(zremrangeByScore.size(), 3);
            assertTrue(zremrangeByScore.containsKey(new BytesKey(Utils.stringToBytes("b"))));
            assertTrue(zremrangeByScore.containsKey(new BytesKey(Utils.stringToBytes("c"))));
            assertTrue(zremrangeByScore.containsKey(new BytesKey(Utils.stringToBytes("d"))));

        }

        {
            Map<BytesKey, Double> map1 = new HashMap<>();
            map1.put(new BytesKey(Utils.stringToBytes("a")), 1.0);
            map1.put(new BytesKey(Utils.stringToBytes("b")), 2.0);
            map1.put(new BytesKey(Utils.stringToBytes("c")), 3.0);
            map1.put(new BytesKey(Utils.stringToBytes("d")), 4.0);
            map1.put(new BytesKey(Utils.stringToBytes("e")), 5.0);
            map1.put(new BytesKey(Utils.stringToBytes("f")), 6.0);
            redisZSet.zadd(map1);

            Map<BytesKey, Double> zremrangeByLex = redisZSet.zremrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("(b")), ZSetLex.fromLex(Utils.stringToBytes("[e")));
            assertEquals(zremrangeByLex.size(), 3);
            assertTrue(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("c"))));
            assertTrue(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("d"))));
            assertTrue(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("e"))));

        }
    }
}
