package com.netease.nim.camellia.dashboard.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 会把所有实现了IResourceChecker接口的spring bean都加载进来，
 * Created by caojiajun on 2019/12/10.
 */
@Service
public class ResourceCheckService implements ApplicationContextAware, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCheckService.class);

    private ApplicationContext applicationContext;
    private final Map<String, IResourceChecker> checkerMap = new HashMap<>();

    /**
     * 只要有一个check成功，即成功
     * @param url url
     * @return 结果
     */
    public boolean check(String url) {
        for (IResourceChecker checker : checkerMap.values()) {
            boolean check = checker.check(url);
            if (check) return true;
        }
        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.applicationContext != null) {
            Map<String, IResourceChecker> checkerMap = applicationContext.getBeansOfType(IResourceChecker.class);
            if (!checkerMap.isEmpty()) {
                this.checkerMap.putAll(checkerMap);
                for (Map.Entry<String, IResourceChecker> entry : checkerMap.entrySet()) {
                    logger.info("load IResourceChecker = {}", entry.getValue().getClass().getName());
                }
            }
        }
    }
}
