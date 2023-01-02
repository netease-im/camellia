package com.netease.nim.camellia.feign.conf;

import com.netease.nim.camellia.feign.CamelliaFeignProps;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

/**
 * Created by caojiajun on 2022/3/30
 */
public interface DynamicContractTypeGetter {

    Contract getContract(ContractType contractType);

    Encoder getEncoder(ContractType contractType);

    Decoder getDecoder(ContractType contractType);

    public static class Default implements DynamicContractTypeGetter {

        @Override
        public Contract getContract(ContractType contractType) {
            if (contractType == ContractType.DEFAULT) {
                return CamelliaFeignProps.defaultContract;
            } else if (contractType == ContractType.SPRING_MVC) {
                return new SpringMvcContract();
            } else {
                return CamelliaFeignProps.defaultContract;
            }
        }

        @Override
        public Encoder getEncoder(ContractType contractType) {
            if (contractType == ContractType.DEFAULT) {
                return new JacksonEncoder();
            } else if (contractType == ContractType.SPRING_MVC) {
                return new SpringEncoder(HttpMessageConverters::new);
            } else {
                return CamelliaFeignProps.defaultEncoder;
            }
        }

        @Override
        public Decoder getDecoder(ContractType contractType) {
            if (contractType == ContractType.DEFAULT) {
                return new JacksonDecoder();
            } else if (contractType == ContractType.SPRING_MVC) {
                return new SpringDecoder(HttpMessageConverters::new);
            } else {
                return CamelliaFeignProps.defaultDecoder;
            }
        }
    }
}
