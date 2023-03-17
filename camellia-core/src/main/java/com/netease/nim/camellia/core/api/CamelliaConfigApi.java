package com.netease.nim.camellia.core.api;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Created by caojiajun on 2023/3/17
 */
@Headers({"Content-Type: application/json", "Accept: application/json"})
public interface CamelliaConfigApi {

    @RequestLine("GET /camellia/config/api/getConfig?namespace={namespace}&md5={md5}")
    CamelliaConfigResponse getConfig(@Param("namespace") String namespace, @Param("md5") String md5);
}
