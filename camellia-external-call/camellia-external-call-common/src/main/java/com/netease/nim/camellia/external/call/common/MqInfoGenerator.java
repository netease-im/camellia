package com.netease.nim.camellia.external.call.common;

import java.util.Collections;
import java.util.List;

/**
 * Created by caojiajun on 2023/2/28
 */
public interface MqInfoGenerator {

    default List<MqInfo> fast() {
        return Collections.emptyList();
    }

    default List<MqInfo> fastError() {
        return Collections.emptyList();
    }

    default List<MqInfo> slow() {
        return Collections.emptyList();
    }

    default List<MqInfo> slowError() {
        return Collections.emptyList();
    }

    default List<MqInfo> retry() {
        return Collections.emptyList();
    }

    default List<MqInfo> retryHighPriority() {
        return Collections.emptyList();
    }

    default List<MqInfo> heavyTraffic() {
        return Collections.emptyList();
    }

    default List<MqInfo> isolation() {
        return Collections.emptyList();
    }

}
