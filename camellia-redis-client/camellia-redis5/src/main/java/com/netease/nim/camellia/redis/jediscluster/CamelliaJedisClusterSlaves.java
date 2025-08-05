package com.netease.nim.camellia.redis.jediscluster;

import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.base.resource.RedisClusterResource;
import com.netease.nim.camellia.redis.base.resource.RedisClusterSlavesResource;
import com.netease.nim.camellia.redis.base.utils.SafeEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2024/11/11
 */
public class CamelliaJedisClusterSlaves extends CamelliaJedisCluster {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaJedisClusterSlaves.class);

    private final ClusterCommandObjects clusterCommandObjects = new ClusterCommandObjects();

    private final JedisClusterWrapper jedisCluster;
    private final JedisClusterWrapper jedisClusterSlaves;

    private final RedisClusterSlavesResource resource;
    private final CamelliaRedisEnv env;

    public CamelliaJedisClusterSlaves(RedisClusterSlavesResource resource, CamelliaRedisEnv env) {
        super(new RedisClusterResource(resource.getNodes(), resource.getUserName(), resource.getPassword()), env);
        RedisClusterResource redisClusterResource = new RedisClusterResource(resource.getNodes(), resource.getUserName(), resource.getPassword());
        this.jedisClusterSlaves = env.getJedisClusterFactory().getJedisCluster(redisClusterResource);
        this.jedisCluster = env.getJedisClusterFactory().getJedisCluster(resource);
        this.resource = resource;
        this.env = env;
    }

    @Override
    public Jedis getJedis(byte[] key) {
        NodeType nodeType = selectNodeType();
        if (nodeType == NodeType.slave) {
            return jedisClusterSlaves.getSlaveJedis(key);
        } else {
            return jedisCluster.getJedis(key);
        }
    }

    @Override
    public List<Jedis> getJedisList() {
        List<Jedis> list = new ArrayList<>();
        if (resource.isWithMaster()) {
            List<Jedis> jedisList = jedisCluster.getJedisList();
            list.addAll(jedisList);
        }
        List<Jedis> jedisList = jedisClusterSlaves.getSlaveJedisList();
        list.addAll(jedisList);
        return list;
    }

    private static enum NodeType {
        master,
        slave,
        ;
    }

    private NodeType selectNodeType() {
        if (resource.isWithMaster()) {
            boolean slave = ThreadLocalRandom.current().nextBoolean();
            if (slave) {
                return NodeType.slave;
            } else {
                return NodeType.master;
            }
        }
        return NodeType.slave;
    }

    @Override
    public byte[] get(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.get(key));
            } catch (Exception e) {
                return jedisCluster.get(key);
            }
        } else {
            return jedisCluster.get(key);
        }
    }

    @Override
    public Boolean exists(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.exists(key));
            } catch (Exception e) {
                return jedisCluster.exists(key);
            }
        } else {
            return jedisCluster.exists(key);
        }
    }

    @Override
    public String type(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.type(key));
            } catch (Exception e) {
                return jedisCluster.type(key);
            }
        } else {
            return jedisCluster.type(key);
        }
    }

    @Override
    public Long ttl(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.ttl(key));
            } catch (Exception e) {
                return jedisCluster.ttl(key);
            }
        } else {
            return jedisCluster.ttl(key);
        }
    }

    @Override
    public Long pttl(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.pttl(key));
            } catch (Exception e) {
                return jedisCluster.pttl(key);
            }
        } else {
            return jedisCluster.pttl(key);
        }
    }

    @Override
    public Boolean getbit(byte[] key, long offset) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.getbit(key, offset));
            } catch (Exception e) {
                return jedisCluster.getbit(key, offset);
            }
        } else {
            return jedisCluster.getbit(key, offset);
        }
    }

    @Override
    public byte[] getrange(byte[] key, long startOffset, long endOffset) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.getrange(key, startOffset, endOffset));
            } catch (Exception e) {
                return jedisCluster.getrange(key, startOffset, endOffset);
            }
        } else {
            return jedisCluster.getrange(key, startOffset, endOffset);
        }
    }

    @Override
    public byte[] hget(byte[] key, byte[] field) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hget(key, field));
            } catch (Exception e) {
                return jedisCluster.hget(key, field);
            }
        } else {
            return jedisCluster.hget(key, field);
        }
    }

    @Override
    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hmget(key, fields));
            } catch (Exception e) {
                return jedisCluster.hmget(key, fields);
            }
        } else {
            return jedisCluster.hmget(key, fields);
        }
    }

    @Override
    public Boolean hexists(byte[] key, byte[] field) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hexists(key, field));
            } catch (Exception e) {
                return jedisCluster.hexists(key, field);
            }
        } else {
            return jedisCluster.hexists(key, field);
        }
    }

    @Override
    public Long hlen(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hlen(key));
            } catch (Exception e) {
                return jedisCluster.hlen(key);
            }
        } else {
            return jedisCluster.hlen(key);
        }
    }

    @Override
    public Set<byte[]> hkeys(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hkeys(key));
            } catch (Exception e) {
                return jedisCluster.hkeys(key);
            }
        } else {
            return jedisCluster.hkeys(key);
        }
    }

    @Override
    public List<byte[]> hvals(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hvals(key));
        } else {
            return jedisCluster.hvals(key);
        }
    }

    @Override
    public Map<byte[], byte[]> hgetAll(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hgetAll(key));
            } catch (Exception e) {
                return jedisCluster.hgetAll(key);
            }
        } else {
            return jedisCluster.hgetAll(key);
        }
    }

    @Override
    public Long llen(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.llen(key));
            } catch (Exception e) {
                return jedisCluster.llen(key);
            }
        } else {
            return jedisCluster.llen(key);
        }
    }

    @Override
    public List<byte[]> lrange(byte[] key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.lrange(key, start, end));
            } catch (Exception e) {
                return jedisCluster.lrange(key, start, end);
            }
        } else {
            return jedisCluster.lrange(key, start, end);
        }
    }

    @Override
    public String ltrim(byte[] key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.ltrim(key, start, end));
            } catch (Exception e) {
                return jedisCluster.ltrim(key, start, end);
            }
        } else {
            return jedisCluster.ltrim(key, start, end);
        }
    }

    @Override
    public byte[] lindex(byte[] key, long index) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.lindex(key, index));
            } catch (Exception e) {
                return jedisCluster.lindex(key, index);
            }
        } else {
            return jedisCluster.lindex(key, index);
        }
    }

    @Override
    public Set<byte[]> smembers(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.smembers(key));
            } catch (Exception e) {
                return jedisCluster.smembers(key);
            }
        } else {
            return jedisCluster.smembers(key);
        }
    }

    @Override
    public Long scard(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.scard(key));
            } catch (Exception e) {
                return jedisCluster.scard(key);
            }
        } else {
            return jedisCluster.scard(key);
        }
    }

    @Override
    public Boolean sismember(byte[] key, byte[] member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sismember(key, member));
            } catch (Exception e) {
                return jedisCluster.sismember(key, member);
            }
        } else {
            return jedisCluster.sismember(key, member);
        }
    }

    @Override
    public byte[] srandmember(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.srandmember(key));
            } catch (Exception e) {
                return jedisCluster.srandmember(key);
            }
        } else {
            return jedisCluster.srandmember(key);
        }
    }

    @Override
    public List<byte[]> srandmember(byte[] key, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.srandmember(key, count));
            } catch (Exception e) {
                return jedisCluster.srandmember(key, count);
            }
        } else {
            return jedisCluster.srandmember(key, count);
        }
    }

    @Override
    public Long strlen(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.strlen(key));
            } catch (Exception e) {
                return jedisCluster.strlen(key);
            }
        } else {
            return jedisCluster.strlen(key);
        }
    }

    @Override
    public List<byte[]> zrange(byte[] key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrange(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrange(key, start, end);
            }
        } else {
            return jedisCluster.zrange(key, start, end);
        }
    }

    @Override
    public Long zrank(byte[] key, byte[] member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrank(key, member));
            } catch (Exception e) {
                return jedisCluster.zrank(key, member);
            }
        } else {
            return jedisCluster.zrank(key, member);
        }
    }

    @Override
    public Long zrevrank(byte[] key, byte[] member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrank(key, member));
            } catch (Exception e) {
                return jedisCluster.zrevrank(key, member);
            }
        } else {
            return jedisCluster.zrevrank(key, member);
        }
    }

    @Override
    public List<byte[]> zrevrange(byte[] key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrange(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrevrange(key, start, end);
            }
        } else {
            return jedisCluster.zrevrange(key, start, end);
        }
    }

    @Override
    public List<Tuple> zrangeWithScores(byte[] key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeWithScores(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrangeWithScores(key, start, end);
            }
        } else {
            return jedisCluster.zrangeWithScores(key, start, end);
        }
    }

    @Override
    public List<Tuple> zrevrangeWithScores(byte[] key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeWithScores(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrevrangeWithScores(key, start, end);
            }
        } else {
            return jedisCluster.zrevrangeWithScores(key, start, end);
        }
    }

    @Override
    public Long zcard(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zcard(key));
            } catch (Exception e) {
                return jedisCluster.zcard(key);
            }
        } else {
            return jedisCluster.zcard(key);
        }
    }

    @Override
    public Double zscore(byte[] key, byte[] member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zscore(key, member));
            } catch (Exception e) {
                return jedisCluster.zscore(key, member);
            }
        } else {
            return jedisCluster.zscore(key, member);
        }
    }

    @Override
    public List<byte[]> sort(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sort(key));
            } catch (Exception e) {
                return jedisCluster.sort(key);
            }
        } else {
            return jedisCluster.sort(key);
        }
    }

    @Override
    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sort(key, sortingParameters));
            } catch (Exception e) {
                return jedisCluster.sort(key, sortingParameters);
            }
        } else {
            return jedisCluster.sort(key, sortingParameters);
        }
    }

    @Override
    public Long zcount(byte[] key, double min, double max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zcount(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zcount(key, min, max);
            }
        } else {
            return jedisCluster.zcount(key, min, max);
        }
    }

    @Override
    public Long zcount(byte[] key, byte[] min, byte[] max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zcount(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zcount(key, min, max);
            }
        } else {
            return jedisCluster.zcount(key, min, max);
        }
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, double min, double max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max);
        }
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max);
        }
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, double max, double min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min);
        }
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        }
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min);
        }
    }

    @Override
    public List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        }
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        }
    }

    @Override
    public List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        }
    }

    @Override
    public Long zlexcount(byte[] key, byte[] min, byte[] max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zlexcount(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zlexcount(key, min, max);
            }
        } else {
            return jedisCluster.zlexcount(key, min, max);
        }
    }

    @Override
    public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByLex(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByLex(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByLex(key, min, max);
        }
    }

    @Override
    public List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByLex(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByLex(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByLex(key, min, max, offset, count);
        }
    }

    @Override
    public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByLex(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByLex(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByLex(key, max, min);
        }
    }

    @Override
    public List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByLex(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
        }
    }

    @Override
    public Long bitcount(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitcount(key));
            } catch (Exception e) {
                return jedisCluster.bitcount(key);
            }
        } else {
            return jedisCluster.bitcount(key);
        }
    }

    @Override
    public Long bitcount(byte[] key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitcount(key, start, end));
            } catch (Exception e) {
                return jedisCluster.bitcount(key, start, end);
            }
        } else {
            return jedisCluster.bitcount(key, start, end);
        }
    }


    @Override
    public long pfcount(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.pfcount(key));
            } catch (Exception e) {
                return jedisCluster.pfcount(key);
            }
        } else {
            return jedisCluster.pfcount(key);
        }
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geodist(key, member1, member2));
            } catch (Exception e) {
                return jedisCluster.geodist(key, member1, member2);
            }
        } else {
            return jedisCluster.geodist(key, member1, member2);
        }
    }

    @Override
    public Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geodist(key, member1, member2, unit));
            } catch (Exception e) {
                return jedisCluster.geodist(key, member1, member2, unit);
            }
        } else {
            return jedisCluster.geodist(key, member1, member2, unit);
        }
    }

    @Override
    public List<byte[]> geohash(byte[] key, byte[]... members) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geohash(key, members));
            } catch (Exception e) {
                return jedisCluster.geohash(key, members);
            }
        } else {
            return jedisCluster.geohash(key, members);
        }
    }

    @Override
    public List<GeoCoordinate> geopos(byte[] key, byte[]... members) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geopos(key, members));
            } catch (Exception e) {
                return jedisCluster.geopos(key, members);
            }
        } else {
            return jedisCluster.geopos(key, members);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadius(key, longitude, latitude, radius, unit));
            } catch (Exception e) {
                return jedisCluster.georadius(key, longitude, latitude, radius, unit);
            }
        } else {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadius(key, longitude, latitude, radius, unit, param));
            } catch (Exception e) {
                return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
            }
        } else {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadiusByMember(key, member, radius, unit));
            } catch (Exception e) {
                return jedisCluster.georadiusByMember(key, member, radius, unit);
            }
        } else {
            return jedisCluster.georadiusByMember(key, member, radius, unit);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit, GeoRadiusParam param) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadiusByMember(key, member, radius, unit, param));
            } catch (Exception e) {
                return jedisCluster.georadiusByMember(key, member, radius, unit, param);
            }
        } else {
            return jedisCluster.georadiusByMember(key, member, radius, unit, param);
        }
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hscan(key, cursor, new ScanParams()));
            } catch (Exception e) {
                return jedisCluster.hscan(key, cursor);
            }
        } else {
            return jedisCluster.hscan(key, cursor);
        }
    }

    @Override
    public ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hscan(key, cursor, params));
            } catch (Exception e) {
                return jedisCluster.hscan(key, cursor, params);
            }
        } else {
            return jedisCluster.hscan(key, cursor, params);
        }
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sscan(key, cursor, new ScanParams()));
            } catch (Exception e) {
                return jedisCluster.sscan(key, cursor);
            }
        } else {
            return jedisCluster.sscan(key, cursor);
        }
    }

    @Override
    public ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sscan(key, cursor, params));
            } catch (Exception e) {
                return jedisCluster.sscan(key, cursor, params);
            }
        } else {
            return jedisCluster.sscan(key, cursor, params);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zscan(key, cursor, new ScanParams()));
            } catch (Exception e) {
                return jedisCluster.zscan(key, cursor);
            }
        } else {
            return jedisCluster.zscan(key, cursor);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zscan(key, cursor, params));
            } catch (Exception e) {
                return jedisCluster.zscan(key, cursor, params);
            }
        } else {
            return jedisCluster.zscan(key, cursor, params);
        }
    }

    @Override
    public List<Long> bitfield(byte[] key, byte[]... arguments) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitfield(key, arguments));
            } catch (Exception e) {
                return jedisCluster.bitfield(key, arguments);
            }
        } else {
            return jedisCluster.bitfield(key, arguments);
        }
    }

    @Override
    public String get(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.get(key));
            } catch (Exception e) {
                return jedisCluster.get(key);
            }
        } else {
            return jedisCluster.get(key);
        }
    }

    @Override
    public Boolean exists(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.exists(key));
            } catch (Exception e) {
                return jedisCluster.exists(key);
            }
        } else {
            return jedisCluster.exists(key);
        }
    }

    @Override
    public String type(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.type(key));
            } catch (Exception e) {
                return jedisCluster.type(key);
            }
        } else {
            return jedisCluster.type(key);
        }
    }

    @Override
    public Long ttl(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.ttl(key));
            } catch (Exception e) {
                return jedisCluster.ttl(key);
            }
        } else {
            return jedisCluster.ttl(key);
        }
    }

    @Override
    public Long pttl(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.pttl(key));
            } catch (Exception e) {
                return jedisCluster.pttl(key);
            }
        } else {
            return jedisCluster.pttl(key);
        }
    }

    @Override
    public Boolean getbit(String key, long offset) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.getbit(key, offset));
            } catch (Exception e) {
                return jedisCluster.getbit(key, offset);
            }
        } else {
            return jedisCluster.getbit(key, offset);
        }
    }

    @Override
    public String getrange(String key, long startOffset, long endOffset) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.getrange(key, startOffset, endOffset));
            } catch (Exception e) {
                return jedisCluster.getrange(key, startOffset, endOffset);
            }
        } else {
            return jedisCluster.getrange(key, startOffset, endOffset);
        }
    }

    @Override
    public String substr(String key, int start, int end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.getrange(key, start, end));
            } catch (Exception e) {
                return jedisCluster.getrange(key, start, end);
            }
        } else {
            return jedisCluster.getrange(key, start, end);
        }
    }

    @Override
    public String hget(String key, String field) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hget(key, field));
            } catch (Exception e) {
                return jedisCluster.hget(key, field);
            }
        } else {
            return jedisCluster.hget(key, field);
        }
    }

    @Override
    public List<String> hmget(String key, String... fields) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hmget(key, fields));
            } catch (Exception e) {
                return jedisCluster.hmget(key, fields);
            }
        } else {
            return jedisCluster.hmget(key, fields);
        }
    }

    @Override
    public Boolean hexists(String key, String field) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hexists(key, field));
            } catch (Exception e) {
                return jedisCluster.hexists(key, field);
            }
        } else {
            return jedisCluster.hexists(key, field);
        }
    }

    @Override
    public Long hlen(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hlen(key));
            } catch (Exception e) {
                return jedisCluster.hlen(key);
            }
        } else {
            return jedisCluster.hlen(key);
        }
    }

    @Override
    public Set<String> hkeys(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hkeys(key));
            } catch (Exception e) {
                return jedisCluster.hkeys(key);
            }
        } else {
            return jedisCluster.hkeys(key);
        }
    }

    @Override
    public List<String> hvals(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hvals(key));
            } catch (Exception e) {
                return jedisCluster.hvals(key);
            }
        } else {
            return jedisCluster.hvals(key);
        }
    }

    @Override
    public Map<String, String> hgetAll(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hgetAll(key));
            } catch (Exception e) {
                return jedisCluster.hgetAll(key);
            }
        } else {
            return jedisCluster.hgetAll(key);
        }
    }

    @Override
    public Long llen(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.llen(key));
            } catch (Exception e) {
                return jedisCluster.llen(key);
            }
        } else {
            return jedisCluster.llen(key);
        }
    }

    @Override
    public List<String> lrange(String key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.lrange(key, start, end));
            } catch (Exception e) {
                return jedisCluster.lrange(key, start, end);
            }
        } else {
            return jedisCluster.lrange(key, start, end);
        }
    }

    @Override
    public String ltrim(String key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.ltrim(key, start, end));
            } catch (Exception e) {
                return jedisCluster.ltrim(key, start, end);
            }
        } else {
            return jedisCluster.ltrim(key, start, end);
        }
    }

    @Override
    public String lindex(String key, long index) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.lindex(key, index));
            } catch (Exception e) {
                return jedisCluster.lindex(key, index);
            }
        } else {
            return jedisCluster.lindex(key, index);
        }
    }

    @Override
    public Set<String> smembers(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.smembers(key));
            } catch (Exception e) {
                return jedisCluster.smembers(key);
            }
        } else {
            return jedisCluster.smembers(key);
        }
    }

    @Override
    public Long scard(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.scard(key));
            } catch (Exception e) {
                return jedisCluster.scard(key);
            }
        } else {
            return jedisCluster.scard(key);
        }
    }

    @Override
    public Boolean sismember(String key, String member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sismember(key, member));
            } catch (Exception e) {
                return jedisCluster.sismember(key, member);
            }
        } else {
            return jedisCluster.sismember(key, member);
        }
    }

    @Override
    public String srandmember(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.srandmember(key));
            } catch (Exception e) {
                return jedisCluster.srandmember(key);
            }
        } else {
            return jedisCluster.srandmember(key);
        }
    }

    @Override
    public List<String> srandmember(String key, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.srandmember(key, count));
            } catch (Exception e) {
                return jedisCluster.srandmember(key, count);
            }
        } else {
            return jedisCluster.srandmember(key, count);
        }
    }

    @Override
    public Long strlen(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.strlen(key));
            } catch (Exception e) {
                return jedisCluster.strlen(key);
            }
        } else {
            return jedisCluster.strlen(key);
        }
    }

    @Override
    public List<String> zrange(String key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrange(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrange(key, start, end);
            }
        } else {
            return jedisCluster.zrange(key, start, end);
        }
    }

    @Override
    public Long zrank(String key, String member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrank(key, member));
            } catch (Exception e) {
                return jedisCluster.zrank(key, member);
            }
        } else {
            return jedisCluster.zrank(key, member);
        }
    }

    @Override
    public Long zrevrank(String key, String member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrank(key, member));
            } catch (Exception e) {
                return jedisCluster.zrevrank(key, member);
            }
        } else {
            return jedisCluster.zrevrank(key, member);
        }
    }

    @Override
    public List<String> zrevrange(String key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrange(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrevrange(key, start, end);
            }
        } else {
            return jedisCluster.zrevrange(key, start, end);
        }
    }

    @Override
    public List<Tuple> zrangeWithScores(String key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeWithScores(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrangeWithScores(key, start, end);
            }
        } else {
            return jedisCluster.zrangeWithScores(key, start, end);
        }
    }

    @Override
    public List<Tuple> zrevrangeWithScores(String key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeWithScores(key, start, end));
            } catch (Exception e) {
                return jedisCluster.zrevrangeWithScores(key, start, end);
            }
        } else {
            return jedisCluster.zrevrangeWithScores(key, start, end);
        }
    }

    @Override
    public Long zcard(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zcard(key));
            } catch (Exception e) {
                return jedisCluster.zcard(key);
            }
        } else {
            return jedisCluster.zcard(key);
        }
    }

    @Override
    public Double zscore(String key, String member) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zscore(key, member));
            } catch (Exception e) {
                return jedisCluster.zscore(key, member);
            }
        } else {
            return jedisCluster.zscore(key, member);
        }
    }

    @Override
    public List<String> sort(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sort(key));
            } catch (Exception e) {
                return jedisCluster.sort(key);
            }
        } else {
            return jedisCluster.sort(key);
        }
    }

    @Override
    public List<String> sort(String key, SortingParams sortingParameters) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sort(key, sortingParameters));
            } catch (Exception e) {
                return jedisCluster.sort(key, sortingParameters);
            }
        } else {
            return jedisCluster.sort(key, sortingParameters);
        }
    }

    @Override
    public Long zcount(String key, double min, double max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zcount(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zcount(key, min, max);
            }
        } else {
            return jedisCluster.zcount(key, min, max);
        }
    }

    @Override
    public Long zcount(String key, String min, String max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zcount(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zcount(key, min, max);
            }
        } else {
            return jedisCluster.zcount(key, min, max);
        }
    }

    @Override
    public List<String> zrangeByScore(String key, double min, double max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max);
        }
    }

    @Override
    public List<String> zrangeByScore(String key, String min, String max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max);
        }
    }

    @Override
    public List<String> zrevrangeByScore(String key, double max, double min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min);
        }
    }

    @Override
    public List<String> zrangeByScore(String key, double min, double max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        }
    }

    @Override
    public List<String> zrevrangeByScore(String key, String max, String min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min);
        }
    }

    @Override
    public List<String> zrangeByScore(String key, String min, String max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScore(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScore(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScore(key, min, max, offset, count);
        }
    }

    @Override
    public List<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        }
    }

    @Override
    public List<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScore(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScore(key, max, min, offset, count);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min);
        }
    }

    @Override
    public List<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByScoreWithScores(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByScoreWithScores(key, min, max, offset, count);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        }
    }

    @Override
    public List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByScoreWithScores(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByScoreWithScores(key, max, min, offset, count);
        }
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zlexcount(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zlexcount(key, min, max);
            }
        } else {
            return jedisCluster.zlexcount(key, min, max);
        }
    }

    @Override
    public List<String> zrangeByLex(String key, String min, String max) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByLex(key, min, max));
            } catch (Exception e) {
                return jedisCluster.zrangeByLex(key, min, max);
            }
        } else {
            return jedisCluster.zrangeByLex(key, min, max);
        }
    }

    @Override
    public List<String> zrangeByLex(String key, String min, String max, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrangeByLex(key, min, max, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrangeByLex(key, min, max, offset, count);
            }
        } else {
            return jedisCluster.zrangeByLex(key, min, max, offset, count);
        }
    }

    @Override
    public List<String> zrevrangeByLex(String key, String max, String min) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByLex(key, max, min));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByLex(key, max, min);
            }
        } else {
            return jedisCluster.zrevrangeByLex(key, max, min);
        }
    }

    @Override
    public List<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zrevrangeByLex(key, max, min, offset, count));
            } catch (Exception e) {
                return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
            }
        } else {
            return jedisCluster.zrevrangeByLex(key, max, min, offset, count);
        }
    }

    @Override
    public Long bitcount(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitcount(key));
            } catch (Exception e) {
                return jedisCluster.bitcount(key);
            }
        } else {
            return jedisCluster.bitcount(key);
        }
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitcount(key, start, end));
            } catch (Exception e) {
                return jedisCluster.bitcount(key, start, end);
            }
        } else {
            return jedisCluster.bitcount(key, start, end);
        }
    }

    @Override
    public Long bitpos(String key, boolean value) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitpos(key, value));
            } catch (Exception e) {
                return jedisCluster.bitpos(key, value);
            }
        } else {
            return jedisCluster.bitpos(key, value);
        }
    }

    @Override
    public Long bitpos(byte[] key, boolean value) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitpos(key, value));
            } catch (Exception e) {
                return jedisCluster.bitpos(key, value);
            }
        } else {
            return jedisCluster.bitpos(key, value);
        }
    }

    @Override
    public Long bitpos(String key, boolean value, BitPosParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitpos(key, value, params));
            } catch (Exception e) {
                return jedisCluster.bitpos(key, value, params);
            }
        } else {
            return jedisCluster.bitpos(key, value, params);
        }
    }

    @Override
    public Long bitpos(byte[] key, boolean value, BitPosParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitpos(key, value, params));
            } catch (Exception e) {
                return jedisCluster.bitpos(key, value, params);
            }
        } else {
            return jedisCluster.bitpos(key, value, params);
        }
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hscan(key, cursor, new ScanParams()));
            } catch (Exception e) {
                return jedisCluster.hscan(key, cursor);
            }
        } else {
            return jedisCluster.hscan(key, cursor);
        }
    }

    @Override
    public ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.hscan(key, cursor, params));
            } catch (Exception e) {
                return jedisCluster.hscan(key, cursor, params);
            }
        } else {
            return jedisCluster.hscan(key, cursor, params);
        }
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sscan(key, cursor, new ScanParams()));
            } catch (Exception e) {
                return jedisCluster.sscan(key, cursor);
            }
        } else {
            return jedisCluster.sscan(key, cursor);
        }
    }

    @Override
    public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.sscan(key, cursor, params));
            } catch (Exception e) {
                return jedisCluster.sscan(key, cursor, params);
            }
        } else {
            return jedisCluster.sscan(key, cursor, params);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zscan(key, cursor, new ScanParams()));
            } catch (Exception e) {
                return jedisCluster.zscan(key, cursor);
            }
        } else {
            return jedisCluster.zscan(key, cursor);
        }
    }

    @Override
    public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.zscan(key, cursor, params));
            } catch (Exception e) {
                return jedisCluster.zscan(key, cursor, params);
            }
        } else {
            return jedisCluster.zscan(key, cursor, params);
        }
    }

    @Override
    public long pfcount(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.pfcount(key));
            } catch (Exception e) {
                return jedisCluster.pfcount(key);
            }
        } else {
            return jedisCluster.pfcount(key);
        }
    }

    @Override
    public Double geodist(String key, String member1, String member2) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geodist(key, member1, member2));
            } catch (Exception e) {
                return jedisCluster.geodist(key, member1, member2);
            }
        } else {
            return jedisCluster.geodist(key, member1, member2);
        }
    }

    @Override
    public Double geodist(String key, String member1, String member2, GeoUnit unit) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geodist(key, member1, member2, unit));
            } catch (Exception e) {
                return jedisCluster.geodist(key, member1, member2, unit);
            }
        } else {
            return jedisCluster.geodist(key, member1, member2, unit);
        }
    }

    @Override
    public List<String> geohash(String key, String... members) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geohash(key, members));
            } catch (Exception e) {
                return jedisCluster.geohash(key, members);
            }
        } else {
            return jedisCluster.geohash(key, members);
        }
    }

    @Override
    public List<GeoCoordinate> geopos(String key, String... members) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.geopos(key, members));
            } catch (Exception e) {
                return jedisCluster.geopos(key, members);
            }
        } else {
            return jedisCluster.geopos(key, members);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadius(key, longitude, latitude, radius, unit));
            } catch (Exception e) {
                return jedisCluster.georadius(key, longitude, latitude, radius, unit);
            }
        } else {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadius(key, longitude, latitude, radius, unit, param));
            } catch (Exception e) {
                return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
            }
        } else {
            return jedisCluster.georadius(key, longitude, latitude, radius, unit, param);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadiusByMember(key, member, radius, unit));
            } catch (Exception e) {
                return jedisCluster.georadiusByMember(key, member, radius, unit);
            }
        } else {
            return jedisCluster.georadiusByMember(key, member, radius, unit);
        }
    }

    @Override
    public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit, GeoRadiusParam param) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.georadiusByMember(key, member, radius, unit, param));
            } catch (Exception e) {
                return jedisCluster.georadiusByMember(key, member, radius, unit, param);
            }
        } else {
            return jedisCluster.georadiusByMember(key, member, radius, unit, param);
        }
    }

    @Override
    public List<Long> bitfield(String key, String... arguments) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.bitfield(key, arguments));
            } catch (Exception e) {
                return jedisCluster.bitfield(key, arguments);
            }
        } else {
            return jedisCluster.bitfield(key, arguments);
        }
    }

    @Override
    public byte[] dump(String key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.dump(key));
            } catch (Exception e) {
                return jedisCluster.dump(key);
            }
        } else {
            return jedisCluster.dump(key);
        }
    }

    @Override
    public byte[] dump(byte[] key) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.dump(key));
            } catch (Exception e) {
                return jedisCluster.dump(key);
            }
        } else {
            return jedisCluster.dump(key);
        }
    }

    @Override
    public Long exists(byte[]... keys) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                List<Future<Long>> list = new ArrayList<>();
                for (byte[] key : keys) {
                    Future<Long> future = env.getConcurrentExec().submit(() -> {
                        try {
                            Boolean result = jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.exists(key));
                            if (result != null && result) {
                                return 1L;
                            } else {
                                return 0L;
                            }
                        } catch (Exception e) {
                            logger.error("execute exists(byte[] key) on slave error, fallback to master, ex = {}", e.toString());
                            boolean exists = jedisCluster.exists(key);
                            if (exists) {
                                return 1L;
                            } else {
                                return 0L;
                            }
                        }
                    });
                    list.add(future);
                }
                long result = 0;
                for (Future<Long> future : list) {
                    result += future.get();
                }
                return result;
            } catch (Exception e) {
                logger.error("execute exists(byte[]... keys) on slave error, fallback to master, ex = {}", e.toString());
                return super.exists(keys);
            }
        } else {
            return super.exists(keys);
        }
    }

    @Override
    public Map<byte[], byte[]> mget(byte[]... keys) {
        NodeType type = selectNodeType();
        if (type == NodeType.slave) {
            try {
                Map<byte[], Future<byte[]>> map = new HashMap<>();
                for (byte[] key : keys) {
                    Future<byte[]> future = env.getConcurrentExec().submit(() -> {
                        try {
                            return jedisClusterSlaves.executeCommandToReplica(clusterCommandObjects.get(key));
                        } catch (Exception e) {
                            logger.error("execute get(byte[] key) on slave error, fallback to master, ex = {}", e.toString());
                            return jedisCluster.get(key);
                        }
                    });
                    map.put(key, future);
                }
                Map<byte[], byte[]> result = new HashMap<>();
                for (Map.Entry<byte[], Future<byte[]>> entry : map.entrySet()) {
                    result.put(entry.getKey(), entry.getValue().get());
                }
                return result;
            } catch (Exception e) {
                logger.error("execute mget(byte[]... keys) on slave error, fallback to master, ex = {}", e.toString());
                return super.mget(keys);
            }
        } else {
            return super.mget(keys);
        }
    }

    @Override
    public Long exists(String... keys) {
        byte[][] byteKeys = new byte[keys.length][];
        for (int i=0; i<keys.length; i++) {
            byteKeys[i] = SafeEncoder.encode(keys[i]);
        }
        return exists(byteKeys);
    }

    @Override
    public Map<String, String> mget(String... keys) {
        byte[][] byteKeys = new byte[keys.length][];
        for (int i=0; i<keys.length; i++) {
            byteKeys[i] = SafeEncoder.encode(keys[i]);
        }
        Map<byte[], byte[]> mget = mget(byteKeys);
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<byte[], byte[]> entry : mget.entrySet()) {
            byte[] key = entry.getKey();
            byte[] value = entry.getValue();
            if (value == null) {
                result.put(SafeEncoder.encode(key), null);
            } else {
                result.put(SafeEncoder.encode(key), SafeEncoder.encode(value));
            }
        }
        return result;
    }

}
