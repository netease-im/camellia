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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by caojiajun on 2020/10/22
 */
public class ConfigInitUtil {

    private static final Logger logger = LoggerFactory.getLogger(ConfigInitUtil.class);

    public static void initProxyDynamicConfHook(CamelliaServerProperties serverProperties) {
        ProxyDynamicConfHook hook = serverProperties.getProxyDynamicConfHook();
        if (hook != null) {
            logger.info("ProxyDynamicConfHook init success, class = {}", hook.getClass().getName());
            ProxyDynamicConf.updateProxyDynamicConfHook(hook);
            return;
        }
        String proxyDynamicConfHookClassName = serverProperties.getProxyDynamicConfHookClassName();
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
        ClientAuthProvider clientAuthProvider = serverProperties.getClientAuthProvider();
        if (clientAuthProvider != null) {
            logger.info("ClientAuthProvider init success, class = {}", clientAuthProvider.getClass().getName());
            return clientAuthProvider;
        }
        String clientAuthProviderClassName = serverProperties.getClientAuthProviderClassName();
        if (clientAuthProviderClassName != null) {
            try {
                Class<?> clazz;
                try {
                    clazz = Class.forName(clientAuthProviderClassName);
                } catch (ClassNotFoundException e) {
                    clazz = Thread.currentThread().getContextClassLoader().loadClass(clientAuthProviderClassName);
                }
                try {
                    clientAuthProvider = (ClientAuthProvider) clazz.getDeclaredConstructor(CamelliaServerProperties.class).newInstance(serverProperties);
                } catch (NoSuchMethodException e) {
                    clientAuthProvider = (ClientAuthProvider) clazz.newInstance();
                }
                logger.info("ClientAuthProvider init success, class = {}", clientAuthProviderClassName);
                return clientAuthProvider;
            } catch (Exception e) {
                logger.error("ClientAuthProvider init error, class = {}", clientAuthProviderClassName, e);
                throw new CamelliaRedisException(e);
            }
        }
        throw new CamelliaRedisException("clientAuthProviderClassName missing");
    }

    public static MonitorCallback initMonitorCallback(CamelliaServerProperties serverProperties) {
        MonitorCallback monitorCallback = serverProperties.getMonitorCallback();
        if (monitorCallback != null) {
            logger.info("MonitorCallback init success, class = {}", monitorCallback.getClass().getName());
            return monitorCallback;
        }
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
        SlowCommandMonitorCallback slowCommandMonitorCallback = serverProperties.getSlowCommandMonitorCallback();
        if (slowCommandMonitorCallback != null) {
            logger.info("SlowCommandCallback init success, class = {}", slowCommandMonitorCallback.getClass().getName());
        } else {
            String slowCommandCallbackClassName = serverProperties.getSlowCommandCallbackClassName();
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
        }
        return new CommandSpendTimeConfig(slowCommandThresholdMillisTime, slowCommandMonitorCallback);
    }

    public static CommandInterceptor initCommandInterceptor(CamelliaServerProperties serverProperties) {
        CommandInterceptor commandInterceptor = serverProperties.getCommandInterceptor();
        if (commandInterceptor != null) {
            logger.info("CommandInterceptor init success, class = {}", commandInterceptor.getClass().getName());
            return commandInterceptor;
        }
        String commandInterceptorClassName = serverProperties.getCommandInterceptorClassName();
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
        if (!serverProperties.isHotKeyMonitorEnable()) return null;
        CamelliaServerProperties.HotKeyMonitorConfig config = serverProperties.getHotKeyMonitorConfig();
        if (config == null) return null;
        HotKeyMonitorCallback hotKeyMonitorCallback = config.getHotKeyMonitorCallback();
        if (hotKeyMonitorCallback != null) {
            logger.info("HotKeyMonitorCallback init success, class = {}", hotKeyMonitorCallback.getClass().getName());
        } else {
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
        }
        HotKeyConfig hotKeyConfig = new HotKeyConfig(config.getCheckMillis(), config.getCheckCacheMaxCapacity(),
                config.getCheckThreshold(), config.getMaxHotKeyCount());
        return new CommandHotKeyMonitorConfig(hotKeyConfig, hotKeyMonitorCallback);
    }

