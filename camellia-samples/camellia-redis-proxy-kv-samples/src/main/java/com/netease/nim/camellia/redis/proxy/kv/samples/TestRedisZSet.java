package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.redis.proxy.upstream.kv.cache.RedisZSet;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLex;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetLimit;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetScore;
import com.netease.nim.camellia.redis.proxy.upstream.kv.command.zset.utils.ZSetTuple;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.util.*;

/**
 * Created by caojiajun on 2024/8/23
 */
public class TestRedisZSet {

    public static void main(String[] args) {
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
            assertEquals(zadd.containsKey(new BytesKey(Utils.stringToBytes("c"))), true);
        }

        {
            List<ZSetTuple> zrange = redisZSet.zrange(0, -1);
            assertEquals(zrange.size(), 4);
            assertEquals(zrange.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("a"))), true);
            assertEquals(zrange.get(0).getScore(), 1.0);
            assertEquals(zrange.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrange.get(1).getScore(), 2.0);
            assertEquals(zrange.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrange.get(2).getScore(), 4.0);
            assertEquals(zrange.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrange.get(3).getScore(), 5.0);
        }


        {
            List<ZSetTuple> zrange = redisZSet.zrange(1, 3);
            assertEquals(zrange.size(), 3);

            assertEquals(zrange.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrange.get(0).getScore(), 2.0);
            assertEquals(zrange.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrange.get(1).getScore(), 4.0);
            assertEquals(zrange.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrange.get(2).getScore(), 5.0);
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
            assertEquals(zrange.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrange.get(0).getScore(), 3.0);
            assertEquals(zrange.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrange.get(1).getScore(), 4.0);
            assertEquals(zrange.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("e"))), true);
            assertEquals(zrange.get(2).getScore(), 5.0);

            List<ZSetTuple> zrange1 = redisZSet.zrange(2, 10);
            assertEquals(zrange1.size(), 4);
            assertEquals(zrange1.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrange1.get(0).getScore(), 3.0);
            assertEquals(zrange1.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrange1.get(1).getScore(), 4.0);
            assertEquals(zrange1.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("e"))), true);
            assertEquals(zrange1.get(2).getScore(), 5.0);
            assertEquals(zrange1.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("f"))), true);
            assertEquals(zrange1.get(3).getScore(), 6.0);

            List<ZSetTuple> zrevrange = redisZSet.zrevrange(2, 4);
            assertEquals(zrevrange.size(), 3);
            assertEquals(zrevrange.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrevrange.get(0).getScore(), 4.0);
            assertEquals(zrevrange.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrevrange.get(1).getScore(), 3.0);
            assertEquals(zrevrange.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrevrange.get(2).getScore(), 2.0);


            List<ZSetTuple> zrevrange1 = redisZSet.zrevrange(2, -1);
            assertEquals(zrevrange1.size(), 4);
            assertEquals(zrevrange1.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrevrange1.get(0).getScore(), 4.0);
            assertEquals(zrevrange1.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrevrange1.get(1).getScore(), 3.0);
            assertEquals(zrevrange1.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrevrange1.get(2).getScore(), 2.0);
            assertEquals(zrevrange1.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("a"))), true);
            assertEquals(zrevrange1.get(3).getScore(), 1.0);


        }
        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("1.0")), ZSetScore.fromBytes(Utils.stringToBytes("3.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 3);
            assertEquals(zrangebyscore.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("a"))), true);
            assertEquals(zrangebyscore.get(0).getScore(), 1.0);
            assertEquals(zrangebyscore.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrangebyscore.get(1).getScore(), 2.0);
            assertEquals(zrangebyscore.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrangebyscore.get(2).getScore(), 3.0);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("(4.0")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 2);
            assertEquals(zrangebyscore.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrangebyscore.get(0).getScore(), 2.0);
            assertEquals(zrangebyscore.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrangebyscore.get(1).getScore(), 3.0);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 5);
            assertEquals(zrangebyscore.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrangebyscore.get(0).getScore(), 2.0);
            assertEquals(zrangebyscore.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrangebyscore.get(1).getScore(), 3.0);
            assertEquals(zrangebyscore.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrangebyscore.get(2).getScore(), 4.0);
            assertEquals(zrangebyscore.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("e"))), true);
            assertEquals(zrangebyscore.get(3).getScore(), 5.0);
            assertEquals(zrangebyscore.get(4).getMember().equals(new BytesKey(Utils.stringToBytes("f"))), true);
            assertEquals(zrangebyscore.get(4).getScore(), 6.0);
        }

        {
            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("-inf")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 6);
            assertEquals(zrangebyscore.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("a"))), true);
            assertEquals(zrangebyscore.get(0).getScore(), 1.0);
            assertEquals(zrangebyscore.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zrangebyscore.get(1).getScore(), 2.0);
            assertEquals(zrangebyscore.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zrangebyscore.get(2).getScore(), 3.0);
            assertEquals(zrangebyscore.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zrangebyscore.get(3).getScore(), 4.0);
            assertEquals(zrangebyscore.get(4).getMember().equals(new BytesKey(Utils.stringToBytes("e"))), true);
            assertEquals(zrangebyscore.get(4).getScore(), 5.0);
            assertEquals(zrangebyscore.get(5).getMember().equals(new BytesKey(Utils.stringToBytes("f"))), true);
            assertEquals(zrangebyscore.get(5).getScore(), 6.0);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("4.0")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 3);
            assertEquals(list.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(list.get(0).getScore(), 4.0);
            assertEquals(list.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(list.get(1).getScore(), 3.0);
            assertEquals(list.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(list.get(2).getScore(), 2.0);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 5);
            assertEquals(list.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("f"))), true);
            assertEquals(list.get(0).getScore(), 6.0);
            assertEquals(list.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("e"))), true);
            assertEquals(list.get(1).getScore(), 5.0);
            assertEquals(list.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(list.get(2).getScore(), 4.0);
            assertEquals(list.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(list.get(3).getScore(), 3.0);
            assertEquals(list.get(4).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(list.get(4).getScore(), 2.0);
        }
        {
            List<ZSetTuple> list = redisZSet.zrevrangeByScore(ZSetScore.fromBytes(Utils.stringToBytes("2.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("+inf")), ZSetLimit.fromBytes(new byte[][]{Utils.stringToBytes("LIMIT"), Utils.stringToBytes("2"), Utils.stringToBytes("2")}, 0));
            assertEquals(list.size(), 2);
            assertEquals(list.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(list.get(0).getScore(), 4.0);
            assertEquals(list.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(list.get(1).getScore(), 3.0);
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
            assertEquals(list.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(list.get(0).getScore(), 2.0);
            assertEquals(list.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(list.get(1).getScore(), 3.0);
            assertEquals(list.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(list.get(2).getScore(), 4.0);
        }
        {
            List<ZSetTuple> list = redisZSet.zrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("(a")), ZSetLex.fromLex(Utils.stringToBytes("+")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 5);
            assertEquals(list.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(list.get(0).getScore(), 2.0);
            assertEquals(list.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(list.get(1).getScore(), 3.0);
            assertEquals(list.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(list.get(2).getScore(), 4.0);
            assertEquals(list.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("e"))), true);
            assertEquals(list.get(3).getScore(), 5.0);
            assertEquals(list.get(4).getMember().equals(new BytesKey(Utils.stringToBytes("f"))), true);
            assertEquals(list.get(4).getScore(), 6.0);
        }
        {
            List<ZSetTuple> list = redisZSet.zrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("-")), ZSetLex.fromLex(Utils.stringToBytes("+")), ZSetLimit.NO_LIMIT);
            assertEquals(list.size(), 6);
            assertEquals(list.get(0).getMember().equals(new BytesKey(Utils.stringToBytes("a"))), true);
            assertEquals(list.get(0).getScore(), 1.0);
            assertEquals(list.get(1).getMember().equals(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(list.get(1).getScore(), 2.0);
            assertEquals(list.get(2).getMember().equals(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(list.get(2).getScore(), 3.0);
            assertEquals(list.get(3).getMember().equals(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(list.get(3).getScore(), 4.0);
            assertEquals(list.get(4).getMember().equals(new BytesKey(Utils.stringToBytes("e"))), true);
            assertEquals(list.get(4).getScore(), 5.0);
            assertEquals(list.get(5).getMember().equals(new BytesKey(Utils.stringToBytes("f"))), true);
            assertEquals(list.get(5).getScore(), 6.0);
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
                assertEquals(entry.getKey().equals(new BytesKey(Utils.stringToBytes("b"))), true);
                assertEquals(entry.getValue(), 2.0);
            }

            List<ZSetTuple> zrange = redisZSet.zrange(0, -1);
            assertEquals(zrange.size(), 5);
            for (ZSetTuple zSetTuple : zrange) {
                assertEquals(zSetTuple.getMember().equals(new BytesKey(Utils.stringToBytes("b"))), false);
            }

            List<ZSetTuple> zrangebyscore = redisZSet.zrangebyscore(ZSetScore.fromBytes(Utils.stringToBytes("0.0")),
                    ZSetScore.fromBytes(Utils.stringToBytes("1000")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangebyscore.size(), 5);
            for (ZSetTuple zSetTuple : zrangebyscore) {
                assertEquals(zSetTuple.getMember().equals(new BytesKey(Utils.stringToBytes("b"))), false);
            }

            List<ZSetTuple> zrangeByLex = redisZSet.zrangeByLex(ZSetLex.fromLex(Utils.stringToBytes("-")), ZSetLex.fromLex(Utils.stringToBytes("+")), ZSetLimit.NO_LIMIT);
            assertEquals(zrangeByLex.size(), 5);
            for (ZSetTuple zSetTuple : zrange) {
                assertEquals(zSetTuple.getMember().equals(new BytesKey(Utils.stringToBytes("b"))), false);
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
            assertEquals(zremrangeByRank.containsKey(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zremrangeByRank.containsKey(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zremrangeByRank.containsKey(new BytesKey(Utils.stringToBytes("e"))), true);
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
            assertEquals(zremrangeByScore.containsKey(new BytesKey(Utils.stringToBytes("b"))), true);
            assertEquals(zremrangeByScore.containsKey(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zremrangeByScore.containsKey(new BytesKey(Utils.stringToBytes("d"))), true);

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
            assertEquals(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("c"))), true);
            assertEquals(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("d"))), true);
            assertEquals(zremrangeByLex.containsKey(new BytesKey(Utils.stringToBytes("e"))), true);

        }
    }


    private static void assertEquals(Object result, Object expect) {
        if (Objects.equals(result, expect)) {
            System.out.println("SUCCESS, thread=" + Thread.currentThread().getName());
        } else {
            System.out.println("ERROR, expect " + expect + " but found " + result);
            throw new RuntimeException();
        }
    }
}
