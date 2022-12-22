package com.netease.nim.camellia.core.api;

import com.netease.nim.camellia.core.model.IpCheckerDto;
import com.netease.nim.camellia.core.model.RateLimitDto;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.util.List;

/**
 * Created by caojiajun on 2019/5/29.
 */
@Headers({"Content-Type: application/json", "Accept: application/json"})
public interface CamelliaMiscApi {

    @RequestLine("GET /camellia/misc/api/permissions/ip-checkers?md5={md5}")
    DataWithMd5Response<List<IpCheckerDto>> getIpCheckerList(@Param("md5") String md5);

    @RequestLine("GET /camellia/misc/api/permissions/rate-limit?md5={md5}")
    DataWithMd5Response<List<RateLimitDto>> getRateLimitConfigurationList(@Param("md5") String md5);

}
