package com.netease.nim.camellia.redis.springboot;

import com.netease.nim.camellia.core.api.ReloadableLocalFileCamelliaApi;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.tools.utils.FileUtil;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;
import com.netease.nim.camellia.redis.CamelliaRedisEnv;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.jedis.JedisPoolFactory;
import com.netease.nim.camellia.redis.jediscluster.JedisClusterFactory;
import com.netease.nim.camellia.redis.proxy.*;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.ProxyJedisPoolConfig;
import com.netease.nim.camellia.redis.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.resource.RedisTemplateResourceTableUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Map;


/**
 *
 * Created by caojiajun on 2020/3/31.
 */
@Configuration
@EnableConfigurationProperties({CamelliaRedisProperties.class})
public class CamelliaRedisConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaRedisConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(value = {ProxyEnv.class})
    public ProxyEnv proxyEnv() {
        return ProxyEnv.defaultProxyEnv();
    }

    @Autowired(required = false)
    private CamelliaRedisProxyFactory proxyFactory;

    @Autowired(required = false)
    private ProxyJedisPoolConfig proxyJedisPoolConfig;

    @Bean
    public CamelliaRedisTemplate camelliaRedisTemplate(CamelliaRedisProperties properties) {
        if (proxyFactory != null) {
            CamelliaRedisProxyContext.register(proxyFactory);
            logger.info("CamelliaRedisProxyFactory register success, type = {}", proxyFactory.getClass().getName());
        }
        if (proxyJedisPoolConfig != null) {
            proxyJedisPoolConfig.setJedisPoolConfig(jedisPoolConfig(properties.getRedisConf()));
            proxyJedisPoolConfig.setTimeout(properties.getRedisConf().getJedis().getTimeout());
        }
        CamelliaRedisProperties.Type type = properties.getType();
        CamelliaRedisProperties.RedisConf redisConf = properties.getRedisConf();
        CamelliaRedisEnv redisEnv = camelliaRedisEnv(redisConf);
        if (type == CamelliaRedisProperties.Type.LOCAL) {
            CamelliaRedisProperties.Local local = properties.getLocal();
            CamelliaRedisProperties.Local.Type localType = local.getType();
            if (localType == CamelliaRedisProperties.Local.Type.SIMPLE) {
                String url = local.getResource();
                Resource resource = RedisResourceUtil.parseResourceByUrl(new Resource(url));
                return new CamelliaRedisTemplate(redisEnv, ResourceTableUtil.simpleTable(resource));
            } else if (localType == CamelliaRedisProperties.Local.Type.COMPLEX) {
                String jsonFile = local.getJsonFile();
                if (jsonFile == null) {
                    throw new IllegalArgumentException("missing jsonFile");
                }
                String fileContent = FileUtil.readFileByName(jsonFile);
                if (fileContent == null) {
                    throw new IllegalArgumentException(jsonFile + " read fail");
                }
                ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(fileContent);
                RedisResourceUtil.checkResourceTable(resourceTable);
                if (!local.isDynamic()) {
                    return new CamelliaRedisTemplate(redisEnv, resourceTable);
                }
                String filePath = FileUtil.getAbsoluteFilePath(jsonFile);
                if (filePath == null) {
                    return new CamelliaRedisTemplate(redisEnv, resourceTable);
                }
                ReloadableLocalFileCamelliaApi camelliaApi = new ReloadableLocalFileCamelliaApi(filePath, RedisResourceUtil.RedisResourceTableChecker);
                long checkIntervalMillis = local.getCheckIntervalMillis();
                if (checkIntervalMillis <= 0) {
                    throw new IllegalArgumentException("checkIntervalMillis <= 0");
                }
                return new CamelliaRedisTemplate(redisEnv, camelliaApi, checkIntervalMillis);
            } else {
                throw new UnsupportedOperationException("only support simple/complex");
            }
        } else if (type == CamelliaRedisProperties.Type.REMOTE) {
            CamelliaRedisProperties.Remote remote = properties.getRemote();
            String url = remote.getUrl();
            long bid = remote.getBid();
            String bgroup = remote.getBgroup();
            boolean monitor = remote.isMonitor();
            int connectTimeoutMillis = remote.getConnectTimeoutMillis();
            int readTimeoutMillis = remote.getReadTimeoutMillis();
            long checkIntervalMillis = remote.getCheckIntervalMillis();
            Map<String, String> headerMap = remote.getHeaderMap();
            return new CamelliaRedisTemplate(redisEnv, url, bid, bgroup, monitor, checkIntervalMillis, connectTimeoutMillis, readTimeoutMillis, headerMap);
        } else if (type == CamelliaRedisProperties.Type.CUSTOM) {
            CamelliaRedisProperties.Custom custom = properties.getCustom();
            String className = custom.getResourceTableUpdaterClassName();
            if (className == null) {
                throw new IllegalArgumentException("proxyRouteConfUpdaterClassName missing");
            }
            RedisTemplateResourceTableUpdater updater;
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                }
                updater = (RedisTemplateResourceTableUpdater) clazz.getConstructor().newInstance();
                logger.info("RedisTemplateResourceTableUpdater init success, class = {}", className);
            } catch (Exception e) {
                logger.error("RedisTemplateResourceTableUpdater init error, class = {}", className, e);
                throw new CamelliaRedisException(e);
            }
            return new CamelliaRedisTemplate(redisEnv, updater);
        } else {
            throw new UnsupportedOperationException("only support local/remote/custom");
        }
    }

    private JedisPoolConfig jedisPoolConfig(CamelliaRedisProperties.RedisConf redisConf) {
        CamelliaRedisProperties.RedisConf.Jedis jedis = redisConf.getJedis();
        if (jedis != null) {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxIdle(jedis.getMaxIdle());
            jedisPoolConfig.setMinIdle(jedis.getMinIdle());
            jedisPoolConfig.setMaxTotal(jedis.getMaxActive());
            jedisPoolConfig.setMaxWaitMillis(jedis.getMaxWaitMillis());
            return jedisPoolConfig;
        } else {
            return new JedisPoolConfig();
        }
    }

    private JedisPoolConfig jedisClusterPoolConfig(CamelliaRedisProperties.RedisConf redisConf) {
        CamelliaRedisProperties.RedisConf.JedisCluster jedisCluster = redisConf.getJedisCluster();
        if (jedisCluster != null) {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxIdle(jedisCluster.getMaxIdle());
            jedisPoolConfig.setMinIdle(jedisCluster.getMinIdle());
            jedisPoolConfig.setMaxTotal(jedisCluster.getMaxActive());
            jedisPoolConfig.setMaxWaitMillis(jedisCluster.getMaxWaitMillis());
            return jedisPoolConfig;
        } else {
            return new JedisPoolConfig();
        }
    }

    private CamelliaRedisEnv camelliaRedisEnv(CamelliaRedisProperties.RedisConf redisConf) {
        CamelliaRedisProperties.RedisConf.Jedis jedis = redisConf.getJedis();
        JedisPoolFactory jedisPoolFactory;
        if (jedis != null) {
            int timeout = jedis.getTimeout();
            jedisPoolFactory = new JedisPoolFactory.DefaultJedisPoolFactory(jedisPoolConfig(redisConf), timeout);
        } else {
            jedisPoolFactory = new JedisPoolFactory.DefaultJedisPoolFactory();
        }
        CamelliaRedisProperties.RedisConf.JedisCluster jedisCluster = redisConf.getJedisCluster();
        JedisClusterFactory jedisClusterFactory;
        if (jedisCluster != null) {
            int timeout = jedisCluster.getTimeout();
            int maxAttempts = jedisCluster.getMaxAttempts();
            jedisClusterFactory = new JedisClusterFactory.DefaultJedisClusterFactory(jedisClusterPoolConfig(redisConf), timeout, timeout, maxAttempts);
        } else {
            jedisClusterFactory = new JedisClusterFactory.DefaultJedisClusterFactory();
        }
        return new CamelliaRedisEnv.Builder()
                .proxyEnv(proxyEnv())
                .jedisPoolFactory(jedisPoolFactory)
                .jedisClusterFactory(jedisClusterFactory)
                .build();
    }
}