    public static CommandHotKeyCacheConfig initHotKeyCacheConfig(CamelliaServerProperties serverProperties) {
        if (!serverProperties.isHotKeyCacheEnable()) return null;
        CamelliaServerProperties.HotKeyCacheConfig config = serverProperties.getHotKeyCacheConfig();
        if (config == null) return null;
        CommandHotKeyCacheConfig commandHotKeyCacheConfig = new CommandHotKeyCacheConfig();
        commandHotKeyCacheConfig.setCounterCheckThreshold(config.getCounterCheckThreshold());
        commandHotKeyCacheConfig.setCounterCheckMillis(config.getCounterCheckMillis());
        commandHotKeyCacheConfig.setCounterMaxCapacity(config.getCounterMaxCapacity());
        commandHotKeyCacheConfig.setCacheExpireMillis(config.getCacheExpireMillis());
        commandHotKeyCacheConfig.setCacheMaxCapacity(config.getCacheMaxCapacity());
        commandHotKeyCacheConfig.setNeedCacheNull(config.isNeedCacheNull());
        String hotKeyCacheKeyCheckerClassName = config.getCacheKeyCheckerClassName();
        HotKeyCacheKeyChecker hotKeyCacheKeyChecker = config.getHotKeyCacheKeyChecker();
        if (hotKeyCacheKeyChecker != null) {
            logger.info("HotKeyCacheKeyChecker init success, class = {}", hotKeyCacheKeyChecker.getClass().getName());
        } else {
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
        }
        commandHotKeyCacheConfig.setHotKeyCacheKeyChecker(hotKeyCacheKeyChecker);
        commandHotKeyCacheConfig.setHotKeyCacheStatsCallbackIntervalSeconds(config.getHotKeyCacheStatsCallbackIntervalSeconds());
        String hotKeyCacheStatsCallbackClassName = config.getHotKeyCacheStatsCallbackClassName();
        HotKeyCacheStatsCallback hotKeyCacheStatsCallback = config.getHotKeyCacheStatsCallback();
        if (hotKeyCacheStatsCallback != null) {
            logger.info("HotKeyCacheStatsCallback init success, class = {}", hotKeyCacheStatsCallback.getClass().getName());
        } else {
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
        }
        commandHotKeyCacheConfig.setHotKeyCacheStatsCallback(hotKeyCacheStatsCallback);
        return commandHotKeyCacheConfig;
    }

    public static CommandBigKeyMonitorConfig initBigKeyMonitorConfig(CamelliaServerProperties serverProperties) {
        if (!serverProperties.isBigKeyMonitorEnable()) return null;
        CamelliaServerProperties.BigKeyMonitorConfig config = serverProperties.getBigKeyMonitorConfig();
        if (config == null) return null;
        BigKeyMonitorCallback bigKeyMonitorCallback = config.getBigKeyMonitorCallback();
        if (bigKeyMonitorCallback != null) {
            logger.info("BigKeyCallback init success, class = {}", bigKeyMonitorCallback.getClass().getName());
        } else {
            String bigKeyCallbackClassName = config.getBigKeyMonitorCallbackClassName();
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
        }
        return new CommandBigKeyMonitorConfig(config.getStringSizeThreshold(),
                config.getListSizeThreshold(), config.getZsetSizeThreshold(),
                config.getHashSizeThreshold(), config.getSetSizeThreshold(), bigKeyMonitorCallback);
    }

