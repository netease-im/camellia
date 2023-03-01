package com.netease.nim.camellia.external.call.common;

import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2023/2/28
 */
public interface MqInfoGenerator {

    //快+成功
    default List<MqInfo> fast() {
        return Collections.emptyList();
    }

    //快+失败
    default List<MqInfo> fastError() {
        return Collections.emptyList();
    }

    //慢+成功
    default List<MqInfo> slow() {
        return Collections.emptyList();
    }

    //慢+失败
    default List<MqInfo> slowError() {
        return Collections.emptyList();
    }

    //重试
    default List<MqInfo> retry() {
        return Collections.emptyList();
    }

    //重试+高优
    default List<MqInfo> retryHighPriority() {
        return Collections.emptyList();
    }

    //突发流量
    default List<MqInfo> heavyTraffic() {
        return Collections.emptyList();
    }

    //隔离
    default List<MqInfo> isolation() {
        return Collections.emptyList();
    }

    //降级，进入到这里的可能被丢弃
    default List<MqInfo> degradation() {
        return Collections.emptyList();
    }
}
