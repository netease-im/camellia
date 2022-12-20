package com.netease.nim.camellia.cache.core.boot;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.reflect.Field;
import java.util.function.Supplier;

/**
 * @see ProxyCachingConfiguration
 */
@Configuration
public class CamelliaProxyCachingConfiguration extends ProxyCachingConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @Override
    public CacheInterceptor cacheInterceptor() {
        try {
            CacheInterceptor interceptor = new CamelliaCacheInterceptor();
            interceptor.setCacheOperationSources(cacheOperationSource());
            try {
                Field cacheManagerField = AbstractCachingConfiguration.class.getDeclaredField("cacheManager");
                cacheManagerField.setAccessible(true);
                Object cacheManager = cacheManagerField.get(this);
                if (cacheManager != null) {
                    if (cacheManager instanceof Supplier) {
                        CacheManager manager = ((Supplier<CacheManager>) cacheManager).get();
                        if (manager != null) {
                            interceptor.setCacheManager(manager);
                        }
                    } else if (cacheManager instanceof CacheManager) {
                        interceptor.setCacheManager((CacheManager) cacheManager);
                    }
                }
            } catch (Exception ignore) {
            }

            Object keyGenerator;
            try {
                Field keyGeneratorField = AbstractCachingConfiguration.class.getDeclaredField("keyGenerator");
                keyGeneratorField.setAccessible(true);
                keyGenerator = keyGeneratorField.get(this);
                if (keyGenerator != null) {
                    if (keyGenerator instanceof Supplier) {
                        KeyGenerator generator = ((Supplier<KeyGenerator>) keyGenerator).get();
                        if (generator != null) {
                            interceptor.setKeyGenerator(generator);
                        }
                    } else if (keyGenerator instanceof KeyGenerator) {
                        interceptor.setKeyGenerator((KeyGenerator) keyGenerator);
                    }
                }
            } catch (Exception ignore) {
            }
            try {
                Field errorHandlerField = AbstractCachingConfiguration.class.getDeclaredField("errorHandler");
                errorHandlerField.setAccessible(true);
                Object errorHandler = errorHandlerField.get(this);
                if (errorHandler != null) {
                    if (errorHandler instanceof Supplier) {
                        CacheErrorHandler handler = ((Supplier<CacheErrorHandler>) errorHandler).get();
                        if (handler != null) {
                            interceptor.setErrorHandler(handler);
                        }
                    } else if (errorHandler instanceof CacheErrorHandler) {
                        interceptor.setErrorHandler((CacheErrorHandler) errorHandler);
                    }
                }
            } catch (Exception ignore) {
            }
            return interceptor;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        this.enableCaching = AnnotationAttributes.fromMap(
                importMetadata.getAnnotationAttributes(EnableCamelliaCaching.class.getName(), false));
        if (this.enableCaching == null) {
            throw new IllegalArgumentException(
                    "@EnableCamelliaCaching is not present on importing class " + importMetadata.getClassName());
        }
    }
}
