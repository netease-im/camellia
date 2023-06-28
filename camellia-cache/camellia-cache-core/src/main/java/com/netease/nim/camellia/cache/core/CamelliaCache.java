package com.netease.nim.camellia.cache.core;

import com.netease.nim.camellia.cache.core.boot.CamelliaCacheThreadLocal;
import com.netease.nim.camellia.cache.core.exception.CamelliaCacheException;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.cache.Cache;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;

public class CamelliaCache implements Cache {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaCache.class);

    private final INativeCache nativeCache;
    private final ICamelliaCacheConfig cacheConfig;
    private final CamelliaCachePrefixGetter cachePrefixGetter;

    public CamelliaCache(ICamelliaCacheConfig cacheConfig) {
        this.nativeCache = cacheConfig.getNativeCache();
        this.cacheConfig = cacheConfig;
        this.cachePrefixGetter = cacheConfig.getCachePrefixGetter();
    }

    @Override
    public String getName() {
        return this.cacheConfig.getName();
    }

    @Override
    public ValueWrapper get(Object key) {
        if (!CamelliaCacheEnv.enable) return null;
        String cachePrefix = getCachePrefix();
        return _get(cachePrefix, key);
    }

    private ValueWrapper _get(String cachePrefix, Object key) {
        if (isMultiGetActive(key)) {
            Object result = multiGet(cachePrefix, key);
            return () -> result;
        }
        String cacheKey = buildCacheKey(cachePrefix, key);
        Object result = nativeCache.get(cacheKey);
        if (logger.isDebugEnabled()) {
            logger.debug("CamelliaCache get, key = {}", cacheKey);
        }
        if (result == null) return null;
        if (result instanceof NullCache) {
            return () -> null;
        }
        return () -> result;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        if (!CamelliaCacheEnv.enable) return null;
        String cachePrefix = getCachePrefix();
        if (isMultiGetActive(key)) {
            return multiGet(cachePrefix, key);
        }
        String cacheKey = buildCacheKey(cachePrefix, key);
        Object value = nativeCache.get(cacheKey);
        if (logger.isDebugEnabled()) {
            logger.debug("CamelliaCache get, key = {}, class = {}", cacheKey, type);
        }
        if (value != null && !type.isInstance(value)) {
            nativeCache.delete(cacheKey);
            throw new CamelliaCacheException("CamelliaCache value is not of required type [" + type.getName() + "]: " + value);
        }
        if (value instanceof NullCache) {
            return null;
        }
        return (T) value;
    }

    @Override
    public void evict(Object key) {
        if (!CamelliaCacheEnv.enable) return;
        String cachePrefix = getCachePrefix();
        if (isMultiEvictActive(key)) {
            multiEvict(cachePrefix, key);
            return;
        }
        String cacheKey = buildCacheKey(cachePrefix, key);
        nativeCache.delete(cacheKey);
        if (logger.isDebugEnabled()) {
            logger.debug("CamelliaCache evict, key = {}", cacheKey);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (!CamelliaCacheEnv.enable) return;
        String cachePrefix = getCachePrefix();
        _put(cachePrefix, key, value);
    }

    private void _put(String cachePrefix, Object key, Object value) {
        if (value == null) {
            if (cacheConfig.isCacheNull()) {
                value = NullCache.INSTANCE;
            } else {
                return;
            }
        }
        String cacheKey = buildCacheKey(cachePrefix, key);
        long expireMillis = cacheConfig.getExpireMillis();
        if (expireMillis > 0) {
            nativeCache.put(cacheKey, value, expireMillis);
            if (logger.isDebugEnabled()) {
                logger.debug("CamelliaCache put, key = {}, expireMillis = {}", cacheKey, expireMillis);
            }
        } else {
            nativeCache.put(cacheKey, value);
            if (logger.isDebugEnabled()) {
                logger.debug("CamelliaCache put, key = {}, no expire", cacheKey);
            }
        }
    }

    private String getCachePrefix() {
        if (cachePrefixGetter == null) return null;
        return cachePrefixGetter.get();
    }

    private String buildCacheKey(String cachePrefix, Object key) {
        if (cachePrefix != null) {
            return cachePrefix + key;
        } else {
            return String.valueOf(key);
        }
    }

    private String deBuildCacheKey(String cachePrefix, String cacheKey) {
        if (cachePrefix != null) {
            return cacheKey.substring(cachePrefix.length());
        } else {
            return cacheKey;
        }
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        if (!CamelliaCacheEnv.enable) {
            try {
                return valueLoader.call();
            } catch (Exception e1) {
                logger.error("valueLoader call error", e1);
                throw new CamelliaCacheException(e1);
            }
        }
        String cachePrefix = getCachePrefix();
        try {
            ValueWrapper value = _get(cachePrefix, key);
            if (value != null) {
                return (T) value.get();
            }

            if (isMultiGetActive(key)) {
                return multiGet(cachePrefix, key);
            }
        } catch (Exception e) {
            logger.error("get of valueLoader error", e);
            try {
                return valueLoader.call();
            } catch (Exception e1) {
                logger.error("valueLoader call error", e1);
                throw new CamelliaCacheException(e1);
            }
        }

        String cacheKey = buildCacheKey(cachePrefix, key);
        String lockKey = cacheKey + "~lock";
        try {
            T result;
            long expireMillis = CamelliaCacheEnv.syncLoadExpireMillis;
            long sleepMillis = CamelliaCacheEnv.syncLoadSleepMillis;
            int retry = CamelliaCacheEnv.syncLoadMaxRetry;
            boolean lock = nativeCache.acquireLock(lockKey, expireMillis);
            if (logger.isDebugEnabled()) {
                logger.debug("CamelliaCache sync get, key = {}, lock = {}", cacheKey, lock);
            }
            if (lock) {
                result = valueLoader.call();
                _put(cachePrefix, key, result);
                nativeCache.releaseLock(lockKey);
                return result;
            } else {
                while (retry -- > 0) {
                    ValueWrapper valueWrapper = _get(cachePrefix, key);
                    if (valueWrapper != null) {
                        return (T) valueWrapper.get();
                    }
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                result = valueLoader.call();
                _put(cachePrefix, key, result);
                return result;
            }
        } catch (Exception e) {
            logger.error("get of valueLoader error", e);
            try {
                nativeCache.delete(cacheKey);
                nativeCache.releaseLock(lockKey);
            } catch (Exception e1) {
                logger.error("get of valueLoader error to delete cacheKey/lockKey error, key = {}", cacheKey, e1);
            }
            try {
                return valueLoader.call();
            } catch (Exception e1) {
                logger.error("valueLoader call error", e1);
                throw new CamelliaCacheException(e1);
            }
        }
    }


    private boolean isMultiEvictActive(Object key) {
        String k = String.valueOf(key);
        if (!k.startsWith(Constants.MEVICT)) {
            return false;
        }
        String[] split = k.split("\\|");
        if (split.length < 3) return false;
        if (!split[2].contains("#")) return false;
        int listIndex;
        try {
            listIndex = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        MethodInvocation invocation = CamelliaCacheThreadLocal.invocationThreadLocal.get();
        if (invocation == null) return false;
        if (!(invocation instanceof ReflectiveMethodInvocation)) {
            return false;
        }
        Class<?>[] parameterTypes = invocation.getMethod().getParameterTypes();
        if (parameterTypes.length <= listIndex) {
            return false;
        }
        if (!List.class.isAssignableFrom(parameterTypes[listIndex])) {
            return false;
        }
        return true;
    }


    private boolean isMultiGetActive(Object key) {
        String k = String.valueOf(key);
        if (!k.startsWith(Constants.MGET)) {
            return false;
        }
        String[] split = k.split("\\|");
        if (split.length < 4) return false;
        if (!split[2].contains("#")) return false;
        if (!split[3].contains("#")) return false;
        int listIndex;
        try {
            listIndex = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        MethodInvocation invocation = CamelliaCacheThreadLocal.invocationThreadLocal.get();
        if (invocation == null) return false;
        if (!(invocation instanceof ReflectiveMethodInvocation)) {
            return false;
        }
        Class<?>[] parameterTypes = invocation.getMethod().getParameterTypes();
        if (parameterTypes.length <= listIndex) {
            return false;
        }
        if (!List.class.isAssignableFrom(parameterTypes[listIndex])) {
            return false;
        }
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (!List.class.isAssignableFrom(returnType)) {
            return false;
        }
        return true;
    }

    //使用|分隔，mget是标识符，之后是数字，之后是mget的key的拼装规则，之后是缓存回填的key的拼装规则
    //2表示第三个参数是List（0开始算）
    //#.*表示List中的元素，#.id表示List中的元素的id字段
    //第一组的#表示参数中的List，第二组的#表示返回的List
    //mget|2|(xxxxx)(#.*)(yyyy)(#.id)|(xxxxx)(#.name)(yyyy)(#.id)
    private <T> T multiGet(String cachePrefix, Object key) {
        try {
            String k = String.valueOf(key);
            String[] split = k.split("\\|");
            Integer listIndex = Integer.parseInt(split[1]);
            String getExpression = split[2];//mget表达式，根据该表达式生成一组key，去cache中批量拿
            String putExpression = split[3];//mput表达式，cache没有命中的情况下，穿透到DB，查询结果回调cache

            ReflectiveMethodInvocation invocation = (ReflectiveMethodInvocation) CamelliaCacheThreadLocal.invocationThreadLocal.get();
            Object[] arguments = invocation.getArguments();
            List listObj = (List) arguments[listIndex];
            //生成一组key
            List<String> toGetKeys = assembleCacheKeyList(cachePrefix, getExpression, listObj);

            //返回
            List<Object> result = new ArrayList<>();

            if (logger.isDebugEnabled()) {
                logger.debug("CamelliaCache multiGet, keys = {}", toGetKeys);
            }
            if (toGetKeys.isEmpty()) return (T) result;

            List<Object> missIds = new ArrayList<>();
            Set<String> missKeys = new HashSet<>();

            //cache中批量拿，分批拿
            List<Object> cacheResult = new ArrayList<>();
            List<List<String>> keysList = splitKeys(toGetKeys, CamelliaCacheEnv.multiOpBatchSize);
            for (List<String> keys : keysList) {
                List<Object> subCacheResult = nativeCache.multiGet(keys);
                cacheResult.addAll(subCacheResult);
            }

            for (int i=0; i < listObj.size(); i++) {
                Object obj = cacheResult.get(i);
                if (obj != null) {//拿到了说明命中
                    if (!(obj instanceof NullCache)) {
                        result.add(obj);
                    }
                } else {//没拿到，计入miss
                    Object id = listObj.get(i);
                    missIds.add(id);
                    missKeys.add(toGetKeys.get(i));
                }
            }
            if (!missIds.isEmpty()) {
                //如果miss不为空，篡改参数，发起db查询
                invocation.getArguments()[listIndex] = missIds;
                Object proceed = invocation.proceed();
                List dbResult = (List) proceed;
                if (!dbResult.isEmpty()) {
                    //db查询结果放到返回中
                    result.addAll(dbResult);
                }
                Map<String, Object> batchPutKV = new HashMap<>();
                //db查询结果，回填到cache
                List<String> toPutKeys = assembleCacheKeyList(cachePrefix, putExpression, dbResult);
                for (int i=0; i<toPutKeys.size(); i++) {
                    batchPutKV.put(toPutKeys.get(i), dbResult.get(i));
                }
                //db查询不到的，回填null到cache
                if (missIds.size() > dbResult.size()) {
                    toPutKeys.forEach(missKeys::remove);
                    for (String missKey : missKeys) {
                        if (cacheConfig.isCacheNull()) {
                            batchPutKV.put(deBuildCacheKey(cachePrefix, missKey), NullCache.INSTANCE);
                        }
                    }
                }
                //写到缓存
                if (!batchPutKV.isEmpty()) {
                    if (cacheConfig.getExpireMillis() > 0) {
                        nativeCache.multiPut(batchPutKV, cacheConfig.getExpireMillis());
                    } else {
                        nativeCache.multiPut(batchPutKV);
                    }
                }
            }
            return (T) result;
        } catch (Throwable t) {
            logger.error("multiGet error", t);
            throw new CamelliaCacheException(t);
        }
    }

    //1表示第二个参数是List
    //#.*表示List中的元素，#.id表示List中的元素的id字段
    //mevict|1|(xxxxx)(#.*)(yyyy)(#.id)
    private void multiEvict(String cachePrefix, Object key) {
        try {
            String k = String.valueOf(key);
            String[] split = k.split("\\|");
            int listIndex = Integer.parseInt(split[1]);
            String expression = split[2];//表达式

            ReflectiveMethodInvocation invocation = (ReflectiveMethodInvocation) CamelliaCacheThreadLocal.invocationThreadLocal.get();

            Object[] arguments = invocation.getArguments();

            List listObj = (List) arguments[listIndex];

            //生成一组key
            List<String> toEvictKeys = assembleCacheKeyList(cachePrefix, expression, listObj);

            if (logger.isDebugEnabled()) {
                logger.debug("CamelliaCache multiEvict, keys = {}", toEvictKeys);
            }
            //批量删除cache，分批删
            if (!toEvictKeys.isEmpty()) {
                List<List<String>> keysList = splitKeys(toEvictKeys, CamelliaCacheEnv.multiOpBatchSize);
                for (List<String> keys : keysList) {
                    nativeCache.multiDelete(keys);
                }
            }
        } catch (Throwable t) {
            logger.error("multiEvict error", t);
            throw new CamelliaCacheException(t);
        }
    }

    //(xxxxx)(#.*)(yyyy)(#.id)
    private List<String> assembleCacheKeyList(String cachePrefix, String expression, List list) throws NoSuchFieldException, IllegalAccessException {
        List<String> items = parseBracket(expression);
        List<String> keys = new ArrayList<>();
        for (Object obj : list) {
            StringBuilder key = new StringBuilder("");
            for (String item : items) {
                if (item.startsWith("#.")) {
                    String keyField = item.substring(2);
                    if (keyField.equals("*")) {
                        key.append(obj);
                    } else {
                        Object value = getValue(obj, keyField);
                        key.append(value);
                    }
                } else {
                    key.append(item);
                }
            }
            keys.add(buildCacheKey(cachePrefix, key.toString()));
        }
        return keys;
    }

    //把(xx)(yy)(zz)解析成[xx,yy,zz]，此处会做严格的语法检查
    private List<String> parseBracket(String expression) {
        List<String> result = new ArrayList<>();
        StringBuilder item = new StringBuilder();
        char startChar = expression.charAt(0);
        if (startChar != '(') {
            throw new CamelliaCacheException("not start with '('");
        }
        boolean inner = false;
        for (int i=0; i<expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                if (inner) {
                    throw new CamelliaCacheException("duplicate '('");
                }
                item = new StringBuilder();
                inner = true;
            } else if (c == ')') {
                if (!inner) {
                    throw new CamelliaCacheException("duplicate ')'");
                }
                if (item.length() > 0) {
                    result.add(item.toString());
                    item = new StringBuilder();
                }
                inner = false;
            } else {
                if (!inner) {
                    throw new CamelliaCacheException("missing '('");
                }
                item.append(c);
            }
        }
        if (item.length() > 0) {
            throw new CamelliaCacheException("missing ')'");
        }
        return result;
    }

    //增加一个本地缓存，提高一点性能
    private final Map<Class<?>, Map<String, Field>> fieldCache = new HashMap<>();
    private Object getValue(Object obj, String keyField) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Map<String, Field> subMap = fieldCache.get(clazz);
        Field field;
        if (subMap != null) {
            field = subMap.get(keyField);
            if (field != null) {
                return field.get(obj);
            }
        }
        if (subMap == null) {
            subMap = new HashMap<>();
            fieldCache.put(clazz, subMap);
        }
        field = clazz.getDeclaredField(keyField);
        field.setAccessible(true);
        subMap.put(keyField, field);
        return field.get(obj);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        if (!CamelliaCacheEnv.enable) return null;
        String cachePrefix = getCachePrefix();
        ValueWrapper wrapper = _get(cachePrefix, key);
        if (wrapper == null) {
            _put(cachePrefix, key, value);
            return null;
        } else {
            return wrapper;
        }
    }

    @Override
    public Object getNativeCache() {
        return nativeCache;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private static List<List<String>> splitKeys(List<String> collection, int splitSize) {
        if (splitSize <= 0) splitSize = 500;
        if (collection == null) return Collections.emptyList();
        if (collection.isEmpty()) return Collections.emptyList();
        List<List<String>> res = new ArrayList<>();
        if (collection.size() < splitSize) {
            res.add(new ArrayList<>(collection));
        } else {
            List<String> tmp = new ArrayList<>();
            for (String t : collection) {
                tmp.add(t);
                if (tmp.size() == splitSize) {
                    res.add(tmp);
                    tmp = new ArrayList<>();
                }
            }
            if (!tmp.isEmpty()) {
                res.add(tmp);
            }
        }
        return res;
    }
}

