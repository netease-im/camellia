package com.netease.nim.camellia.id.gen.strict.samples;

import com.netease.nim.camellia.id.gen.sdk.CamelliaIdGenSdkConfig;
import com.netease.nim.camellia.id.gen.sdk.CamelliaStrictIdGenSdk;

/**
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaStrictIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8082");
        config.setMaxRetry(2);//重试次数
        CamelliaStrictIdGenSdk idGenSdk = new CamelliaStrictIdGenSdk(config);
        System.out.println(idGenSdk.peekId("a"));
        System.out.println(idGenSdk.genId("a"));
    }
}
