package com.netease.nim.camellia.core.client.callback;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShardingConfig;
import com.netease.nim.camellia.core.client.annotation.ShardingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.client.env.ProxyEnv;
import com.netease.nim.camellia.core.client.hub.IProxyHub;
import com.netease.nim.camellia.core.util.CamelliaMapUtils;
import com.netease.nim.camellia.core.util.ExceptionUtils;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * Created by caojiajun on 2019/5/17.
 */
public class ShardingCallback<T> implements MethodInterceptor {

    private final IProxyHub<T> proxyHub;
    private ProxyEnv env = ProxyEnv.defaultProxyEnv();

    public ShardingCallback(IProxyHub<T> proxyHub, Class<T> clazz, ProxyEnv env) {
        this.proxyHub = proxyHub;
        for (Method method : clazz.getMethods()) {
            initCache(method);
        }
        if (env != null) {
            this.env = env;
        }
    }

    @Override
    public Object intercept(Object o, final Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        try {
            if (!isOpMethod(method)) {
                return methodProxy.invokeSuper(o, objects);
            }
            CollectionType collectionType = getCollectionType(method);

            if (collectionType == CollectionType.NOT_COLLECTION) {
                byte[][] key = parseShardingSimple(method, objects);
                T proxy = proxyHub.chooseProxy(key);
                return method.invoke(proxy, objects);
            } else {
                Map<T, List<Object[]>> proxyMap = new HashMap<>();
                Map<byte[][], Object[]> map;
                switch (collectionType) {
                    case LIST:
                        map = parseShardingListParam(method, objects);
                        break;
                    case SET:
                        map = parseShardingSetParam(method, objects);
                        break;
                    case Map:
                        map = parseShardingMapParam(method, objects);
                        break;
                    case ARRAY:
                        map = parseShardingArrayParam(method, objects);
                        break;
                    default:
                        throw new UnsupportedEncodingException("CollectionType only support List/Set/Map/Array");
                }
                for (Map.Entry<byte[][], Object[]> entry : map.entrySet()) {
                    byte[][] key = entry.getKey();
                    Object[] param = entry.getValue();
                    T proxy = proxyHub.chooseProxy(key);
                    List<Object[]> list = CamelliaMapUtils.computeIfAbsent(proxyMap, proxy, k -> new ArrayList<>());
                    list.add(param);
                }
                Map<T, Object[]> finalProxyMap;
                switch (collectionType) {
                    case LIST:
                        finalProxyMap = mergeProxyMapOfList(proxyMap, method, objects);
                        break;
                    case SET:
                        finalProxyMap = mergeProxyMapOfSet(proxyMap, method, objects);
                        break;
                    case Map:
                        finalProxyMap = mergeProxyMapOfMap(proxyMap, method, objects);
                        break;
                    case ARRAY:
                        finalProxyMap = mergeProxyMapOfArray(proxyMap, method, objects);
                        break;
                    default:
                        throw new UnsupportedEncodingException("CollectionType only support List/Set/Map/Array");
                }

                if (finalProxyMap.size() == 1) {
                    for (Map.Entry<T, Object[]> entry : finalProxyMap.entrySet()) {
                        T proxy = entry.getKey();
                        Object[] params = entry.getValue();
                        return method.invoke(proxy, params);
                    }
                    throw new RuntimeException("will not invoke here");
                } else {
                    if (env.isShardingConcurrentEnable()) {
                        final AtomicBoolean isInvokeError = new AtomicBoolean(false);
                        final Throwable[] invokeError = new Throwable[1];
                        List<Object> results = new ArrayList<>();
                        List<Future<Object>> futureList = new ArrayList<>();
                        for (Map.Entry<T, Object[]> entry : finalProxyMap.entrySet()) {
                            final T proxy = entry.getKey();
                            final Object[] params = entry.getValue();
                            Future<Object> future = env.getShardingConcurrentExec().submit(() -> {
                                try {
                                    return method.invoke(proxy, params);
                                } catch (Throwable e) {
                                    invokeError[0] = e;
                                    isInvokeError.set(true);
                                    throw e;
                                }
                            });
                            futureList.add(future);
                        }
                        for (Future<Object> future : futureList) {
                            if (isInvokeError.get() && invokeError[0] != null) {
                                throw invokeError[0];
                            }
                            Object result = future.get();
                            results.add(result);
                        }
                        return mergeResult(results, method);
                    } else {
                        List<Object> results = new ArrayList<>();
                        for (Map.Entry<T, Object[]> entry : finalProxyMap.entrySet()) {
                            T proxy = entry.getKey();
                            Object[] params = entry.getValue();
                            Object result = method.invoke(proxy, params);
                            results.add(result);
                        }
                        return mergeResult(results, method);
                    }
                }
            }
        } catch (Throwable e) {
            throw ExceptionUtils.onError(e);
        }
    }

