package com.netease.nim.camellia.core.client.callback;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.ShardingParam;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import com.netease.nim.camellia.core.client.hub.IProxyHub;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by caojiajun on 2026/4/29
 */
public class ShardingParamAnnotationTest {

    @Test
    public void shouldDefineShardingParamAsRuntimeParameterAnnotation() {
        Assert.assertEquals(RetentionPolicy.RUNTIME, ShardingParam.class.getAnnotation(Retention.class).value());
        Assert.assertTrue(Arrays.asList(ShardingParam.class.getAnnotation(Target.class).value()).contains(ElementType.PARAMETER));
    }

    @Test
    public void shouldExposeDefaultSimpleTypeAndExplicitCollectionType() throws Exception {
        ShardingParam simple = shardingParam(method("write", String.class, String.class), 0);
        ShardingParam collection = shardingParam(method("writeList", List.class, Integer.class), 0);

        Assert.assertEquals(ShardingParam.Type.Simple, simple.type());
        Assert.assertEquals(ShardingParam.Type.Collection, collection.type());
    }

    @Test
    public void shouldFindShardingParamIndexesOnlyForReadOrWriteMethods() throws Exception {
        ShardingCallback<AnnotatedService> callback = callback();

        Assert.assertEquals(Arrays.asList(0, 1),
                invoke(callback, "getShardingParamIndex", new Class<?>[] {Method.class},
                        method("writeWithTwoKeys", String.class, Integer.class, String.class)));
        Assert.assertEquals(Arrays.asList(1),
                invoke(callback, "getShardingParamIndex", new Class<?>[] {Method.class},
                        method("read", String.class, byte[].class)));
        Assert.assertNull(invoke(callback, "getShardingParamIndex", new Class<?>[] {Method.class},
                method("notOp", String.class)));
    }

    @Test
    public void shouldParseSimpleShardingParamsInMethodParameterOrder() throws Exception {
        ShardingCallback<AnnotatedService> callback = callback();
        Method method = method("writeWithTwoKeys", String.class, Integer.class, String.class);

        byte[][] key = invoke(callback, "parseShardingSimple",
                new Class<?>[] {Method.class, Object[].class},
                method, new Object[] {"namespace", 100, "value"});

        Assert.assertEquals(2, key.length);
        Assert.assertArrayEquals(bytes("namespace"), key[0]);
        Assert.assertArrayEquals(bytes("100"), key[1]);
    }

    @Test
    public void shouldPreserveByteArrayShardingParam() throws Exception {
        ShardingCallback<AnnotatedService> callback = callback();
        Method method = method("read", String.class, byte[].class);
        byte[] rawKey = new byte[] {1, 2, 3};

        byte[][] key = invoke(callback, "parseShardingSimple",
                new Class<?>[] {Method.class, Object[].class},
                method, new Object[] {"namespace", rawKey});

        Assert.assertEquals(1, key.length);
        Assert.assertSame(rawKey, key[0]);
    }

    @Test
    public void shouldDetectCollectionShardingParamTypes() throws Exception {
        ShardingCallback<AnnotatedService> callback = callback();

        assertCollectionType(callback, "LIST", method("writeList", List.class, Integer.class));
        assertCollectionType(callback, "SET", method("writeSet", Set.class));
        assertCollectionType(callback, "Map", method("writeMap", Map.class));
        assertCollectionType(callback, "ARRAY", method("writeArray", String[].class));
    }

