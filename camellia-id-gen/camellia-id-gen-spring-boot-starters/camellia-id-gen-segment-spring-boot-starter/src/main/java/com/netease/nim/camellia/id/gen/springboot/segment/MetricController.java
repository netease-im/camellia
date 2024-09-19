package com.netease.nim.camellia.id.gen.springboot.segment;

import com.netease.nim.camellia.id.gen.monitor.PrometheusMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by caojiajun on 2023/12/25
 */
@RestController
public class MetricController {

    @GetMapping(value = "/metrics", produces = "text/plain;charset=UTF-8")
    public String metrics() {
        return PrometheusMetrics.metrics();
    }
}
