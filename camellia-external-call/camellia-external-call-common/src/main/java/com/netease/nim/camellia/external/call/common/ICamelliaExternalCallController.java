package com.netease.nim.camellia.external.call.common;

/**
 * Created by caojiajun on 2023/2/27
 */
public interface ICamelliaExternalCallController {

    ExternalCallSelectInfo select(String isolationKey);

    void reportInputStats(ExternalCallInputStats inputStats);
}
