package com.netease.nim.camellia.external.call.samples;

import com.netease.nim.camellia.external.call.client.CamelliaExternalCallLocalClient;
import com.netease.nim.camellia.external.call.common.CamelliaExternalCallInvoker;
import com.netease.nim.camellia.tools.executor.CamelliaDynamicIsolationExecutor;
import com.netease.nim.camellia.tools.utils.SysUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2023/3/2
 */
public class TestSync {

    public static void main(String[] args) {
        test();
    }

    private static void test() {
        CamelliaDynamicIsolationExecutor executor = new CamelliaDynamicIsolationExecutor("test", SysUtils.getCpuNum() * 2);
        CamelliaExternalCallInvoker<String, String> invoker = TestSync::invoke;
        CamelliaExternalCallLocalClient<String, String> client = new CamelliaExternalCallLocalClient<>(executor, invoker);

        CompletableFuture<String> future1 = client.submit("tenant_1", UUID.randomUUID().toString());
        future1.thenAccept(s -> System.out.println("result1=" + s));
        future1.exceptionally(throwable -> {
            System.out.println("error1=" + throwable.toString());
            return null;
        });

        CompletableFuture<String> future2 = client.submit("tenant_3", UUID.randomUUID().toString());
        future2.thenAccept(s -> System.out.println("result2=" + s));
        future2.exceptionally(throwable -> {
            System.out.println("error2=" + throwable.toString());
            return null;
        });
    }

    private static String invoke(String isolationKey, String request) throws InterruptedException {
        if (isolationKey.equals("tenant_1")) {
            Thread.sleep(100);
            return request + "_1";
        } else if (isolationKey.equals("tenant_2")) {
            Thread.sleep(1000);
            return request + "_2";
        } else if (isolationKey.equals("tenant_3")) {
            throw new IllegalArgumentException("error");
        }
        Thread.sleep(ThreadLocalRandom.current().nextInt(100));
        return request + "_mix";
    }
}
