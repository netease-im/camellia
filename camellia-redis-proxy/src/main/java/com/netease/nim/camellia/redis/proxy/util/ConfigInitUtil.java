package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.command.async.CommandInterceptor;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.BigKeyMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.async.bigkey.CommandBigKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.converter.*;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.CommandHotKeyMonitorConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.async.hotkey.HotKeyConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.CommandHotKeyCacheConfig;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheKeyChecker;
import com.netease.nim.camellia.redis.proxy.command.async.hotkeycache.HotKeyCacheStatsCallback;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.CommandSpendTimeConfig;
import com.netease.nim.camellia.redis.proxy.command.async.spendtime.SlowCommandMonitorCallback;
import com.netease.nim.camellia.redis.proxy.command.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfHook;
import com.netease.nim.camellia.redis.proxy.monitor.MonitorCallback;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * Created by caojiajun on 2020/10/22
 */
public class ConfigInitUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigInitUtil.class);

    public static void initProxyDynamicConfHook(CamelliaServerProperties serverProperties) {
        String proxyDynamicConfHookClassName = serverProperties.getProxyDynamicConfHookClassName();
        ProxyDynamicConfHook hook;
        if (proxyDynamicConfHookClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(proxyDynamicConfHookClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(proxyDynamicConfHookClassName);
                }
                hook = (ProxyDynamicConfHook) clazz.newInstance();
                logger.info("ProxyDynamicConfHook init success, class = {}", proxyDynamicConfHookClassName);
                ProxyDynamicConf.updateProxyDynamicConfHook(hook);
            } catch (Exception e) {
                logger.error("ProxyDynamicConfHook init error, class = {}", proxyDynamicConfHookClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
    }

    public static ClientAuthProvider initClientAuthProvider(CamelliaServerProperties serverProperties) {
        ClientAuthProvider clientAuthProvider = null;
        String clientAuthProviderClassName = serverProperties.getClientAuthProviderClassName();
        if (clientAuthProviderClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(clientAuthProviderClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(clientAuthProviderClassName);
                }
                Constructor constructorWithProperties = clazz.getDeclaredConstructor(new Class[]{CamelliaServerProperties.class});
                if (constructorWithProperties != null) {
                    clientAuthProvider = (ClientAuthProvider) constructorWithProperties.newInstance(serverProperties);
                } else {
                    clientAuthProvider = (ClientAuthProvider) clazz.newInstance();
                }

                logger.info("ClientAuthProvider init success, class = {}", clientAuthProvider);
            } catch (Exception e) {
                logger.error("ClientAuthProvider init error, class = {}", clientAuthProvider, e);
                throw new CamelliaRedisException(e);
            }
        }
        return clientAuthProvider;
    }

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
        SlowCommandMonitorCallback slowCommandMonitorCallback = null;
        if (slowCommandCallbackClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(slowCommandCallbackClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(slowCommandCallbackClassName);
                }
                slowCommandMonitorCallback = (SlowCommandMonitorCallback) clazz.newInstance();
                logger.info("SlowCommandCallback init success, class = {}", slowCommandCallbackClassName);
            } catch (Exception e) {
                logger.error("SlowCommandCallback init error, class = {}", slowCommandCallbackClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return new CommandSpendTimeConfig(slowCommandThresholdMillisTime, slowCommandMonitorCallback);
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
        commandHotKeyCacheConfig.setCounterCheckThreshold(cacheConfig.getCounterCheckThreshold());
        commandHotKeyCacheConfig.setCounterCheckMillis(cacheConfig.getCounterCheckMillis());
        commandHotKeyCacheConfig.setCounterMaxCapacity(cacheConfig.getCounterMaxCapacity());
        commandHotKeyCacheConfig.setCacheExpireMillis(cacheConfig.getCacheExpireMillis());
        commandHotKeyCacheConfig.setCacheMaxCapacity(cacheConfig.getCacheMaxCapacity());
        commandHotKeyCacheConfig.setNeedCacheNull(cacheConfig.isNeedCacheNull());
        String hotKeyCacheKeyCheckerClassName = cacheConfig.getCacheKeyCheckerClassName();
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

    public static CommandBigKeyMonitorConfig initBigKeyMonitorConfig(CamelliaServerProperties serverProperties) {
        if (!serverProperties.isBigKeyMonitorEnable()) return null;
        CamelliaServerProperties.BigKeyMonitorConfig bigKeyMonitorConfig = serverProperties.getBigKeyMonitorConfig();
        String bigKeyCallbackClassName = bigKeyMonitorConfig.getBigKeyMonitorCallbackClassName();
        BigKeyMonitorCallback bigKeyMonitorCallback = null;
        if (bigKeyCallbackClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(bigKeyCallbackClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(bigKeyCallbackClassName);
                }
                bigKeyMonitorCallback = (BigKeyMonitorCallback) clazz.newInstance();
                logger.info("BigKeyCallback init success, class = {}", bigKeyCallbackClassName);
            } catch (Exception e) {
                logger.error("BigKeyCallback init error, class = {}", bigKeyCallbackClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return new CommandBigKeyMonitorConfig(bigKeyMonitorConfig.getStringSizeThreshold(),
                bigKeyMonitorConfig.getListSizeThreshold(), bigKeyMonitorConfig.getZsetSizeThreshold(),
                bigKeyMonitorConfig.getHashSizeThreshold(), bigKeyMonitorConfig.getSetSizeThreshold(), bigKeyMonitorCallback);
    }

    public static ConverterConfig initConverterConfig(CamelliaServerProperties serverProperties) {
        if (!serverProperties.isConverterEnable()) return null;
        CamelliaServerProperties.ConverterConfig converterConfig = serverProperties.getConverterConfig();
        if (converterConfig == null) return null;
        ConverterConfig config = new ConverterConfig();
        String stringConverterClassName = converterConfig.getStringConverterClassName();
        if (stringConverterClassName != null) {
            try {
                StringConverter stringConverter;
                Class<?> clazz;
                try {
                    clazz = Class.forName(stringConverterClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(stringConverterClassName);
                }
                stringConverter = (StringConverter) clazz.newInstance();
                logger.info("StringConverter init success, class = {}", stringConverterClassName);
                config.setStringConverter(stringConverter);
            } catch (Exception e) {
                logger.error("StringConverter init error, class = {}", stringConverterClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        String listConverterClassName = converterConfig.getListConverterClassName();
        if (listConverterClassName != null) {
            try {
                ListConverter listConverter;
                Class<?> clazz;
                try {
                    clazz = Class.forName(listConverterClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(listConverterClassName);
                }
                listConverter = (ListConverter) clazz.newInstance();
                logger.info("ListConverter init success, class = {}", listConverterClassName);
                config.setListConverter(listConverter);
            } catch (Exception e) {
                logger.error("ListConverter init error, class = {}", stringConverterClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        String setConverterClassName = converterConfig.getSetConverterClassName();
        if (setConverterClassName != null) {
            try {
                SetConverter setConverter;
                Class<?> clazz;
                try {
                    clazz = Class.forName(setConverterClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(setConverterClassName);
                }
                setConverter = (SetConverter) clazz.newInstance();
                logger.info("SetConverter init success, class = {}", setConverterClassName);
                config.setSetConverter(setConverter);
            } catch (Exception e) {
                logger.error("SetConverter init error, class = {}", setConverterClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        String hashConverterClassName = converterConfig.getHashConverterClassName();
        if (hashConverterClassName != null) {
            try {
                HashConverter hashConverter;
                Class<?> clazz;
                try {
                    clazz = Class.forName(hashConverterClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(hashConverterClassName);
                }
                hashConverter = (HashConverter) clazz.newInstance();
                logger.info("HashConverter init success, class = {}", hashConverterClassName);
                config.setHashConverter(hashConverter);
            } catch (Exception e) {
                logger.error("HashConverter init error, class = {}", hashConverterClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        String zsetConverterClassName = converterConfig.getZsetConverterClassName();
        if (zsetConverterClassName != null) {
            try {
                ZSetConverter zSetConverter;
                Class<?> clazz;
                try {
                    clazz = Class.forName(zsetConverterClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(zsetConverterClassName);
                }
                zSetConverter = (ZSetConverter) clazz.newInstance();
                logger.info("ZSetConverter init success, class = {}", zsetConverterClassName);
                config.setzSetConverter(zSetConverter);
            } catch (Exception e) {
                logger.error("ZSetConverter init error, class = {}", zsetConverterClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        return config;
    }
}
