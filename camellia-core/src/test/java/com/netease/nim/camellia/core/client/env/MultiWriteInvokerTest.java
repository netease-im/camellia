package com.netease.nim.camellia.core.client.env;

import com.netease.nim.camellia.core.model.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2026/10/20.
 */
public class MultiWriteInvokerTest {

    @Test
    public void shouldExposePrimaryIndexOnly() throws Throwable {
        List<Resource> resources = Arrays.asList(
                new Resource("hbase://write-0"),
                new Resource("hbase://write-1"),
                new Resource("hbase://write-2"));
        for (MultiWriteType type : MultiWriteType.values()) {
            ProxyEnv env = new ProxyEnv.Builder()
                    .multiWriteType(type)
                    .multiWriteAsyncExecPoolSize(2)
                    .build();
            List<Integer> primaryIndexes = new CopyOnWriteArrayList<>();
            List<Integer> allIndexes = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(resources.size());
            MultiWriteInvoker.invoke(env, resources, (resource, index) -> {
                allIndexes.add(index);
                if (MultiWriteContext.isPrimary()) {
                    primaryIndexes.add(index);
                }
                latch.countDown();
                return index;
            });
            Assert.assertTrue(type + " all invocations should finish", latch.await(5, TimeUnit.SECONDS));
            List<Integer> sortedIndexes = new ArrayList<>(allIndexes);
            Collections.sort(sortedIndexes);
            Assert.assertEquals(type + " all indexes should be invoked", Arrays.asList(0, 1, 2), sortedIndexes);
            Assert.assertEquals(type + " only primary(index=0) should write out-param",
                    Collections.singletonList(0), primaryIndexes);
            Assert.assertTrue(type + " caller context should be cleared", MultiWriteContext.isPrimary());
        }
    }

    @Test
    public void shouldTreatSingleResourceAsPrimary() throws Throwable {
        ProxyEnv env = new ProxyEnv.Builder().build();
        List<Boolean> primaryFlags = new CopyOnWriteArrayList<>();
        MultiWriteInvoker.invoke(env, Collections.singletonList(new Resource("hbase://write-0")), (resource, index) -> {
            primaryFlags.add(MultiWriteContext.isPrimary());
            return null;
        });
        Assert.assertEquals(Collections.singletonList(true), primaryFlags);
        Assert.assertTrue(MultiWriteContext.isPrimary());
    }
}
