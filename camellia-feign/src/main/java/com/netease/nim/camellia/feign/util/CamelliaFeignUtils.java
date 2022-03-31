package com.netease.nim.camellia.feign.util;

import com.netease.nim.camellia.feign.CamelliaFeignProps;
import com.netease.nim.camellia.feign.client.DynamicOption;
import com.netease.nim.camellia.feign.client.DynamicOptionClient;
import com.netease.nim.camellia.feign.conf.ContractType;
import com.netease.nim.camellia.feign.conf.DynamicContractTypeGetter;
import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;

/**
 * Created by caojiajun on 2022/3/1
 */
public class CamelliaFeignUtils {

    public static <T> T generate(CamelliaFeignProps feignProps, DynamicOption dynamicOption, Class<T> apiType, String feignUrl) {
        Feign.Builder builder = Feign.builder()
                    .requestInterceptors(feignProps.getRequestInterceptors())
                    .logLevel(feignProps.getLogLevel() == null ? CamelliaFeignProps.defaultLevel : feignProps.getLogLevel())
                    .retryer(feignProps.getRetryer() == null ? CamelliaFeignProps.defaultRetry : feignProps.getRetryer())
                    .logger(feignProps.getLogger() == null ? CamelliaFeignProps.defaultLogger : feignProps.getLogger())
                    .errorDecoder(feignProps.getErrorDecoder() == null ? CamelliaFeignProps.defaultErrorDecoder : feignProps.getErrorDecoder())
                    .options(feignProps.getOptions() == null ? CamelliaFeignProps.defaultOptions : feignProps.getOptions())
                    .invocationHandlerFactory(feignProps.getInvocationHandlerFactory() == null ? CamelliaFeignProps.defaultInvocationHandlerFactory : feignProps.getInvocationHandlerFactory());
        if (feignProps.isDecode404()) {
            builder.decode404();
        }
        Client client = feignProps.getClient();
        if (client == null) {
            client = CamelliaFeignProps.defaultClient;
        }
        if (dynamicOption != null) {
            builder.client(new DynamicOptionClient(client, dynamicOption));
        } else {
            builder.client(client);
        }
        ContractType contractType = ContractType.checkContractType(apiType);
        if (feignProps.getContract() == null) {
            if (dynamicOption == null) {
                builder.contract(CamelliaFeignProps.defaultContract);
            } else {
                DynamicContractTypeGetter dynamicContractTypeGetter = dynamicOption.getDynamicContractTypeGetter();
                if (dynamicContractTypeGetter == null) {
                    builder.contract(CamelliaFeignProps.defaultContract);
                } else {
                    Contract contract = dynamicContractTypeGetter.getContract(contractType);
                    if (contract != null) {
                        builder.contract(contract);
                    } else {
                        builder.contract(CamelliaFeignProps.defaultContract);
                    }
                }
            }
        } else {
            builder.contract(feignProps.getContract());
        }
        if (feignProps.getDecoder() == null) {
            if (dynamicOption == null) {
                builder.decoder(CamelliaFeignProps.defaultDecoder);
            } else {
                DynamicContractTypeGetter dynamicContractTypeGetter = dynamicOption.getDynamicContractTypeGetter();
                if (dynamicContractTypeGetter == null) {
                    builder.decoder(CamelliaFeignProps.defaultDecoder);
                } else {
                    Decoder decoder = dynamicContractTypeGetter.getDecoder(contractType);
                    if (decoder != null) {
                        builder.decoder(decoder);
                    } else {
                        builder.decoder(CamelliaFeignProps.defaultDecoder);
                    }
                }
            }
        } else {
            builder.decoder(feignProps.getDecoder());
        }
        if (feignProps.getEncoder() == null) {
            if (dynamicOption == null) {
                builder.encoder(CamelliaFeignProps.defaultEncoder);
            } else {
                DynamicContractTypeGetter dynamicContractTypeGetter = dynamicOption.getDynamicContractTypeGetter();
                if (dynamicContractTypeGetter == null) {
                    builder.encoder(CamelliaFeignProps.defaultEncoder);
                } else {
                    Encoder encoder = dynamicContractTypeGetter.getEncoder(contractType);
                    if (encoder != null) {
                        builder.encoder(encoder);
                    } else {
                        builder.encoder(CamelliaFeignProps.defaultEncoder);
                    }
                }
            }
        } else {
            builder.encoder(feignProps.getEncoder());
        }
        return builder.target(apiType, feignUrl);
    }
}
