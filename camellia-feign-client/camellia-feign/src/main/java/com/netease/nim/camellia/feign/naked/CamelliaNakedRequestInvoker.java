package com.netease.nim.camellia.feign.naked;


import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.feign.naked.exception.CamelliaNakedClientException;

public interface CamelliaNakedRequestInvoker<R, W> {
    W invoke(FeignResource feignResource, R request) throws CamelliaNakedClientException;
}
