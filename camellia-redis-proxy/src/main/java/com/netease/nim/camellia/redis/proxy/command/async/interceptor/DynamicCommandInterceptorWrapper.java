package com.netease.nim.camellia.redis.proxy.command.async.interceptor;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态CommandInterceptor的Wrapper
 * 可以组装多个CommandInterceptor
 */
public class DynamicCommandInterceptorWrapper implements CommandInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DynamicCommandInterceptorWrapper.class);

    private final ConcurrentHashMap<String, CommandInterceptor> cache = new ConcurrentHashMap<>();
    private List<CommandInterceptor> commandInterceptorList = new ArrayList<>();

    public DynamicCommandInterceptorWrapper() {
        reload();
        ProxyDynamicConf.registerCallback(this::reload);
    }

    private void reload() {
        try {
            String classNames = ProxyDynamicConf.getString("dynamic.command.interceptor.class.names", null);
            List<CommandInterceptor> list = new ArrayList<>();
            if (classNames != null) {
                String[] split = classNames.split("\\|");
                for (String className : split) {
                    CommandInterceptor interceptor = cache.get(className);
                    if (interceptor != null) {
                        list.add(interceptor);
                    } else {
                        try {
                            Class<?> clazz;
                            try {
                                clazz = Class.forName(className);
                            } catch (ClassNotFoundException e) {
                                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                            }
                            interceptor = (CommandInterceptor) clazz.newInstance();
                            logger.info("CommandInterceptor init success, class = {}", className);
                            cache.put(className, interceptor);
                            list.add(interceptor);
                        } catch (Exception e) {
                            logger.error("CommandInterceptor init error, will skip, class = {}", className, e);
                        }
                    }
                }
            }
            this.commandInterceptorList = list;
        } catch (Exception e) {
            logger.error("DynamicCommandInterceptorWrapper reload error", e);
        } finally {
            logger.info("DynamicCommandInterceptorWrapper reload finish, interceptor list.size = {}", commandInterceptorList.size());
            for (int i=0; i<commandInterceptorList.size(); i++) {
                CommandInterceptor interceptor = commandInterceptorList.get(i);
                logger.info("commandInterceptor-{}, class = {}", i, interceptor.getClass().getName());
            }
        }
    }

    @Override
    public CommandInterceptResponse check(Command command) {
        for (CommandInterceptor commandInterceptor : commandInterceptorList) {
            try {
                CommandInterceptResponse response = commandInterceptor.check(command);
                if (!response.isPass()) {
                    return response;
                }
            } catch (Exception e) {
                ErrorLogCollector.collect(DynamicCommandInterceptorWrapper.class,
                        "commandInterceptor error, class = " + commandInterceptor.getClass().getName(), e);
                return CommandInterceptResponse.DEFAULT_FAIL;
            }
        }
        return CommandInterceptResponse.SUCCESS;
    }
}
