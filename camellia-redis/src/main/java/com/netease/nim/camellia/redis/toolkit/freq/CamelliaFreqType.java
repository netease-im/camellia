package com.netease.nim.camellia.redis.toolkit.freq;

/**
 * Created by caojiajun on 2022/8/2
 */
public enum CamelliaFreqType {
    //单机模式
    STANDALONE,
    //集群模式，走redis
    CLUSTER,
    //混合，先过单机，再过集群，主要是用于输入qps非常高，但是频控后的目标qps又很低的场景
    //假设输入10w的QPS，目标是20的QPS
    // 如果是普通的集群模式，则10w的QPS都会打到redis
    // 如果用混合模式，且一共有10个节点在处理，则穿透到redis最多是20*10=200QPS，最终通过的也只有20QPS，可以极大的降低redis的压力
    MISC,
    ;
}
