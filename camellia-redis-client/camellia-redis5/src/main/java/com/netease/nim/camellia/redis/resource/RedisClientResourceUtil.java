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
            String urlWithoutQueryString = RedisResourceUtil.getUrlWithoutQueryString(substring);
            int index = urlWithoutQueryString.lastIndexOf("@");

            String[] userNameAndPassword = RedisResourceUtil.getUserNameAndPassword(urlWithoutQueryString.substring(0, index));
            String userName = userNameAndPassword[0];
            if (userName != null) {
                throw new CamelliaRedisException("not support set username");
            }
            String password = userNameAndPassword[1];

            String proxyName = urlWithoutQueryString.substring(index + 1);

            Map<String, String> paramMap = RedisResourceUtil.getParamMap(substring);

            CamelliaRedisProxyResource camelliaRedisProxyResource;
            String bidStr = paramMap.get("bid");
            String bgroup = paramMap.get("bgroup");
            String dbStr = paramMap.get("db");
            if (bidStr != null || bgroup != null || dbStr != null) {
                long bid = bidStr == null ? -1 : Long.parseLong(bidStr);
                int db = dbStr == null ? 0 : Integer.parseInt(dbStr);
                camelliaRedisProxyResource = new CamelliaRedisProxyResource(password, proxyName, bid, bgroup, db);
            } else {
                camelliaRedisProxyResource = new CamelliaRedisProxyResource(password, proxyName);
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
