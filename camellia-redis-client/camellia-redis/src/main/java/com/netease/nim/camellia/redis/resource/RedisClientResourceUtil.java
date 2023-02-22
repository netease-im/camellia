package com.netease.nim.camellia.redis.resource;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.ResourceTableChecker;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ResourceUtil;
import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.base.resource.CamelliaRedisProxyResource;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.base.resource.RedisType;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyContext;
import com.netease.nim.camellia.redis.proxy.CamelliaRedisProxyFactory;
import com.netease.nim.camellia.redis.proxy.RedisProxyResource;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPool;
import com.netease.nim.camellia.redis.proxy.discovery.jedis.RedisProxyJedisPoolContext;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.Set;

/**
 * Created by caojiajun on 2023/1/30
 */
public class RedisClientResourceUtil {

    public static final ResourceTableChecker RedisResourceTableChecker = resourceTable -> {
        try {
            checkResourceTable(resourceTable);
            return true;
        } catch (Exception e) {
            return false;
        }
    };

    public static void checkResourceTable(ResourceTable resourceTable) {
        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException("resourceTable check fail");
        }
        Set<Resource> allResources = ResourceUtil.getAllResources(resourceTable);
        for (Resource redisResource : allResources) {
            parseResourceByUrl(redisResource);
        }
    }

    public static Resource parseResourceByUrl(Resource resource) {
        String url = resource.getUrl();
        if (url.startsWith(RedisType.RedisProxy.getPrefix())) {
            String substring = url.substring(RedisType.RedisProxy.getPrefix().length());
            long id = Long.parseLong(substring);
            RedisProxyJedisPool pool = RedisProxyJedisPoolContext.get(id);
            if (pool == null) {
                throw new CamelliaRedisException("not found RedisProxyJedisPool with id = " + id);
            }
            RedisProxyResource redisProxyResource = new RedisProxyResource(pool);
            if (!redisProxyResource.getUrl().equals(resource.getUrl())) {
                throw new CamelliaRedisException("resource url not equals");
            }
            return redisProxyResource;
        } else if (url.startsWith(RedisType.CamelliaRedisProxy.getPrefix())) {
            String substring = url.substring(RedisType.CamelliaRedisProxy.getPrefix().length());
            if (!substring.contains("@")) {
                throw new CamelliaRedisException("missing @");
            }
            int index = substring.lastIndexOf("@");
            String password = substring.substring(0, index);
            if (password.length() == 0) {
                password = null;
            }

            CamelliaRedisProxyResource camelliaRedisProxyResource;
            String proxyName;
            if (!substring.contains("?")) {
                proxyName = substring.substring(index + 1);
                camelliaRedisProxyResource = new CamelliaRedisProxyResource(password, proxyName);
            } else {
                int i = substring.lastIndexOf("?");
                proxyName = substring.substring(index + 1, i);
                String queryString = substring.substring(i + 1);
                Map<String, String> params = RedisResourceUtil.getParams(queryString);
                String bidStr = params.get("bid");
                long bid = bidStr == null ? -1 : Long.parseLong(bidStr);
                String bgroup = params.get("bgroup");
                String dbStr = params.get("db");
                int db = dbStr == null ? 0 : Integer.parseInt(dbStr);
                camelliaRedisProxyResource = new CamelliaRedisProxyResource(password, proxyName, bid, bgroup, db);
            }
            CamelliaRedisProxyFactory factory = CamelliaRedisProxyContext.getFactory();
            if (factory == null) {
                throw new CamelliaRedisException("no CamelliaRedisProxyFactory register to CamelliaRedisProxyContext");
            }
            JedisPool jedisPool = factory.initOrGet(camelliaRedisProxyResource);
            if (jedisPool == null) {
                throw new CamelliaRedisException("CamelliaRedisProxyFactory initOrGet JedisPool fail");
            }
            return camelliaRedisProxyResource;
        }
        return RedisResourceUtil.parseResourceByUrl(resource);
    }
}
