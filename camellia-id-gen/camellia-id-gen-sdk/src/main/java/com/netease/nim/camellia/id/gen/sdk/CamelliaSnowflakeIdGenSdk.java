package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import com.netease.nim.camellia.id.gen.snowflake.ICamelliaSnowflakeIdGen;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 基于http协议访问中心化部署的发号器
 * Created by caojiajun on 2021/9/27
 */
public class CamelliaSnowflakeIdGenSdk implements ICamelliaSnowflakeIdGen {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaSnowflakeIdGenSdk.class);

    private final CamelliaIdGenSdkConfig config;
    private final OkHttpClient okHttpClient;

    public CamelliaSnowflakeIdGenSdk(CamelliaIdGenSdkConfig config) {
        if (config.getUrl() == null || config.getUrl().trim().length() == 0) {
            throw new CamelliaIdGenException("url is empty");
        }
        this.config = config;
        this.okHttpClient = CamelliaIdGenHttpUtils.initOkHttpClient(config);
        logger.info("CamelliaSnowflakeIdGenSdk init success, url = {}", config.getUrl());
    }

    @Override
    public long genId() {
        try {
            String fullUrl = config.getUrl() + "/camellia/id/gen/snowflake/genId";
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(e);
        }
    }
}
