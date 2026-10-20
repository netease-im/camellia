package com.netease.nim.camellia.id.gen.strict;

/**
 * 可以返回严格递增的消息id
 * Created by caojiajun on 2021/9/24
 */
public interface ICamelliaStrictIdGen {

    /**
     * 生成一个id
     * @param tag 业务tag
     * @return id
     */
    long genId(String tag);

    /**
     * 返回最新的id，但是不使用
     * @param tag 业务tag
     * @return id
     */
    long peekId(String tag);

    /**
     * 仅从缓存中获取一个id，缓存为空时不触发补号
     * <p>
     * 默认实现表示当前实现不支持预取缓存，返回null
     * @param tag 业务tag
     * @return id，缓存为空时返回null
     */
    default Long genIdIfCached(String tag) {
        return null;
    }

    /**
     * 仅从缓存中查看下一个即将使用的id，但不消费，缓存为空时不触发补号
     * <p>
     * 默认实现表示当前实现不支持预取缓存，返回null
     * @param tag 业务tag
     * @return id，缓存为空时返回null
     */
    default Long peekIdIfCached(String tag) {
        return null;
    }

    /**
     * 判断该tag是否正在补号
     * <p>
     * 默认实现表示当前实现不支持异步补号，返回false
     * @param tag 业务tag
     * @return 是否正在补号
     */
    default boolean isLoading(String tag) {
        return false;
    }

    /**
     * 解析id中的regionId
     * @param id id
     * @return regionId
     */
    long decodeRegionId(long id);
}
