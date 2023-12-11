package com.netease.nim.camellia.feign.springboot;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.discovery.ReloadableDiscoveryFactory;
import com.netease.nim.camellia.feign.CamelliaFeignEnv;
import com.netease.nim.camellia.feign.CamelliaFeignProps;
import com.netease.nim.camellia.feign.CamelliaFeignClientFactory;
import com.netease.nim.camellia.feign.conf.CamelliaFeignDynamicOptionGetter;
import com.netease.nim.camellia.feign.discovery.FeignServerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by caojiajun on 2022/3/30
 */
@Configuration
@EnableConfigurationProperties({CamelliaFeignProperties.class})
public class CamelliaFeignConfiguration {

    @Autowired(required = false)
    private DiscoveryClient discoveryClient;

    @Bean
    @ConditionalOnMissingBean(value = CamelliaFeignProps.class)
    public CamelliaFeignProps feignProps() {
        return new CamelliaFeignProps();
    }

    @Bean
    @ConditionalOnMissingBean(value = CamelliaFeignEnv.class)
    public CamelliaFeignEnv camelliaFeignEnv() {
        if (discoveryClient == null) {
            return CamelliaFeignEnv.defaultFeignEnv();
        }
        ReloadableDiscoveryFactory<FeignServerInfo> factory = new ReloadableDiscoveryFactory<>(serviceName -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
            Set<FeignServerInfo> servers = new HashSet<>();
            for (ServiceInstance instance : instances) {
                FeignServerInfo serverInfo = new FeignServerInfo();
                serverInfo.setHost(instance.getHost());
                serverInfo.setPort(instance.getPort());
                servers.add(serverInfo);
            }
            return new ArrayList<>(servers);
        });
        return new CamelliaFeignEnv.Builder()
                .discoveryFactory(factory)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(value = CamelliaFeignDynamicOptionGetter.class)
    public CamelliaFeignDynamicOptionGetter dynamicOptionGetter(CamelliaFeignProperties properties) {
        CamelliaFeignProperties.Client client = properties.getClient();
        if (client != null) {
            return new CamelliaFeignDynamicOptionGetter.DefaultCamelliaFeignDynamicOptionGetter(client.getConnectTimeoutMillis(),
                    client.getReadTimeoutMillis(), client.isCircuitBreakerEnable());
        } else {
            return new CamelliaFeignDynamicOptionGetter.DefaultCamelliaFeignDynamicOptionGetter(3000);
        }
    }

    @Bean
    @ConditionalOnMissingBean(value = CamelliaFeignClientFactory.class)
    public CamelliaFeignClientFactory feignServiceFactory(CamelliaFeignProperties properties) {
        CamelliaFeignProperties.Remote remote = properties.getRemote();
        CamelliaApi camelliaApi = null;
        long checkIntervalMillis = 5000;
        if (remote != null && remote.isEnable()) {
            camelliaApi = CamelliaApiUtil.init(remote.getUrl(), remote.getHeaderMap());
            checkIntervalMillis = remote.getCheckIntervalMillis();
        }
        return new CamelliaFeignClientFactory(camelliaFeignEnv(), camelliaApi, checkIntervalMillis, feignProps(), dynamicOptionGetter(properties));
    }

}
