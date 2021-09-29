package com.netease.nim.camellia.id.gen.snowflake.samples;

import com.netease.nim.camellia.id.gen.sdk.CamelliaIdGenSdkConfig;
import com.netease.nim.camellia.id.gen.sdk.CamelliaSnowflakeIdGenSdk;

/**
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaSnowflakeIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8081");
        config.setMaxRetry(2);//重试次数
        CamelliaSnowflakeIdGenSdk idGenSdk = new CamelliaSnowflakeIdGenSdk(config);
        System.out.println(idGenSdk.genId());
    }
}
