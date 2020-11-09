package com.netease.nim.camellia.redis.proxy.util;

import com.lmax.disruptor.WaitStrategy;
import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.CommandHotKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.CommandHotKeyCacheConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheStatsCallback;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.SlowCommandCallback;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaTranspondProperties;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class ConfigInitUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigInitUtil.class);

    public static MonitorCallback initMonitorCallback(CamelliaServerProperties serverProperties) {
        MonitorCallback monitorCallback = null;
        String monitorCallbackClassName = serverProperties.getMonitorCallbackClassName();
        if (monitorCallbackClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(monitorCallbackClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(monitorCallbackClassName);
                }
                monitorCallback = (MonitorCallback) clazz.newInstance();
                logger.info("MonitorCallback init success, class = {}", monitorCallbackClassName);
            } catch (Exception e) {
                logger.error("MonitorCallback init error, class = {}", monitorCallbackClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return monitorCallback;
    }

    public static CommandSpendTimeConfig initCommandSpendTimeConfig(CamelliaServerProperties serverProperties) {
        boolean commandSpendTimeMonitorEnable = serverProperties.isMonitorEnable() && serverProperties.isCommandSpendTimeMonitorEnable();
        if (!commandSpendTimeMonitorEnable) return null;
        long slowCommandThresholdMillisTime = serverProperties.getSlowCommandThresholdMillisTime();
        String slowCommandCallbackClassName = serverProperties.getSlowCommandCallbackClassName();
        SlowCommandCallback slowCommandCallback = null;
        if (slowCommandCallbackClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(slowCommandCallbackClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(slowCommandCallbackClassName);
                }
                slowCommandCallback = (SlowCommandCallback) clazz.newInstance();
                logger.info("SlowCommandCallback init success, class = {}", slowCommandCallbackClassName);
            } catch (Exception e) {
                logger.error("SlowCommandCallback init error, class = {}", slowCommandCallbackClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return new CommandSpendTimeConfig(slowCommandThresholdMillisTime, slowCommandCallback);
    }

    public static CommandInterceptor initCommandInterceptor(CamelliaServerProperties serverProperties) {
        String commandInterceptorClassName = serverProperties.getCommandInterceptorClassName();
        CommandInterceptor commandInterceptor = null;
        if (commandInterceptorClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(commandInterceptorClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(commandInterceptorClassName);
                }
                commandInterceptor = (CommandInterceptor) clazz.newInstance();
                logger.info("CommandInterceptor init success, class = {}", commandInterceptorClassName);
            } catch (Exception e) {
                logger.error("CommandInterceptor init error, class = {}", commandInterceptorClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return commandInterceptor;
    }

    public static void checkDisruptorWaitStrategyClassName(CamelliaTranspondProperties transpondProperties) {
        CamelliaTranspondProperties.RedisConfProperties.DisruptorConf disruptorConf = transpondProperties.getRedisConf().getDisruptorConf();
        if (disruptorConf != null) {
            String waitStrategyClassName = disruptorConf.getWaitStrategyClassName();
            if (waitStrategyClassName != null) {
                try {
                    Class<?> clazz = Class.forName(waitStrategyClassName);
                    Object o = clazz.newInstance();
                    if (!(o instanceof WaitStrategy)) {
                        throw new CamelliaRedisException("not instance of com.lmax.disruptor.WaitStrategy");
                    }
                } catch (CamelliaRedisException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error("CommandInterceptor init error, class = {}", waitStrategyClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
    }

    public static CommandHotKeyMonitorConfig initCommandHotKeyMonitorConfig(CamelliaServerProperties serverProperties) {
        CommandHotKeyMonitorConfig commandHotKeyMonitorConfig = null;
        CamelliaServerProperties.HotKeyMonitorConfig config = serverProperties.getHotKeyMonitorConfig();
        if (config != null && serverProperties.isHotKeyMonitorEnable()) {
            HotKeyMonitorCallback hotKeyMonitorCallback = null;
            String hotKeyCallbackClassName = config.getHotKeyMonitorCallbackClassName();
            if (hotKeyCallbackClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(hotKeyCallbackClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(hotKeyCallbackClassName);
                    }
                    hotKeyMonitorCallback = (HotKeyMonitorCallback) clazz.newInstance();
                    logger.info("HotKeyMonitorCallback init success, class = {}", hotKeyCallbackClassName);
                } catch (Exception e) {
                    logger.error("HotKeyMonitorCallback init error, class = {}", hotKeyCallbackClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
            HotKeyConfig hotKeyConfig = new HotKeyConfig(config.getCheckMillis(), config.getCheckCacheMaxCapacity(),
                    config.getCheckThreshold(), config.getMaxHotKeyCount());
            commandHotKeyMonitorConfig = new CommandHotKeyMonitorConfig(hotKeyConfig, hotKeyMonitorCallback);
        }
        return commandHotKeyMonitorConfig;
    }

    public static CommandHotKeyCacheConfig initHotKeyCacheConfig(CamelliaServerProperties serverProperties) {
        if (!serverProperties.isHotKeyCacheEnable()) return null;
        CamelliaServerProperties.HotKeyCacheConfig cacheConfig = serverProperties.getHotKeyCacheConfig();
        CommandHotKeyCacheConfig commandHotKeyCacheConfig = new CommandHotKeyCacheConfig();
        commandHotKeyCacheConfig.setHotKeyCacheCounterCheckThreshold(cacheConfig.getHotKeyCacheCounterCheckThreshold());
        commandHotKeyCacheConfig.setHotKeyCacheCounterCheckMillis(cacheConfig.getHotKeyCacheCounterCheckMillis());
        commandHotKeyCacheConfig.setHotKeyCacheCounterMaxCapacity(cacheConfig.getHotKeyCacheCounterMaxCapacity());
        commandHotKeyCacheConfig.setHotKeyCacheExpireMillis(cacheConfig.getHotKeyCacheExpireMillis());
        commandHotKeyCacheConfig.setHotKeyCacheNeedCacheNull(cacheConfig.isHotKeyCacheNeedCacheNull());
        String hotKeyCacheKeyCheckerClassName = cacheConfig.getHotKeyCacheKeyCheckerClassName();
        HotKeyCacheKeyChecker hotKeyCacheKeyChecker = null;
        if (hotKeyCacheKeyCheckerClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(hotKeyCacheKeyCheckerClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(hotKeyCacheKeyCheckerClassName);
                }
                hotKeyCacheKeyChecker = (HotKeyCacheKeyChecker) clazz.newInstance();
                logger.info("HotKeyCacheKeyChecker init success, class = {}", hotKeyCacheKeyCheckerClassName);
            } catch (Exception e) {
                logger.error("HotKeyCacheKeyChecker init error, class = {}", hotKeyCacheKeyCheckerClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        commandHotKeyCacheConfig.setHotKeyCacheKeyChecker(hotKeyCacheKeyChecker);
        commandHotKeyCacheConfig.setHotKeyCacheStatsCallbackIntervalSeconds(cacheConfig.getHotKeyCacheStatsCallbackIntervalSeconds());
        String hotKeyCacheStatsCallbackClassName = cacheConfig.getHotKeyCacheStatsCallbackClassName();
        HotKeyCacheStatsCallback hotKeyCacheStatsCallback = null;
        if (hotKeyCacheStatsCallbackClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(hotKeyCacheStatsCallbackClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(hotKeyCacheStatsCallbackClassName);
                }
                hotKeyCacheStatsCallback = (HotKeyCacheStatsCallback) clazz.newInstance();
                logger.info("HotKeyCacheStatsCallback init success, class = {}", hotKeyCacheStatsCallbackClassName);
            } catch (Exception e) {
                logger.error("HotKeyCacheStatsCallback init error, class = {}", hotKeyCacheStatsCallbackClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        commandHotKeyCacheConfig.setHotKeyCacheStatsCallback(hotKeyCacheStatsCallback);
        return commandHotKeyCacheConfig;
    }
}
