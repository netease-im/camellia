package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.id.gen.snowflake.ICamelliaSnowflakeIdGen;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CamelliaSnowflakeIdGenSdk implements ICamelliaSnowflakeIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaSnowflakeIdGenSdk.class);

    private final CamelliaIdGenSdkConfig config;
    private final OkHttpClient okHttpClient;
    private final CamelliaIdGenInvoker invoker;

    public CamelliaSnowflakeIdGenSdk(CamelliaIdGenSdkConfig config) {
        this.invoker = new CamelliaIdGenInvoker(config);
        this.config = config;
        this.okHttpClient = CamelliaIdGenHttpUtils.initOkHttpClient(config);
        logger.info("CamelliaSnowflakeIdGenSdk init success");
    }

    @Override
    public long genId() {
        return invoker.invoke(server -> {
            String fullUrl = server.getUrl() + "/camellia/id/gen/snowflake/genId";
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        }, config.getMaxRetry());
    }

    @Override
    public long decodeTs(long id) {
        return invoker.invoke(server -> {
            String fullUrl = server.getUrl() + "/camellia/id/gen/snowflake/decodeTs?id=" + id;
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        }, config.getMaxRetry());
    }
}
