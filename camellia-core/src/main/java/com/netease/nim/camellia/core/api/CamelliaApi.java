package com.netease.nim.camellia.core.api;

import feign.Headers;
import feign.Param;
import feign.RequestLine;


/**
 * Created by caojiajun on 2019/5/29.
 */
@Headers({"Content-Type: application/json", "Accept: application/json"})
public interface CamelliaApi {

    @RequestLine("GET /camellia/api/resourceTable?bid={bid}&bgroup={bgroup}&md5={md5}")
    CamelliaApiResponse getResourceTable(@Param("bid") Long bid,
                                         @Param("bgroup") String bgroup,
                                         @Param("md5") String md5);

    @RequestLine("POST /camellia/api/reportStats")
    boolean reportStats(ResourceStats resourceStats);

}
