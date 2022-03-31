package com.netease.nim.camellia.feign.springboot;

import com.netease.nim.camellia.feign.CamelliaFeignClientFactory;
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
        if (beanFactory != null) {
            factory = beanFactory.getBean(CamelliaFeignClientFactory.class);
        } else {
            factory = applicationContext.getBean(CamelliaFeignClientFactory.class);
        }
        return factory.getService(type);
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
