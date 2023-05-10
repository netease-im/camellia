package com.netease.nim.camellia.hot.key.server.springboot.conf;

import com.netease.nim.camellia.hot.key.server.bean.BeanFactory;
import com.netease.nim.camellia.hot.key.server.bean.DefaultBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;


/**
 * Created by caojiajun on 2022/9/19
 */
public class SpringProxyBeanFactory implements BeanFactory {

    private static final Logger logger = LoggerFactory.getLogger(SpringProxyBeanFactory.class);

    private final DefaultBeanFactory defaultBeanFactory = DefaultBeanFactory.INSTANCE;
    private final ApplicationContext applicationContext;

    public SpringProxyBeanFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        try {
            if (applicationContext != null) {
                T bean = applicationContext.getBean(requiredType);
                logger.info("get bean from spring success, requiredType = {}", requiredType.getName());
                return bean;
            }
        } catch (Exception ignore) {
        }
        return defaultBeanFactory.getBean(requiredType);
    }

}
