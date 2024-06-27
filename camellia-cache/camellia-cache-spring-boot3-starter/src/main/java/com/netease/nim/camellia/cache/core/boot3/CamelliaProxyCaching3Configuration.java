package com.netease.nim.camellia.cache.core.boot3;

import com.netease.nim.camellia.cache.core.boot.CamelliaCacheInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @see ProxyCachingConfiguration
 */
@Configuration
public class CamelliaProxyCaching3Configuration extends ProxyCachingConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaProxyCaching3Configuration.class);

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Override
    public CacheInterceptor cacheInterceptor(CacheOperationSource cacheOperationSource) {
        CamelliaCacheInterceptor interceptor = new CamelliaCacheInterceptor();
        interceptor.configure(this.errorHandler, this.keyGenerator, this.cacheResolver, this.cacheManager);
        interceptor.setCacheOperationSource(cacheOperationSource);
        logger.info("CamelliaCacheInterceptor init success");
        return interceptor;
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableCaching = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableCamelliaCaching3.class.getName(), false));
        if (this.enableCaching == null) {
            throw new IllegalArgumentException(
                    "@EnableCamelliaCaching3 is not present on importing class " + importMetadata.getClassName());
        }
    }
}
