package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
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

    public CamelliaStrictIdGenSdk(CamelliaIdGenSdkConfig config) {
        if (config.getUrl() == null || config.getUrl().trim().length() == 0) {
            throw new CamelliaIdGenException("url is empty");
        }
        this.config = config;
        this.okHttpClient = CamelliaIdGenHttpUtils.initOkHttpClient(config);
        if (config.getUrl() == null || config.getUrl().trim().length() == 0) {
            throw new CamelliaIdGenException("url is empty");
        }
        logger.info("CamelliaStrictIdGenSdk init success, url = {}", config.getUrl());
    }

    @Override
    public long genId(String tag) {
        try {
            String fullUrl = config.getUrl() + "/camellia/id/gen/strict/genId?tag=" + URLEncoder.encode(tag, "utf-8");
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(e);
        }
    }

    @Override
    public long peekId(String tag) {
        try {
            String fullUrl = config.getUrl() + "/camellia/id/gen/strict/peekId?tag=" + URLEncoder.encode(tag, "utf-8");
            return CamelliaIdGenHttpUtils.genId(okHttpClient, fullUrl);
        } catch (CamelliaIdGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CamelliaIdGenException(e);
        }
    }
}
