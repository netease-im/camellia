package com.netease.nim.camellia.core.util;

import com.netease.nim.camellia.core.client.annotation.ReadOp;
import com.netease.nim.camellia.core.client.annotation.WriteOp;
import org.junit.Assert;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by caojiajun on 2026/4/29
 */
public class ReadWriteOperationCacheTest {

    @Test
    public void shouldDefineWriteOpAndReadOpAsRuntimeMethodAnnotations() {
        Assert.assertEquals(RetentionPolicy.RUNTIME, WriteOp.class.getAnnotation(Retention.class).value());
        Assert.assertEquals(RetentionPolicy.RUNTIME, ReadOp.class.getAnnotation(Retention.class).value());
        Assert.assertTrue(Arrays.asList(WriteOp.class.getAnnotation(Target.class).value()).contains(ElementType.METHOD));
        Assert.assertTrue(Arrays.asList(ReadOp.class.getAnnotation(Target.class).value()).contains(ElementType.METHOD));
    }

    @Test
    public void shouldDetectWriteOpMethod() throws Exception {
        ReadWriteOperationCache cache = new ReadWriteOperationCache();
        Method method = method("write", String.class);

        Assert.assertNotNull(method.getAnnotation(WriteOp.class));
        Assert.assertEquals(ReadWriteOperationCache.WRITE, cache.getOperationType(method));
        Assert.assertEquals("write(String)", cache.getMethodName(method));
    }

    @Test
    public void shouldDetectReadOpMethod() throws Exception {
        ReadWriteOperationCache cache = new ReadWriteOperationCache();
        Method method = method("read", String.class);

        Assert.assertNotNull(method.getAnnotation(ReadOp.class));
        Assert.assertEquals(ReadWriteOperationCache.READ, cache.getOperationType(method));
        Assert.assertEquals("read(String)", cache.getMethodName(method));
    }

    @Test
    public void shouldReturnUnknownForUnannotatedAndNullMethod() throws Exception {
        ReadWriteOperationCache cache = new ReadWriteOperationCache();

        Assert.assertEquals(ReadWriteOperationCache.UNKNOWN, cache.getOperationType(method("unknown")));
        Assert.assertEquals(ReadWriteOperationCache.UNKNOWN, cache.getOperationType(null));
    }

    @Test
    public void shouldPreferWriteOpWhenMethodHasBothReadAndWriteAnnotations() throws Exception {
        ReadWriteOperationCache cache = new ReadWriteOperationCache();

        Assert.assertEquals(ReadWriteOperationCache.WRITE, cache.getOperationType(method("readAndWrite")));
    }

    @Test
    public void shouldPreheatOperationAndMethodNameCaches() throws Exception {
        ReadWriteOperationCache cache = new ReadWriteOperationCache();

        cache.preheat(AnnotatedService.class);

        Assert.assertEquals(ReadWriteOperationCache.WRITE, cache.getOperationType(method("write", String.class)));
        Assert.assertEquals(ReadWriteOperationCache.READ, cache.getOperationType(method("read", String.class)));
        Assert.assertEquals("readAndWrite()", cache.getMethodName(method("readAndWrite")));
    }

    private Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return AnnotatedService.class.getMethod(name, parameterTypes);
    }

    public static class AnnotatedService {
        @WriteOp
        public void write(String key) {
        }

        @ReadOp
        public String read(String key) {
            return key;
        }

        @WriteOp
        @ReadOp
        public void readAndWrite() {
        }

        public void unknown() {
        }
    }
}