    private Map<T, Object[]> mergeProxyMapOfArray(Map<T, List<Object[]>> proxyMap, Method method, Object[] objects) {
        Map<T, Object[]> finalProxyMap = new HashMap<>();
        int collectionShardingIndex = getShardingCollectionParamIndex(method);
        for (Map.Entry<T, List<Object[]>> entry : proxyMap.entrySet()) {
            T proxy = entry.getKey();
            List<Object[]> value = entry.getValue();
            Object[] param = copy(objects);

            Object arrayParam;
            String name = getShardingCollectionParamArrayParamType(method);
            switch (name) {
                case "java.lang.String":
                    String[] strArrayParam = new String[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        String[] obj = (String[]) (value.get(i)[collectionShardingIndex]);
                        strArrayParam[i] = obj[0];
                    }
                    arrayParam = strArrayParam;
                    break;
                case "java.lang.Long": {
                    Long[] longArrayParam = new Long[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        Long[] obj = (Long[]) (value.get(i)[collectionShardingIndex]);
                        longArrayParam[i] = obj[0];
                    }
                    arrayParam = longArrayParam;
                    break;
                }
                case "java.lang.Integer": {
                    Integer[] integerArrayParam = new Integer[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        Integer[] obj = (Integer[]) (value.get(i)[collectionShardingIndex]);
                        integerArrayParam[i] = obj[0];
                    }
                    arrayParam = integerArrayParam;
                    break;
                }
                case "java.lang.Byte": {
                    Byte[] integerArrayParam = new Byte[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        Byte[] obj = (Byte[]) (value.get(i)[collectionShardingIndex]);
                        integerArrayParam[i] = obj[0];
                    }
                    arrayParam = integerArrayParam;
                    break;
                }
                case "long": {
                    long[] longArrayParam = new long[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        long[] obj = (long[]) (value.get(i)[collectionShardingIndex]);
                        longArrayParam[i] = obj[0];
                    }
                    arrayParam = longArrayParam;
                    break;
                }
                case "int": {
                    int[] intArrayParam = new int[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        int[] obj = (int[]) (value.get(i)[collectionShardingIndex]);
                        intArrayParam[i] = obj[0];
                    }
                    arrayParam = intArrayParam;
                    break;
                }
                case "byte": {
                    byte[] byteArrayParam = new byte[value.size()];
                    for (int i = 0; i < value.size(); i++) {
                        byte[] obj = (byte[]) (value.get(i)[collectionShardingIndex]);
                        byteArrayParam[i] = obj[0];
                    }
                    arrayParam = byteArrayParam;
                    break;
                }
                case "[B": {
                    byte[][] byte2ArrayParam = new byte[value.size()][0];
                    for (int i = 0; i < value.size(); i++) {
                        byte[][] obj = (byte[][]) (value.get(i)[collectionShardingIndex]);
                        byte2ArrayParam[i] = obj[0];
                    }
                    arrayParam = byte2ArrayParam;
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unsupported array param type");
            }

            param[collectionShardingIndex] = arrayParam;
            finalProxyMap.put(proxy, param);
        }
        return finalProxyMap;
    }

    private Map<T, Object[]> mergeProxyMapOfSet(Map<T, List<Object[]>> proxyMap, Method method, Object[] objects) {
        Map<T, Object[]> finalProxyMap = new HashMap<>();
        int collectionShardingIndex = getShardingCollectionParamIndex(method);
        for (Map.Entry<T, List<Object[]>> entry : proxyMap.entrySet()) {
            T proxy = entry.getKey();
            List<Object[]> value = entry.getValue();
            Object[] param = copy(objects);
            Set<Object> set = new HashSet<>();
            for (Object[] obj : value) {
                set.addAll((Set) obj[collectionShardingIndex]);
            }
            param[collectionShardingIndex] = set;
            finalProxyMap.put(proxy, param);
        }
        return finalProxyMap;
    }

    private Map<T, Object[]> mergeProxyMapOfMap(Map<T, List<Object[]>> proxyMap, Method method, Object[] objects) {
        Map<T, Object[]> finalProxyMap = new HashMap<>();
        int collectionShardingIndex = getShardingCollectionParamIndex(method);
        for (Map.Entry<T, List<Object[]>> entry : proxyMap.entrySet()) {
            T proxy = entry.getKey();
            List<Object[]> value = entry.getValue();
            Object[] param = copy(objects);
            Map<Object, Object> map1 = new HashMap<>();
            for (Object[] obj : value) {
                map1.putAll((Map) obj[collectionShardingIndex]);
            }
            param[collectionShardingIndex] = map1;
            finalProxyMap.put(proxy, param);
        }
        return finalProxyMap;
    }

    private Map<T, Object[]> mergeProxyMapOfList(Map<T, List<Object[]>> proxyMap, Method method, Object[] objects) {
        Map<T, Object[]> finalProxyMap = new HashMap<>();
        int collectionShardingIndex = getShardingCollectionParamIndex(method);
        for (Map.Entry<T, List<Object[]>> entry : proxyMap.entrySet()) {
            T proxy = entry.getKey();
            List<Object[]> value = entry.getValue();
            Object[] param = copy(objects);
            List<Object> list = new ArrayList<>();
            for (Object[] obj : value) {
                list.addAll((List) obj[collectionShardingIndex]);
            }
            param[collectionShardingIndex] = list;
            finalProxyMap.put(proxy, param);
        }
        return finalProxyMap;
    }

    private Object mergeResult(List<Object> results, Method method) {
        if (results.size() == 1) return results.get(0);
        Class<?> returnType = method.getReturnType();
        if (Boolean.class.isAssignableFrom(returnType) || boolean.class.isAssignableFrom(returnType)) {
            for (Object result : results) {
                if (result == null) continue;
                if (!(boolean) result) {
                    return false;
                }
            }
            return true;
        } else if (Map.class.isAssignableFrom(returnType)){
            Map map = new HashMap();
            for (Object result : results) {
                if (result == null) continue;
                map.putAll((Map) result);
            }
            return map;
        } else if (Integer.class.isAssignableFrom(returnType) || int.class.isAssignableFrom(returnType)) {
            int ret = 0;
            for (Object result : results) {
                if (result == null) continue;
                ret += (int) result;
            }
            return ret;
        } else if (Long.class.isAssignableFrom(returnType) || long.class.isAssignableFrom(returnType)) {
            long ret = 0;
            for (Object result : results) {
                if (result == null) continue;
                ret += (long) result;
            }
            return ret;
        } else if (Set.class.isAssignableFrom(returnType)) {
            Set set = new HashSet();
            for (Object result : results) {
                if (result == null) continue;
                set.addAll((Set) result);
            }
            return set;
        } else if (List.class.isAssignableFrom(returnType)) {
            //注意，不保证List的顺序
            List list = new ArrayList();
            for (Object result : results) {
                if (result == null) continue;
                list.addAll((List) result);
            }
            return list;
        } else if (void.class.isAssignableFrom(returnType)) {
            return results.get(0);
        } else {
            return results.get(0);
        }
    }

    private Object[] copy(Object[] objects) {
        Object[] copy = new Object[objects.length];
        System.arraycopy(objects, 0, copy, 0, copy.length);
        return copy;
    }

    private Map<byte[][], Object[]> parseShardingArrayParam(Method method, Object[] objects) {
        List<Integer> shardingParamIndex = getShardingParamIndex(method);
        int shardingCollectionParamIndex = getShardingCollectionParamIndex(method);

        Map<byte[][], Object[]> map = new HashMap<>();

        List<Object> arrayParamList = new ArrayList<>();
        List<Object> arrayParamInnerList = new ArrayList<>();
        Object arrayParam = objects[shardingCollectionParamIndex];
        String name = getShardingCollectionParamArrayParamType(method);
        switch (name) {
            case "java.lang.String":
                String[] strArr = (String[]) arrayParam;
                for (String s : strArr) {
                    arrayParamList.add(new String[] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            case "java.lang.Integer":
                Integer[] integerArr = (Integer[]) arrayParam;
                for (Integer s : integerArr) {
                    arrayParamList.add(new Integer[] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            case "java.lang.Long":
                Long[] longArr = (Long[]) arrayParam;
                for (Long s : longArr) {
                    arrayParamList.add(new Long[] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            case "java.lang.Byte":
                Byte[] byteArr = (Byte[]) arrayParam;
                for (Byte s : byteArr) {
                    arrayParamList.add(new Byte[] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            case "int":
                int[] intArr2 = (int[]) arrayParam;
                for (int s : intArr2) {
                    arrayParamList.add(new int[] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            case "long":
                long[] longArr2 = (long[]) arrayParam;
                for (long s : longArr2) {
                    arrayParamList.add(new long[] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            case "byte":
                byte[] byteArr2 = (byte[]) arrayParam;
                for (byte s : byteArr2) {
                    arrayParamList.add(new byte[] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            case "[B":
                byte[][] byteArr3 = (byte[][]) arrayParam;
                for (byte[] s : byteArr3) {
                    arrayParamList.add(new byte[][] {s});
                    arrayParamInnerList.add(s);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported array param type");
        }
        for (int i=0; i<arrayParamList.size(); i++) {
            Object c = arrayParamList.get(i);
            List<byte[]> shardingKey = new ArrayList<>();
            byte[] prefix = getPrefix(method);
            if (prefix != null && prefix.length > 0) {
                shardingKey.add(prefix);
            }
            for (Integer index : shardingParamIndex) {
                if (index == shardingCollectionParamIndex) {
                    shardingKey.add(toBytes(arrayParamInnerList.get(i)));
                } else {
                    shardingKey.add(toBytes(objects[index]));
                }
            }
            byte[][] key = shardingKey.toArray(new byte[0][0]);
            Object[] param = new Object[objects.length];
            for (int j=0; j<objects.length; j++) {
                if (j != shardingCollectionParamIndex) {
                    param[j] = objects[j];
                } else {
                    param[j] = c;
                }
            }
            map.put(key, param);
        }
        return map;
    }

    private Map<byte[][], Object[]> parseShardingSetParam(Method method, Object[] objects) {
        List<Integer> shardingParamIndex = getShardingParamIndex(method);
        int shardingCollectionParamIndex = getShardingCollectionParamIndex(method);

        Map<byte[][], Object[]> map = new HashMap<>();

        Set setParam = (Set) objects[shardingCollectionParamIndex];
        for (Object c : setParam) {
            List<byte[]> shardingKey = new ArrayList<>();
            byte[] prefix = getPrefix(method);
            if (prefix != null && prefix.length > 0) {
                shardingKey.add(prefix);
            }
            for (Integer index : shardingParamIndex) {
                if (index == shardingCollectionParamIndex) {
                    shardingKey.add(toBytes(c));
                } else {
                    shardingKey.add(toBytes(objects[index]));
                }
            }
            byte[][] key = shardingKey.toArray(new byte[0][0]);
            Object[] param = new Object[objects.length];
            for (int i=0; i<objects.length; i++) {
                if (i != shardingCollectionParamIndex) {
                    param[i] = objects[i];
                } else {
                    Set<Object> set = new HashSet<>();
                    set.add(c);
                    param[i] = set;
                }
            }
            map.put(key, param);
        }
        return map;
    }

    private Map<byte[][], Object[]> parseShardingMapParam(Method method, Object[] objects) {
        List<Integer> shardingParamIndex = getShardingParamIndex(method);
        int shardingCollectionParamIndex = getShardingCollectionParamIndex(method);

        Map<byte[][], Object[]> map = new HashMap<>();

        Map mapParam = (Map) objects[shardingCollectionParamIndex];
        for (Object k : mapParam.keySet()) {
            List<byte[]> shardingKey = new ArrayList<>();
            byte[] prefix = getPrefix(method);
            if (prefix != null && prefix.length > 0) {
                shardingKey.add(prefix);
            }
            for (Integer index : shardingParamIndex) {
                if (index == shardingCollectionParamIndex) {
                    shardingKey.add(toBytes(k));
                } else {
                    shardingKey.add(toBytes(objects[index]));
                }
            }
            byte[][] key = shardingKey.toArray(new byte[0][0]);
            Object[] param = new Object[objects.length];
            for (int i=0; i<objects.length; i++) {
                if (i != shardingCollectionParamIndex) {
                    param[i] = objects[i];
                } else {
                    Map<Object, Object> map1 = new HashMap<>();
                    map1.put(k, mapParam.get(k));
                    param[i] = map1;
                }
            }
            map.put(key, param);
        }
        return map;
    }

    private Map<byte[][], Object[]> parseShardingListParam(Method method, Object[] objects) {
        List<Integer> shardingParamIndex = getShardingParamIndex(method);
        int shardingCollectionParamIndex = getShardingCollectionParamIndex(method);

        Map<byte[][], Object[]> map = new HashMap<>();

        List listParam = (List) objects[shardingCollectionParamIndex];
        for (Object c : listParam) {
            List<byte[]> shardingKey = new ArrayList<>();
            byte[] prefix = getPrefix(method);
            if (prefix != null && prefix.length > 0) {
                shardingKey.add(prefix);
            }
            for (Integer index : shardingParamIndex) {
                if (index == shardingCollectionParamIndex) {
                    shardingKey.add(toBytes(c));
                } else {
                    shardingKey.add(toBytes(objects[index]));
                }
            }
            byte[][] key = shardingKey.toArray(new byte[0][0]);
            Object[] param = new Object[objects.length];
            for (int i=0; i<objects.length; i++) {
                if (i != shardingCollectionParamIndex) {
                    param[i] = objects[i];
                } else {
                    List<Object> list = new ArrayList<>();
                    list.add(c);
                    param[i] = list;
                }
            }
            map.put(key, param);
        }
        return map;
    }

    private byte[][] parseShardingSimple(Method method, Object[] objects) {
        List<Integer> paramIndex = getShardingParamIndex(method);
        List<byte[]> shardingKey = new ArrayList<>();
        byte[] prefix = getPrefix(method);
        if (prefix != null && prefix.length > 0) {
            shardingKey.add(prefix);
        }
        for (Integer index : paramIndex) {
            shardingKey.add(toBytes(objects[index]));
        }
        return shardingKey.toArray(new byte[0][0]);
    }

    private byte[] toBytes(Object object) {
        if (object == null) return new byte[0];
        if (object instanceof byte[]) {
            return (byte[]) object;
        } else {
            return object.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    private final Map<Method, List<Integer>> shardingParamIndex = new HashMap<>();
    private final Map<Method, Integer> shardingCollectionParamIndex = new HashMap<>();
    private final Map<Method, CollectionType> collectionTypeMap = new HashMap<>();
    private final Map<Method, byte[]> prefixCache = new HashMap<>();
    private final Map<Method, Boolean> isOpMethodCache = new HashMap<>();
    private final Map<Method, String> shardingCollectionParamArrayParamType = new HashMap<>();

    private byte[] getPrefix(Method method) {
        byte[] prefix = prefixCache.get(method);
        if (prefix != null) return prefix;
        initCache(method);
        return prefixCache.get(method);
    }

    private String getShardingCollectionParamArrayParamType(Method method) {
        String name = shardingCollectionParamArrayParamType.get(method);
        if (name != null) return name;
        initCache(method);
        return shardingCollectionParamArrayParamType.get(method);
    }

    private CollectionType getCollectionType(Method method) {
        CollectionType collectionType = collectionTypeMap.get(method);
        if (collectionType != null) return collectionType;
        initCache(method);
        return collectionTypeMap.get(method);
    }

    private int getShardingCollectionParamIndex(Method method) {
        Integer index = shardingCollectionParamIndex.get(method);
        if (index != null) return index;
        initCache(method);
        return shardingCollectionParamIndex.get(method);
    }

    private List<Integer> getShardingParamIndex(Method method) {
        List<Integer> index = shardingParamIndex.get(method);
        if (index != null) return index;
        initCache(method);
        return shardingParamIndex.get(method);
    }

    private ShardingParam getShardingParam(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (ShardingParam.class.isAssignableFrom(annotation.annotationType())) {
                return (ShardingParam) annotation;
            }
        }
        return null;
    }

    private boolean isOpMethod(Method method) {
        Boolean cache = isOpMethodCache.get(method);
        if (cache != null) return cache;
        cache = method.getAnnotation(WriteOp.class) != null || method.getAnnotation(ReadOp.class) != null;
        isOpMethodCache.put(method, cache);
        return cache;
    }

    private void initCache(Method method) {
        if (!isOpMethod(method)) return;
        int shardingCollectionParamIndexCount = 0;
        List<Integer> paramIndex = new ArrayList<>();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i=0; i<parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            ShardingParam shardingParam = getShardingParam(annotations);
            if (shardingParam != null) {
                if (shardingParam.type() == ShardingParam.Type.Collection) {
                    shardingCollectionParamIndexCount ++;
                    shardingCollectionParamIndex.put(method, i);
                    if (List.class.isAssignableFrom(parameterTypes[i])) {
                        collectionTypeMap.put(method, CollectionType.LIST);
                    } else if (Set.class.isAssignableFrom(parameterTypes[i])) {
                        collectionTypeMap.put(method, CollectionType.SET);
                    } else if (Map.class.isAssignableFrom(parameterTypes[i])) {
                        collectionTypeMap.put(method, CollectionType.Map);
                    } else if (parameterTypes[i].isArray()) {
                        collectionTypeMap.put(method, CollectionType.ARRAY);
                        String name = parameterTypes[i].getComponentType().getName();
                        shardingCollectionParamArrayParamType.put(method, name);
                    } else {
                        throw new UnsupportedOperationException("collection type param only support List/Set/Map/Array");
                    }
                }
                paramIndex.add(i);
            }
        }
        if (shardingCollectionParamIndexCount > 1) {
            throw new UnsupportedOperationException("only support one param is collection type");
        }
        shardingParamIndex.put(method, paramIndex);
        if (!collectionTypeMap.containsKey(method)) {
            collectionTypeMap.put(method, CollectionType.NOT_COLLECTION);
        }

        byte[] prefix = null;
        ShardingConfig shardingConfig = method.getAnnotation(ShardingConfig.class);
        if (shardingConfig == null) {
            shardingConfig = method.getDeclaringClass().getAnnotation(ShardingConfig.class);
        }
        if (shardingConfig != null) {
            String prefixStr = shardingConfig.prefix();
            if (prefixStr.length() != 0) {
                prefix = prefixStr.getBytes(StandardCharsets.UTF_8);
            }
        }
        if (prefix == null) {
            prefix = new byte[0];
        }
        prefixCache.put(method, prefix);
    }

    private enum CollectionType {
        NOT_COLLECTION,
        LIST,
        SET,
        ARRAY,
        Map,
        ;
    }
}