    @Test
    public void shouldSplitListCollectionShardingParamIntoSingleItemCalls() throws Exception {
        ShardingCallback<AnnotatedService> callback = callback();
        Method method = method("writeList", List.class, Integer.class);

        Map<byte[][], Object[]> split = invoke(callback, "parseShardingListParam",
                new Class<?>[] {Method.class, Object[].class},
                method, new Object[] {Arrays.asList("key1", "key2"), 100});

        Assert.assertEquals(2, split.size());
        Set<String> keys = new HashSet<>();
        for (Map.Entry<byte[][], Object[]> entry : split.entrySet()) {
            byte[][] shardingKey = entry.getKey();
            Object[] params = entry.getValue();
            keys.add(new String(shardingKey[0], StandardCharsets.UTF_8));
            Assert.assertArrayEquals(bytes("100"), shardingKey[1]);
            Assert.assertEquals(100, params[1]);
            Assert.assertTrue(params[0] instanceof List);
            Assert.assertEquals(1, ((List<?>) params[0]).size());
        }
        Assert.assertEquals(new HashSet<>(Arrays.asList("key1", "key2")), keys);
    }

    @Test
    public void shouldRejectMoreThanOneCollectionShardingParam() {
        try {
            new ShardingCallback<InvalidTwoCollectionsService>(proxyHub(), InvalidTwoCollectionsService.class, null);
            Assert.fail("multiple collection sharding params should be rejected");
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals("only support one param is collection type", e.getMessage());
        }
    }

    @Test
    public void shouldRejectUnsupportedCollectionShardingParamType() {
        try {
            new ShardingCallback<InvalidCollectionTypeService>(proxyHub(), InvalidCollectionTypeService.class, null);
            Assert.fail("unsupported collection sharding param should be rejected");
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals("collection type param only support List/Set/Map/Array", e.getMessage());
        }
    }

    private void assertCollectionType(ShardingCallback<AnnotatedService> callback, String expected, Method method) throws Exception {
        Object collectionType = invoke(callback, "getCollectionType", new Class<?>[] {Method.class}, method);
        Assert.assertEquals(expected, String.valueOf(collectionType));
        Assert.assertEquals(0, (int) invoke(callback, "getShardingCollectionParamIndex",
                new Class<?>[] {Method.class}, method));
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(ShardingCallback<?> callback, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = ShardingCallback.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        try {
            return (T) method.invoke(callback, args);
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            if (target instanceof Exception) {
                throw (Exception) target;
            }
            throw e;
        }
    }

    private ShardingParam shardingParam(Method method, int parameterIndex) {
        for (Annotation annotation : method.getParameterAnnotations()[parameterIndex]) {
            if (annotation instanceof ShardingParam) {
                return (ShardingParam) annotation;
            }
        }
        return null;
    }

    private Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return AnnotatedService.class.getMethod(name, parameterTypes);
    }

    private ShardingCallback<AnnotatedService> callback() {
        return new ShardingCallback<>(proxyHub(), AnnotatedService.class, null);
    }

    private <T> IProxyHub<T> proxyHub() {
        return key -> null;
    }

    private byte[] bytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    public interface AnnotatedService {
        @WriteOp
        void write(@ShardingParam String key, String value);

        @ReadOp
        String read(String namespace, @ShardingParam byte[] key);

        @WriteOp
        void writeWithTwoKeys(@ShardingParam String namespace, @ShardingParam Integer id, String value);

        void notOp(@ShardingParam String key);

        @WriteOp
        void writeList(@ShardingParam(type = ShardingParam.Type.Collection) List<String> keys,
                       @ShardingParam Integer tenant);

        @WriteOp
        void writeSet(@ShardingParam(type = ShardingParam.Type.Collection) Set<String> keys);

        @WriteOp
        void writeMap(@ShardingParam(type = ShardingParam.Type.Collection) Map<String, String> values);

        @WriteOp
        void writeArray(@ShardingParam(type = ShardingParam.Type.Collection) String[] keys);
    }

    public interface InvalidTwoCollectionsService {
        @WriteOp
        void write(@ShardingParam(type = ShardingParam.Type.Collection) List<String> keys,
                   @ShardingParam(type = ShardingParam.Type.Collection) Set<String> groups);
    }

    public interface InvalidCollectionTypeService {
        @WriteOp
        void write(@ShardingParam(type = ShardingParam.Type.Collection) Iterable<String> keys);
    }
}
