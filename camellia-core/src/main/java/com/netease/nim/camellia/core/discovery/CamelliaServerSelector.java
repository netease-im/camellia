package com.netease.nim.camellia.core.discovery;


import java.util.List;

/**
 * Created by caojiajun on 2022/3/1
 */
public interface CamelliaServerSelector<T> {

    /**
     *
     * @param list 待选择的节点列表
     * @param loadBalanceKey 负载均衡key
     * @return 具体选择哪个节点
     */
    T pick(List<T> list, Object loadBalanceKey);
}
