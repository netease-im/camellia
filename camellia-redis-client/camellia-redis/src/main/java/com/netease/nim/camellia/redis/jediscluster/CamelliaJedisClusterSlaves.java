package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedisClusterSlavesResource;
import com.netease.nim.camellia.redis.base.utils.CloseUtil;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import redis.clients.jedis.*;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.util.JedisClusterCRC16;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2024/11/11
 */
public class CamelliaJedisClusterSlaves extends CamelliaJedisCluster {

    private final JedisClusterWrapper jedisCluster;
    private final RedisClusterSlavesResource resource;
    private final CamelliaRedisEnv env;
    private final CamelliaJedisCluster camelliaJedisCluster;
    private final JedisClusterSlaves jedisClusterSlaves;

    public CamelliaJedisClusterSlaves(RedisClusterSlavesResource resource, CamelliaRedisEnv env) {
        super(new RedisClusterResource(resource.getNodes(), resource.getUserName(), resource.getPassword()), env);
        RedisClusterResource redisClusterResource = new RedisClusterResource(resource.getNodes(), resource.getUserName(), resource.getPassword());
        this.jedisClusterSlaves = env.getJedisClusterFactory().getJedisClusterSlaves(resource);
        this.jedisCluster = env.getJedisClusterFactory().getJedisCluster(redisClusterResource);
        this.camelliaJedisCluster = new CamelliaJedisCluster(redisClusterResource, env);
        this.resource = resource;
        this.env = env;
        renew();
    }

    private void renew() {
        jedisClusterSlaves.renew();
    }

    private ReadOnlyJedisPool getPool(int slot) {
        return jedisClusterSlaves.getPool(slot);
    }

    private String getSlaveNode(int slot) {
        return jedisClusterSlaves.getSlaveNode(slot);
    }

    private ReadOnlyJedisPool getPool(String slaveNode) {
        return jedisClusterSlaves.getPool(slaveNode);
    }

    @Override
    public List<Jedis> getJedisList() {
        boolean withMaster = resource.isWithMaster();
        List<Jedis> jedisList = new ArrayList<>();
        if (withMaster) {
            List<JedisPool> jedisPoolList = jedisCluster.getJedisPoolList();
            if (jedisPoolList != null) {
                for (JedisPool jedisPool : jedisPoolList) {
                    Jedis jedis = jedisPool.getResource();
                    jedisList.add(jedis);
                }
            }
        }
        Set<String> slaves = jedisClusterSlaves.getSlaves();
        for (String slave : slaves) {
            ReadOnlyJedisPool pool = jedisClusterSlaves.getPool(slave);
            jedisList.add(pool.getResource());
        }
        return jedisList;
    }