    public static ConverterConfig initConverterConfig(CamelliaServerProperties serverProperties) {
        if (!serverProperties.isConverterEnable()) return null;
        CamelliaServerProperties.ConverterConfig converterConfig = serverProperties.getConverterConfig();
        if (converterConfig == null) return null;
        ConverterConfig config = new ConverterConfig();
        KeyConverter keyConverter = converterConfig.getKeyConverter();
        if (keyConverter != null) {
            logger.info("KeyConverter init success, class = {}", keyConverter.getClass().getName());
        } else {
            String keyConverterClassName = converterConfig.getKeyConverterClassName();
            if (keyConverterClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(keyConverterClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(keyConverterClassName);
                    }
                    keyConverter = (KeyConverter) clazz.newInstance();
                    logger.info("KeyConverter init success, class = {}", keyConverterClassName);
                } catch (Exception e) {
                    logger.error("KeyConverter init error, class = {}", keyConverterClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
        config.setKeyConverter(keyConverter);

        StringConverter stringConverter = converterConfig.getStringConverter();
        if (stringConverter != null) {
            logger.info("StringConverter init success, class = {}", stringConverter.getClass().getName());
        } else {
            String stringConverterClassName = converterConfig.getStringConverterClassName();
            if (stringConverterClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(stringConverterClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(stringConverterClassName);
                    }
                    stringConverter = (StringConverter) clazz.newInstance();
                    logger.info("StringConverter init success, class = {}", stringConverterClassName);
                } catch (Exception e) {
                    logger.error("StringConverter init error, class = {}", stringConverterClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
        config.setStringConverter(stringConverter);

        ListConverter listConverter = converterConfig.getListConverter();
        if (listConverter != null) {
            logger.info("ListConverter init success, class = {}", listConverter.getClass().getName());
        } else {
            String listConverterClassName = converterConfig.getListConverterClassName();
            if (listConverterClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(listConverterClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(listConverterClassName);
                    }
                    listConverter = (ListConverter) clazz.newInstance();
                    logger.info("ListConverter init success, class = {}", listConverterClassName);
                } catch (Exception e) {
                    logger.error("ListConverter init error, class = {}", listConverterClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
        config.setListConverter(listConverter);

        SetConverter setConverter = converterConfig.getSetConverter();
        if (setConverter != null) {
            logger.info("SetConverter init success, class = {}", setConverter.getClass().getName());
        } else {
            String setConverterClassName = converterConfig.getSetConverterClassName();
            if (setConverterClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(setConverterClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(setConverterClassName);
                    }
                    setConverter = (SetConverter) clazz.newInstance();
                    logger.info("SetConverter init success, class = {}", setConverterClassName);
                } catch (Exception e) {
                    logger.error("SetConverter init error, class = {}", setConverterClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
        config.setSetConverter(setConverter);

        HashConverter hashConverter = converterConfig.getHashConverter();
        if (hashConverter != null) {
            logger.info("HashConverter init success, class = {}", hashConverter.getClass().getName());
        } else {
            String hashConverterClassName = converterConfig.getHashConverterClassName();
            if (hashConverterClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(hashConverterClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(hashConverterClassName);
                    }
                    hashConverter = (HashConverter) clazz.newInstance();
                    logger.info("HashConverter init success, class = {}", hashConverterClassName);
                } catch (Exception e) {
                    logger.error("HashConverter init error, class = {}", hashConverterClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
        config.setHashConverter(hashConverter);

        ZSetConverter zSetConverter = converterConfig.getzSetConverter();
        if (zSetConverter != null) {
            logger.info("ZSetConverter init success, class = {}", zSetConverter.getClass().getName());
        } else {
            String zsetConverterClassName = converterConfig.getZsetConverterClassName();
            if (zsetConverterClassName != null) {
                try {
                    Class<?> clazz;
                    try {
                        clazz = Class.forName(zsetConverterClassName);
                    } catch (ClassNotFoundException e) {
                        clazz = Thread.currentThread().getContextClassLoader().loadClass(zsetConverterClassName);
                    }
                    zSetConverter = (ZSetConverter) clazz.newInstance();
                    logger.info("ZSetConverter init success, class = {}", zsetConverterClassName);
                } catch (Exception e) {
                    logger.error("ZSetConverter init error, class = {}", zsetConverterClassName, e);
                    throw new CamelliaRedisException(e);
                }
            }
        }
        config.setzSetConverter(zSetConverter);
        return config;
    }
}
