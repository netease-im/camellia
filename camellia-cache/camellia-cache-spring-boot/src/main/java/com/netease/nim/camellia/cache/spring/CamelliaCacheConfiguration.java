package com.netease.nim.camellia.cache.spring;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netease.nim.camellia.cache.core.*;
import com.netease.nim.camellia.cache.spring.caffeine.CaffeineNativeCacheInitializer;
import com.netease.nim.camellia.cache.spring.redis.RedisNativeCacheInitializer;
import com.netease.nim.camellia.redis.CamelliaRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
@EnableConfigurationProperties(CamelliaCacheProperties.class)
public class CamelliaCacheConfiguration extends CachingConfigurerSupport {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaCacheConfiguration.class);

    @Autowired(required = false)
    private CamelliaRedisTemplate redisTemplate;

    @Autowired(required = false)
    private CamelliaCachePrefixGetter cachePrefixGetter;

    @Bean
    @ConditionalOnMissingBean(value = CamelliaCacheSerializer.class)
    public CamelliaCacheSerializer<Object> camelliaCacheSerializer(CamelliaCacheProperties camelliaCacheProperties) {
        Jackson2JsonCamelliaCacheSerializer<Object> jackson2JsonCamelliaSerializer = new Jackson2JsonCamelliaCacheSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonCamelliaSerializer.setObjectMapper(objectMapper);
        jackson2JsonCamelliaSerializer.updateCompress(camelliaCacheProperties.isCompressEnable(), camelliaCacheProperties.getCompressThreshold());
        jackson2JsonCamelliaSerializer.updateNullTypeCacheTypeList(camelliaCacheProperties.getNullCacheTypeList());
        logger.info("camellia-cache, compress-enable = {}, compress-threshold = {}, null-cache-type-list = {}",
                camelliaCacheProperties.isCompressEnable(), camelliaCacheProperties.getCompressThreshold(), camelliaCacheProperties.getNullCacheTypeList());
        return jackson2JsonCamelliaSerializer;
    }

    @Bean
    @ConditionalOnMissingBean(value = CacheErrorHandler.class)
    public CacheErrorHandler errorHandler() {
        return new CamelliaCacheErrorHandler();
    }

    @ConditionalOnMissingBean(value = RemoteNativeCacheInitializer.class)
    public RemoteNativeCacheInitializer remoteNativeCacheInitializer() {
        return new RedisNativeCacheInitializer();
    }

    @ConditionalOnMissingBean(value = LocalNativeCacheInitializer.class)
    public LocalNativeCacheInitializer localNativeCacheInitializer() {
        return new CaffeineNativeCacheInitializer();
    }

    @Bean
    @ConditionalOnMissingBean(value = CacheManager.class)
    public CacheManager camelliaCacheManager(CamelliaCacheProperties camelliaCacheProperties) {
        CamelliaCacheManager camelliaCacheManager = new CamelliaCacheManager();
        CamelliaCacheEnv.enable = camelliaCacheProperties.isEnable();
        CamelliaCacheEnv.multiOpBatchSize = camelliaCacheProperties.getMultiOpBatchSize();
        CamelliaCacheEnv.syncLoadExpireMillis = camelliaCacheProperties.getSyncLoadExpireMillis();
        CamelliaCacheEnv.syncLoadMaxRetry = camelliaCacheProperties.getSyncLoadMaxRetry();
        CamelliaCacheEnv.syncLoadSleepMillis = camelliaCacheProperties.getSyncLoadSleepMillis();
        CamelliaCacheEnv.maxCacheValue = camelliaCacheProperties.getMaxCacheValue();
        CamelliaCacheEnv.serializerErrorLogEnable = camelliaCacheProperties.isSerializerErrorLogEnable();
        logger.info("camellia-cache, enable = {}", CamelliaCacheEnv.enable);
        logger.info("camellia-cache, multiOpBatchSize = {}", CamelliaCacheEnv.multiOpBatchSize);
        logger.info("camellia-cache, syncLoadExpireMillis = {}", CamelliaCacheEnv.syncLoadExpireMillis);
        logger.info("camellia-cache, syncLoadMaxRetry = {}", CamelliaCacheEnv.syncLoadMaxRetry);
        logger.info("camellia-cache, syncLoadSleepMillis = {}", CamelliaCacheEnv.syncLoadSleepMillis);
        logger.info("camellia-cache, maxCacheValue = {}", CamelliaCacheEnv.maxCacheValue);
        logger.info("camellia-cache, serializerErrorLogEnable = {}", CamelliaCacheEnv.serializerErrorLogEnable);

        CamelliaCacheSerializer<Object> serializer = camelliaCacheSerializer(camelliaCacheProperties);
        logger.info("camellia-cache, serializer = {}", serializer.getClass().getName());
        if (cachePrefixGetter != null) {
            logger.info("camellia-cache, cachePrefixGetter = {}", cachePrefixGetter.getClass().getName());
        }
        //local cache, default caffeine
        CamelliaCacheProperties.Local local = camelliaCacheProperties.getLocal();
        LocalNativeCacheInitializer localNativeCacheInitializer = localNativeCacheInitializer();
        int initialCapacity = local.getInitialCapacity();
        int maxCapacity = local.getMaxCapacity();
        for (CamelliaCacheNameEnum camelliaCacheNameEnum : CamelliaCacheNameEnum.values()) {
            if (camelliaCacheNameEnum.getName().contains("LOCAL")) {
                LocalNativeCache localNativeCache;
                if (camelliaCacheNameEnum.getName().contains("SAFE")) {
                    localNativeCache = localNativeCacheInitializer.init(serializer, initialCapacity, maxCapacity, true);
                } else {
                    localNativeCache = localNativeCacheInitializer.init(serializer, initialCapacity, maxCapacity, false);
                }
                if (localNativeCache != null) {
                    camelliaCacheManager.addCamelliaCache(new CamelliaCacheConfig<>(cachePrefixGetter, camelliaCacheNameEnum, localNativeCache));
                }
            }
        }
        logger.info("local camellia-cache init success, LocalNativeCacheInitializer = {}", localNativeCacheInitializer.getClass().getName());
        //remote cache, default redis
        RemoteNativeCacheInitializer remoteNativeCacheInitializer = remoteNativeCacheInitializer();
        if (remoteNativeCacheInitializer instanceof RedisNativeCacheInitializer) {
            ((RedisNativeCacheInitializer) remoteNativeCacheInitializer).setRedisTemplate(redisTemplate);
        }
        RemoteNativeCache remoteNativeCache = remoteNativeCacheInitializer.init(serializer);
        if (remoteNativeCache != null) {
            for (CamelliaCacheNameEnum camelliaCacheNameEnum : CamelliaCacheNameEnum.values()) {
                if (camelliaCacheNameEnum.getName().contains("REMOTE")) {
                    camelliaCacheManager.addCamelliaCache(new CamelliaCacheConfig<>(cachePrefixGetter, camelliaCacheNameEnum, remoteNativeCache));
                }
            }
            logger.info("remote camellia-cache init success, RemoteNativeCacheInitializer = {}", remoteNativeCacheInitializer.getClass().getName());
        }
        return camelliaCacheManager;
    }
}
