package com.netease.nim.camellia.feign;

import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.feign.resource.FeignResourceUtils;
import com.netease.nim.camellia.feign.route.CamelliaDashboardFeignResourceTableUpdater;
import com.netease.nim.camellia.feign.route.FeignResourceTableUpdater;
import com.netease.nim.camellia.feign.route.SimpleFeignResourceTableUpdater;
import com.netease.nim.camellia.tools.circuitbreaker.CircuitBreakerConfig;
import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import net.sf.cglib.proxy.Enhancer;

/**
 * Created by caojiajun on 2022/3/1
 */
public final class CamelliaFeign {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends Feign.Builder {
        private final CamelliaFeignProps feignProps = new CamelliaFeignProps();
        private CamelliaFeignEnv feignEnv = CamelliaFeignEnv.defaultFeignEnv();
        private String camelliaUrl;
        private CamelliaApi camelliaApi;
        private long checkIntervalMillis = 5000;
        private long bid = -1;
        private String bgroup = null;
        private ResourceTable resourceTable;
        private CircuitBreakerConfig circuitBreakerConfig;

        public Builder bid(long bid) {
            this.bid = bid;
            return this;
        }

        public Builder bgroup(String bgroup) {
            this.bgroup = bgroup;
            return this;
        }

        public Builder resourceTable(String route) {
            if (route != null) {
                this.resourceTable = ReadableResourceTableUtil.parseTable(route);
                FeignResourceUtils.checkResourceTable(this.resourceTable);
            }
            return this;
        }

        public Builder resourceTable(ResourceTable resourceTable) {
            if (resourceTable != null) {
                this.resourceTable = resourceTable;
                FeignResourceUtils.checkResourceTable(this.resourceTable);
            }
            return this;
        }

        public Builder checkIntervalMillis(long checkIntervalMillis) {
            if (checkIntervalMillis < 0) {
                throw new IllegalArgumentException("illegal checkIntervalMillis");
            }
            this.checkIntervalMillis = checkIntervalMillis;
            return this;
        }

        public Builder camelliaApi(CamelliaApi camelliaApi) {
            this.camelliaApi = camelliaApi;
            return this;
        }

        public Builder camelliaUrl(String camelliaUrl) {
            this.camelliaUrl = camelliaUrl;
            return this;
        }

        public Builder feignEnv(CamelliaFeignEnv feignEnv) {
            this.feignEnv = feignEnv;
            return this;
        }

        public Builder circuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
            this.circuitBreakerConfig = circuitBreakerConfig;
            return this;
        }

        public Builder logLevel(Logger.Level logLevel) {
            this.feignProps.setLogLevel(logLevel);
            return this;
        }

        public Builder contract(Contract contract) {
            this.feignProps.setContract(contract);
            return this;
        }

        public Builder client(Client client) {
            this.feignProps.setClient(client);
            return this;
        }

        public Builder retryer(Retryer retryer) {
            this.feignProps.setRetryer(retryer);
            return this;
        }

        public Builder logger(Logger logger) {
            this.feignProps.setLogger(logger);
            return this;
        }

        public Builder encoder(Encoder encoder) {
            this.feignProps.setEncoder(encoder);
            return this;
        }

        public Builder decoder(Decoder decoder) {
            this.feignProps.setDecoder(decoder);
            return this;
        }

        public Builder decode404() {
            this.feignProps.setDecode404(true);
            return this;
        }

        public Builder errorDecoder(ErrorDecoder errorDecoder) {
            this.feignProps.setErrorDecoder(errorDecoder);
            return this;
        }

        public Builder options(Request.Options options) {
            this.feignProps.setOptions(options);
            return this;
        }

        public Builder requestInterceptor(RequestInterceptor requestInterceptor) {
            this.feignProps.getRequestInterceptors().add(requestInterceptor);
            return this;
        }

        public Builder requestInterceptors(Iterable<RequestInterceptor> requestInterceptors) {
            if (requestInterceptors != null) {
                this.feignProps.getRequestInterceptors().clear();
                for (RequestInterceptor requestInterceptor : requestInterceptors) {
                    this.feignProps.getRequestInterceptors().add(requestInterceptor);
                }
            }
            return this;
        }

        public Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
            this.feignProps.setInvocationHandlerFactory(invocationHandlerFactory);
            return this;
        }

        public <T> T target(Class<T> apiType) {
            return target(apiType, (T) null);
        }

        public <T> T target(Class<T> apiType, T fallback) {
            FeignResourceTableUpdater updater = null;
            //指定配置 优于 注解配置
            //bid/bgroup/CamelliaApi 优于 固定配置
            CamelliaFeignClient annotation = apiType.getAnnotation(CamelliaFeignClient.class);
            if (annotation != null) {
                if (bid < 0) {
                    bid = annotation.bid();
                }
                if (bgroup == null) {
                    bgroup = annotation.bgroup();
                }
            }
            if (bid > 0) {
                CamelliaApi camelliaApi = this.camelliaApi;
                if (camelliaApi == null) {
                    if (camelliaUrl != null) {
                        camelliaApi = CamelliaApiUtil.init(camelliaUrl);
                    }
                }
                if (camelliaApi != null) {
                    updater = new CamelliaDashboardFeignResourceTableUpdater(camelliaApi, bid, bgroup, checkIntervalMillis);
                }
            }
            if (updater == null) {
                if (resourceTable == null) {
                    if (annotation != null && annotation.route() != null && annotation.route().trim().length() != 0) {
                        resourceTable = ReadableResourceTableUtil.parseTable(annotation.route());
                    }
                }
                if (resourceTable == null) {
                    throw new IllegalArgumentException("resourceTable is null");
                }
                FeignResourceUtils.checkResourceTable(resourceTable);
                updater = new SimpleFeignResourceTableUpdater(resourceTable);
            }
            FeignClientFactory.Default<T> factory = new FeignClientFactory.Default<>(apiType, feignProps);
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(apiType);
            enhancer.setCallback(new FeignCallback<>(apiType, fallback, updater, factory, feignEnv, circuitBreakerConfig));
            return (T) enhancer.create();
        }
    }
}
