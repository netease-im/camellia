package com.netease.nim.camellia.feign.springboot;

import com.netease.nim.camellia.feign.CamelliaFeignClient;
import com.netease.nim.camellia.feign.CamelliaFeignClientFactory;
import com.netease.nim.camellia.feign.CamelliaFeignFailureListener;
import com.netease.nim.camellia.feign.CamelliaFeignFallbackFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Created by caojiajun on 2022/3/29
 */
public class CamelliaFeignClientFactoryBean implements FactoryBean<Object>, BeanFactoryAware, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private BeanFactory beanFactory;
    private Class<?> type;

    @Override
    public Object getObject() {
        CamelliaFeignClientFactory factory;
        CamelliaFeignClient annotation = type.getAnnotation(CamelliaFeignClient.class);
        Class<?> fallbackFactoryClazz = null;
        Class<?> fallbackClazz = null;
        Class<?> failureListenerClazz = null;
        if (annotation != null) {
            fallbackFactoryClazz = annotation.fallbackFactory();
            fallbackClazz = annotation.fallback();
            failureListenerClazz = annotation.failureListener();
        }
        Object fallbackFactory = null;
        Object fallback = null;
        Object failureListener = null;
        if (beanFactory != null) {
            factory = beanFactory.getBean(CamelliaFeignClientFactory.class);
            try {
                if (fallbackFactoryClazz != null && !void.class.isAssignableFrom(fallbackFactoryClazz)) {
                    fallbackFactory = beanFactory.getBean(fallbackFactoryClazz);
                }
                if (fallbackFactory == null && fallbackClazz != null && !void.class.isAssignableFrom(fallbackClazz)) {
                    fallback = beanFactory.getBean(fallbackClazz);
                }
                if (failureListenerClazz != null && !void.class.isAssignableFrom(failureListenerClazz)) {
                    failureListener = beanFactory.getBean(failureListenerClazz);
                }
            } catch (Exception ignore) {
            }
        } else {
            factory = applicationContext.getBean(CamelliaFeignClientFactory.class);
            try {
                if (fallbackFactoryClazz != null) {
                    fallbackFactory = applicationContext.getBean(fallbackFactoryClazz);
                }
                if (fallbackFactory == null && fallbackClazz != null) {
                    fallback = applicationContext.getBean(fallbackClazz);
                }
                if (failureListenerClazz != null) {
                    failureListener = beanFactory.getBean(failureListenerClazz);
                }
            } catch (Exception ignore) {
            }
        }
        if (fallbackFactory != null) {
            return factory.getService(type, (CamelliaFeignFallbackFactory) fallbackFactory, (CamelliaFeignFailureListener) failureListener);
        } else if (fallback != null) {
            return factory.getService(type, (CamelliaFeignFallbackFactory) new CamelliaFeignFallbackFactory.Default<>(fallback), (CamelliaFeignFailureListener) failureListener);
        } else {
            return factory.getService(type);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
