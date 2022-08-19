package com.netease.nim.camellia.console.command;

import com.netease.nim.camellia.console.model.WebResult;
import feign.Headers;
import feign.RequestLine;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */

@Headers({"Content-Type: application/json","Accept: application/json"})
public interface DashboardHeartApi {

    @RequestLine("GET /health/check")
    WebResult check();
}
