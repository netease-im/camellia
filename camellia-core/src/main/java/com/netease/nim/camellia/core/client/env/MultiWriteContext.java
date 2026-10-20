package com.netease.nim.camellia.core.client.env;

/**
 * 多写调用的上下文，由 {@link MultiWriteInvoker} 在真正执行写请求的线程上设置。
 *
 * <p>对于带出参（如 HBase batch 的 results 数组）的写方法，多写时同一个出参会传给多个集群的调用，
 * 业务实现可以据此只让主集群（index=0）回填出参，避免多个线程互相覆盖，以及异步多写场景下
 * 方法返回后出参仍被修改。
 *
 * Created by caojiajun on 2026/10/20.
 */
public class MultiWriteContext {

    private static final ThreadLocal<Integer> INDEX = new ThreadLocal<>();

    public static void setIndex(int index) {
        INDEX.set(index);
    }

    public static void clear() {
        INDEX.remove();
    }

    public static int getIndex() {
        Integer index = INDEX.get();
        return index == null ? 0 : index;
    }

    /**
     * 当前线程是否为主写集群（index=0）；非多写调用（未设置上下文）也视为主集群。
     */
    public static boolean isPrimary() {
        return getIndex() == 0;
    }
}
