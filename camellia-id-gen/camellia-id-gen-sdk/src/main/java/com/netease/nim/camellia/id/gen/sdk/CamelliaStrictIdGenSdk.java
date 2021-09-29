package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.id.gen.strict.ICamelliaStrictIdGen;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;

/**
 * 基于http协议访问中心化部署的发号器
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaStrictIdGenSdk implements ICamelliaStrictIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaStrictIdGenSdk.class);

    private final CamelliaIdGenSdkConfig config;
    private final OkHttpClient okHttpClient;
    private final CamelliaIdGenInvoker invoker;

    public CamelliaStrictIdGenSdk(CamelliaIdGenSdkConfig config) {
        this.invoker = new CamelliaIdGenInvoker(config);
        this.config = config;
        this.okHttpClient = CamelliaIdGenHttpUtils.initOkHttpClient(config);
        logger.info("CamelliaStrictIdGenSdk init success");
    }

    @Override
    public long genId(String tag) {
        return invoker.invoke(server -> {
            String fullUrl = server.getUrl() + "/camellia/id/gen/strict/genId?tag=" + URLEncoder.encode(tag, "utf-8");
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        }, config.getMaxRetry());
    }

    @Override
    public long peekId(String tag) {
        return invoker.invoke(server -> {
            String fullUrl = server.getUrl() + "/camellia/id/gen/strict/peekId?tag=" + URLEncoder.encode(tag, "utf-8");
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        }, config.getMaxRetry());
    }
}