    @Override
    public Jedis getJedis(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool != null) {
            return pool.getResource();
        }
        return jedisCluster.getJedisPool(key).getResource();
    }

    @Override
    public byte[] get(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.get(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.get(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.get(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean exists(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.exists(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.exists(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.exists(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String type(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.type(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.type(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.type(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long ttl(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.ttl(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.ttl(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.ttl(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long pttl(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.ttl(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.pttl(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.pttl(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean getbit(byte[] key, long offset) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.getbit(key, offset);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.getbit(key, offset);
            } catch (Exception e) {
                renew();
                return jedisCluster.getbit(key, offset);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public byte[] getrange(byte[] key, long startOffset, long endOffset) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.getrange(key, startOffset, endOffset);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.getrange(key, startOffset, endOffset);
            } catch (Exception e) {
                renew();
                return jedisCluster.getrange(key, startOffset, endOffset);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hget(key, field);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hget(key, field);
            } catch (Exception e) {
                renew();
                return jedisCluster.hget(key, field);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hmget(key, fields);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hmget(key, fields);
            } catch (Exception e) {
                renew();
                return jedisCluster.hmget(key, fields);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hexists(key, field);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hexists(key, field);
            } catch (Exception e) {
                renew();
                return jedisCluster.hexists(key, field);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long hlen(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hlen(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hlen(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.hlen(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hkeys(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hkeys(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.hkeys(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return new ArrayList<>(jedisCluster.hvals(key));
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hvals(key);
            } catch (Exception e) {
                renew();
                return new ArrayList<>(jedisCluster.hvals(key));
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hgetAll(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hgetAll(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.hgetAll(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }


    @Override
    public Long llen(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.llen(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.llen(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.llen(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.lrange(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.lrange(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.lrange(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String ltrim(byte[] key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.ltrim(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.ltrim(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.ltrim(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.lindex(key, index);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.lindex(key, index);
            } catch (Exception e) {
                renew();
                return jedisCluster.lindex(key, index);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.smembers(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.smembers(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.smembers(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long scard(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.scard(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.scard(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.scard(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sismember(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sismember(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.sismember(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public byte[] srandmember(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.srandmember(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.srandmember(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.srandmember(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.srandmember(key, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.srandmember(key, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.srandmember(key, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long strlen(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.strlen(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.strlen(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.strlen(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrange(byte[] key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrange(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrange(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrange(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrank(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrank(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrank(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrank(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrank(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrank(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrevrange(byte[] key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrange(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrange(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrange(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeWithScores(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeWithScores(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeWithScores(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeWithScores(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeWithScores(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeWithScores(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zcard(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zcard(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zcard(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.zcard(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zscore(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zscore(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.zscore(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<byte[]> sort(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sort(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sort(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.sort(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sort(key, sortingParameters);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sort(key, sortingParameters);
            } catch (Exception e) {
                renew();
                return jedisCluster.sort(key, sortingParameters);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zcount(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zcount(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zcount(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zcount(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zcount(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zcount(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zlexcount(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zlexcount(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zlexcount(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByLex(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByLex(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByLex(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByLex(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByLex(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByLex(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByLex(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByLex(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByLex(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByLex(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitcount(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitcount(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitcount(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitcount(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitcount(byte[] key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitcount(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitcount(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitcount(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }


    @Override
    public long pfcount(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.pfcount(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.pfcount(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.pfcount(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geodist(key, member1, member2);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geodist(key, member1, member2);
            } catch (Exception e) {
                renew();
                return jedisCluster.geodist(key, member1, member2);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geodist(key, member1, member2, unit);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geodist(key, member1, member2, unit);
            } catch (Exception e) {
                renew();
                return jedisCluster.geodist(key, member1, member2, unit);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<byte[]> geohash(byte[] key, byte[]... members) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geohash(key, members);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geohash(key, members);
            } catch (Exception e) {
                renew();
                return jedisCluster.geohash(key, members);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geopos(key, members);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geopos(key, members);
            } catch (Exception e) {
                renew();
                return jedisCluster.geopos(key, members);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadius(key, longitude, latitude, radius, unit);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadius(key, longitude, latitude, radius, unit);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadius(key, longitude, latitude, radius, unit, param);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadiusByMember(key, member, radius, unit);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadiusByMember(key, member, radius, unit);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadiusByMember(key, member, radius, unit);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadiusByMember(key, member, radius, unit, param);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadiusByMember(key, member, radius, unit, param);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadiusByMember(key, member, radius, unit, param);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hscan(key, cursor);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hscan(key, cursor);
            } catch (Exception e) {
                renew();
                return jedisCluster.hscan(key, cursor);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hscan(key, cursor, params);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hscan(key, cursor, params);
            } catch (Exception e) {
                renew();
                return jedisCluster.hscan(key, cursor, params);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sscan(key, cursor);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sscan(key, cursor);
            } catch (Exception e) {
                renew();
                return jedisCluster.sscan(key, cursor);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sscan(key, cursor, params);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sscan(key, cursor, params);
            } catch (Exception e) {
                renew();
                return jedisCluster.sscan(key, cursor, params);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zscan(key, cursor);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zscan(key, cursor);
            } catch (Exception e) {
                renew();
                return jedisCluster.zscan(key, cursor);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zscan(key, cursor, params);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zscan(key, cursor, params);
            } catch (Exception e) {
                renew();
                return jedisCluster.zscan(key, cursor, params);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<Long> bitfield(byte[] key, byte[]... arguments) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitfield(key, arguments);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitfield(key, arguments);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitfield(key, arguments);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String get(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.get(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.get(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.get(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean exists(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.exists(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.exists(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.exists(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String type(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.type(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.type(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.type(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long ttl(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.ttl(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.ttl(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.ttl(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long pttl(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.pttl(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.pttl(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.pttl(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean getbit(String key, long offset) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.getbit(key, offset);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.getbit(key, offset);
            } catch (Exception e) {
                renew();
                return jedisCluster.getbit(key, offset);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.getrange(key, startOffset, endOffset);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.getrange(key, startOffset, endOffset);
            } catch (Exception e) {
                renew();
                return jedisCluster.getrange(key, startOffset, endOffset);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String substr(String key, int start, int end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.substr(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.substr(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.substr(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String hget(String key, String field) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hget(key, field);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hget(key, field);
            } catch (Exception e) {
                renew();
                return jedisCluster.hget(key, field);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hmget(key, fields);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hmget(key, fields);
            } catch (Exception e) {
                renew();
                return jedisCluster.hmget(key, fields);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean hexists(String key, String field) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hexists(key, field);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hexists(key, field);
            } catch (Exception e) {
                renew();
                return jedisCluster.hexists(key, field);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long hlen(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hlen(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hlen(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.hlen(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> hkeys(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hkeys(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hkeys(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.hkeys(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<String> hvals(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hvals(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hvals(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.hvals(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hgetAll(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hgetAll(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.hgetAll(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long llen(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.llen(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.llen(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.llen(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.lrange(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.lrange(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.lrange(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String ltrim(String key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.ltrim(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.ltrim(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.ltrim(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String lindex(String key, long index) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.lindex(key, index);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.lindex(key, index);
            } catch (Exception e) {
                renew();
                return jedisCluster.lindex(key, index);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> smembers(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.smembers(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.smembers(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.smembers(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long scard(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.scard(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.scard(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.scard(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Boolean sismember(String key, String member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sismember(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sismember(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.sismember(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String srandmember(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.srandmember(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.srandmember(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.srandmember(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<String> srandmember(String key, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.srandmember(key, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.srandmember(key, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.srandmember(key, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long strlen(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.strlen(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.strlen(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.strlen(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrange(String key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrange(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrange(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrange(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zrank(String key, String member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrank(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrank(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrank(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zrevrank(String key, String member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrank(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrank(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrank(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrevrange(String key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrange(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrange(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrange(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeWithScores(String key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeWithScores(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeWithScores(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeWithScores(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeWithScores(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeWithScores(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeWithScores(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zcard(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zcard(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zcard(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.zcard(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Double zscore(String key, String member) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zscore(key, member);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zscore(key, member);
            } catch (Exception e) {
                renew();
                return jedisCluster.zscore(key, member);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<String> sort(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sort(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sort(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.sort(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sort(key, sortingParameters);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sort(key, sortingParameters);
            } catch (Exception e) {
                renew();
                return jedisCluster.sort(key, sortingParameters);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zcount(String key, double min, double max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zcount(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zcount(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zcount(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zcount(String key, String min, String max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zcount(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zcount(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zcount(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScore(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScore(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByScoreWithScores(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zlexcount(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zlexcount(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zlexcount(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByLex(key, min, max);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByLex(key, min, max);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByLex(key, min, max);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrangeByLex(key, min, max, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrangeByLex(key, min, max, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrangeByLex(key, min, max, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByLex(key, max, min);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByLex(key, max, min);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByLex(key, max, min);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zrevrangeByLex(key, max, min, offset, count);
            } catch (Exception e) {
                renew();
                return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public String echo(String string) {
        int slot = JedisClusterCRC16.getSlot(string);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.echo(string);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.echo(string);
            } catch (Exception e) {
                renew();
                return jedisCluster.echo(string);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitcount(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitcount(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitcount(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitcount(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitcount(key, start, end);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitcount(key, start, end);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitcount(key, start, end);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitpos(String key, boolean value) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitpos(key, value);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitpos(key, value);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitpos(key, value);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitpos(byte[] key, boolean value) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return new JedisClusterCommand<Long>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
                @Override
                public Long execute(Jedis connection) {
                    return connection.bitpos(key, value);
                }
            }.runBinary(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitpos(key, value);
            } catch (Exception e) {
                renew();
                return new JedisClusterCommand<Long>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
                    @Override
                    public Long execute(Jedis connection) {
                        return connection.bitpos(key, value);
                    }
                }.runBinary(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitpos(key, value, params);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitpos(key, value, params);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitpos(key, value, params);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long bitpos(byte[] key, boolean value, BitPosParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return new JedisClusterCommand<Long>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
                @Override
                public Long execute(Jedis connection) {
                    return connection.bitpos(key, value, params);
                }
            }.runBinary(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitpos(key, value, params);
            } catch (Exception e) {
                renew();
                return new JedisClusterCommand<Long>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
                    @Override
                    public Long execute(Jedis connection) {
                        return connection.bitpos(key, value, params);
                    }
                }.runBinary(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hscan(key, cursor);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hscan(key, cursor);
            } catch (Exception e) {
                renew();
                return jedisCluster.hscan(key, cursor);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.hscan(key, cursor, params);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.hscan(key, cursor, params);
            } catch (Exception e) {
                renew();
                return jedisCluster.hscan(key, cursor, params);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sscan(key, cursor);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sscan(key, cursor);
            } catch (Exception e) {
                renew();
                return jedisCluster.sscan(key, cursor);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.sscan(key, cursor, params);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.sscan(key, cursor, params);
            } catch (Exception e) {
                renew();
                return jedisCluster.sscan(key, cursor, params);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zscan(key, cursor);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zscan(key, cursor);
            } catch (Exception e) {
                renew();
                return jedisCluster.zscan(key, cursor);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.zscan(key, cursor, params);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.zscan(key, cursor, params);
            } catch (Exception e) {
                renew();
                return jedisCluster.zscan(key, cursor, params);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public long pfcount(String key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.pfcount(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.pfcount(key);
            } catch (Exception e) {
                renew();
                return jedisCluster.pfcount(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geodist(key, member1, member2);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geodist(key, member1, member2);
            } catch (Exception e) {
                renew();
                return jedisCluster.geodist(key, member1, member2);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geodist(key, member1, member2, unit);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geodist(key, member1, member2, unit);
            } catch (Exception e) {
                renew();
                return jedisCluster.geodist(key, member1, member2, unit);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<String> geohash(String key, String... members) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geohash(key, members);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geohash(key, members);
            } catch (Exception e) {
                renew();
                return jedisCluster.geohash(key, members);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.geopos(key, members);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.geopos(key, members);
            } catch (Exception e) {
                renew();
                return jedisCluster.geopos(key, members);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadius(key, longitude, latitude, radius, unit);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadius(key, longitude, latitude, radius, unit);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadius(key, longitude, latitude, radius, unit, param);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadiusByMember(key, member, radius, unit);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadiusByMember(key, member, radius, unit);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadiusByMember(key, member, radius, unit);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.georadiusByMember(key, member, radius, unit, param);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.georadiusByMember(key, member, radius, unit, param);
            } catch (Exception e) {
                renew();
                return jedisCluster.georadiusByMember(key, member, radius, unit, param);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return jedisCluster.bitfield(key, arguments);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.bitfield(key, arguments);
            } catch (Exception e) {
                renew();
                return jedisCluster.bitfield(key, arguments);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    @Override
    public Long exists(byte[]... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        Map<String, List<byte[]>> map = new HashMap<>();
        for (byte[] key : keys) {
            int slot = JedisClusterCRC16.getSlot(key);
            String slaveNode = getSlaveNode(slot);
            List<byte[]> list = CamelliaMapUtils.computeIfAbsent(map, slaveNode, k -> new ArrayList<>());
            list.add(key);
        }
        AtomicLong result = new AtomicLong(0);
        List<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<String, List<byte[]>> entry : map.entrySet()) {
            final String slaveNode = entry.getKey();
            final List<byte[]> keyList = entry.getValue();
            if (slaveNode == null) {
                byte[][] array = keyList.toArray(new byte[0][0]);
                Long exists = camelliaJedisCluster.exists(array);
                result.addAndGet(exists);
            } else {
                Future<?> future = env.getConcurrentExec().submit(() -> {
                    ReadOnlyJedisPool pool = getPool(slaveNode);
                    Jedis jedis = null;
                    try {
                        jedis = pool.getResource();
                        Pipeline pipelined = jedis.pipelined();
                        List<Response<Boolean>> list = new ArrayList<>();
                        for (byte[] key : keyList) {
                            Response<Boolean> exists = pipelined.exists(key);
                            list.add(exists);
                        }
                        pipelined.sync();
                        for (Response<Boolean> response : list) {
                            if (response.get()) {
                                result.addAndGet(1);
                            }
                        }
                    } catch (Exception e) {
                        renew();
                        byte[][] array = keyList.toArray(new byte[0][0]);
                        Long exists = camelliaJedisCluster.exists(array);
                        result.addAndGet(exists);
                    } finally {
                        CloseUtil.closeQuietly(jedis);
                    }
                });
                futures.add(future);
            }
        }
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            handlerFutureException(e);
        }
        return result.get();
    }

    @Override
    public Map<byte[], byte[]> mget(byte[]... keys) {
        if (keys == null) return Collections.emptyMap();
        if (keys.length == 0) return Collections.emptyMap();
        Map<String, List<byte[]>> map = new HashMap<>();
        for (byte[] key : keys) {
            int slot = JedisClusterCRC16.getSlot(key);
            String slaveNode = getSlaveNode(slot);
            List<byte[]> list = CamelliaMapUtils.computeIfAbsent(map, slaveNode, k -> new ArrayList<>());
            list.add(key);
        }
        final Map<byte[], byte[]> resultMap = new HashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        for (Map.Entry<String, List<byte[]>> entry : map.entrySet()) {
            final String slaveNode = entry.getKey();
            final List<byte[]> keyList = entry.getValue();
            if (slaveNode == null) {
                byte[][] array = keyList.toArray(new byte[0][0]);
                Map<byte[], byte[]> mget = camelliaJedisCluster.mget(array);
                synchronized (resultMap) {
                    resultMap.putAll(mget);
                }
            } else {
                Future<?> future = env.getConcurrentExec().submit(() -> {
                    ReadOnlyJedisPool pool = getPool(slaveNode);
                    Jedis jedis = null;
                    try {
                        jedis = pool.getResource();
                        Pipeline pipelined = jedis.pipelined();
                        List<Response<byte[]>> list = new ArrayList<>();
                        for (byte[] key : keyList) {
                            Response<byte[]> get = pipelined.get(key);
                            list.add(get);
                        }
                        pipelined.sync();
                        for (int i=0; i<list.size(); i++) {
                            byte[] value = list.get(i).get();
                            byte[] key = keyList.get(i);
                            synchronized (resultMap) {
                                resultMap.put(key, value);
                            }
                        }
                    } catch (Exception e) {
                        renew();
                        byte[][] array = keyList.toArray(new byte[0][0]);
                        Map<byte[], byte[]> mget = camelliaJedisCluster.mget(array);
                        synchronized (resultMap) {
                            resultMap.putAll(mget);
                        }
                    } finally {
                        CloseUtil.closeQuietly(jedis);
                    }
                });
                futures.add(future);
            }
        }
        try {
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            handlerFutureException(e);
        }
        return resultMap;
    }

    @Override
    public Long exists(String... keys) {
        if (keys == null) return 0L;
        if (keys.length == 0) return 0L;
        List<byte[]> keysRaw = new ArrayList<>();
        for (String key : keys) {
            keysRaw.add(SafeEncoder.encode(key));
        }
        return exists(keysRaw.toArray(new byte[0][0]));
    }

    @Override
    public Map<String, String> mget(String... keys) {
        if (keys == null) return Collections.emptyMap();
        if (keys.length == 0) return Collections.emptyMap();
        byte[][] bytes = SafeEncoder.encodeMany(keys);
        Map<byte[], byte[]> map = mget(bytes);
        if (map == null) return Collections.emptyMap();
        Map<String, String> retMap = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                retMap.put(SafeEncoder.encode(entry.getKey()), null);
            } else {
                retMap.put(SafeEncoder.encode(entry.getKey()), SafeEncoder.encode(entry.getValue()));
            }
        }
        return retMap;
    }

    @Override
    public byte[] dump(String key) {
        return dump(SafeEncoder.encode(key));
    }

    @Override
    public byte[] dump(byte[] key) {
        int slot = JedisClusterCRC16.getSlot(key);
        ReadOnlyJedisPool pool = getPool(slot);
        if (pool == null) {
            return new JedisClusterCommand<byte[]>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
                @Override
                public byte[] execute(Jedis connection) {
                    return connection.dump(key);
                }
            }.runBinary(key);
        } else {
            Jedis jedis = pool.getResource();
            try {
                return jedis.dump(key);
            } catch (Exception e) {
                renew();
                return new JedisClusterCommand<byte[]>(jedisCluster.getConnectionHandler(), jedisCluster.getMaxAttempts()) {
                    @Override
                    public byte[] execute(Jedis connection) {
                        return connection.dump(key);
                    }
                }.runBinary(key);
            } finally {
                CloseUtil.closeQuietly(jedis);
            }
        }
    }

    private void handlerFutureException(Exception e) {
        if (e instanceof ExecutionException) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof CamelliaRedisException) {
                    throw (CamelliaRedisException) cause;
                } else {
                    throw new CamelliaRedisException(cause);
                }
            } else {
                throw new CamelliaRedisException(e);
            }
        } else {
            throw new CamelliaRedisException(e);
        }
    }
}
