package com.netease.nim.camellia.feign.naked;


import com.netease.nim.camellia.feign.resource.FeignResource;
import com.netease.nim.camellia.feign.naked.exception.ClientException;

/**
 * 具体的请求实现逻辑在这里实现
 * 可以通过控制上抛的异常来决定是否重试
 * Created by yuanyuanjun on 2019/4/22.
 */
public interface CamelliaNakedRequestInvoker<R, W> {
    W doRequest(FeignResource feignResource, R request) throws ClientException;
}
