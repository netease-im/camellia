package com.netease.nim.camellia.redis.proxy.tls.upstream;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.redis.base.resource.RedisResourceUtil;
import com.netease.nim.camellia.redis.base.resource.RedisType;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.monitor.PasswordMaskUtils;
import com.netease.nim.camellia.tools.ssl.SSLContextUtil;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2023/8/9
 */
public class DefaultProxyUpstreamTlsProvider implements ProxyUpstreamTlsProvider {

    private static final Logger logger = LoggerFactory.getLogger(DefaultProxyUpstreamTlsProvider.class);

    private SSLContext defaultSSLContext;
    private final ConcurrentHashMap<String, SSLContext> sslContextMap = new ConcurrentHashMap<>();

    public DefaultProxyUpstreamTlsProvider() {
        reload();
        ProxyDynamicConf.registerCallback(this::reload);
    }

    //只会初始化一次，新增的可以通过动态配置添加
    private synchronized void reload() {
        try {
            if (defaultSSLContext == null) {
                try {
                    initDefaultSSLContext();
                } catch (Exception e) {
                    logger.error("init default SSLContext error", e);
                }
            }
            String string = ProxyDynamicConf.getString("proxy.upstream.tls.config", null);
            if (string != null) {
                JSONArray array = JSONArray.parseArray(string);
                for (Object o : array) {
                    JSONObject json = JSONObject.parseObject(o.toString());
                    try {
                        String resource = json.getString("resource");
                        if (resource == null) {
                            logger.warn("illegal upstream ssl config, resource not found");
                            continue;
                        }
                        json.put("resource", PasswordMaskUtils.maskResource(resource));
                        RedisResourceUtil.parseResourceByUrl(new Resource(resource));
                        RedisType redisType = RedisType.parseRedisType(new Resource(resource));
                        if (redisType == null || !redisType.isTlsEnable()) {
                            logger.warn("illegal upstream ssl config, tls disable, resource = {}", PasswordMaskUtils.maskResource(resource));
                            continue;
                        }
                        if (sslContextMap.containsKey(resource)) {
                            continue;
                        }
                        String caFilePath;
                        String caFile = json.getString("ca.file");
                        if (caFile == null) {
                            caFilePath = json.getString("ca.file.path");
                        } else {
                            caFilePath = SSLContextUtil.getFilePath(caFile);
                        }
                        if (caFilePath == null) {
                            throw new IllegalArgumentException("caFilePath not found");
                        }
                        String crtFilePath;
                        String crtFile = json.getString("crt.file");
                        if (crtFile == null) {
                            crtFilePath = json.getString("crt.file.path");
                        } else {
                            crtFilePath = SSLContextUtil.getFilePath(crtFile);
                        }
                        if (crtFilePath == null) {
                            throw new IllegalArgumentException("crtFilePath not found");
                        }
                        String keyFilePath;
                        String keyFile = json.getString("key.file");
                        if (keyFile == null) {
                            keyFilePath = json.getString("key.file.path");
                        } else {
                            keyFilePath = SSLContextUtil.getFilePath(keyFile);
                        }
                        if (keyFilePath == null) {
                            throw new IllegalArgumentException("keyFilePath not found");
                        }
                        String password = json.getString("password");
                        SSLContext sslContext = SSLContextUtil.genSSLContext(caFilePath, crtFilePath, keyFilePath, password);
                        sslContextMap.put(resource, sslContext);
                        logger.info("proxy upstream SSLContext init success, resource = {}, caFilePath = {}, crtFilePath = {}, keyFilePath = {}",
                                PasswordMaskUtils.maskResource(resource), caFilePath, crtFilePath, keyFilePath);
                    } catch (Exception e) {
                        logger.error("proxy upstream SSLContext init error, config = {}", json);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    private void initDefaultSSLContext() {
        String caFilePath;
        String caFile = ProxyDynamicConf.getString("proxy.upstream.tls.ca.file", null);
        if (caFile == null) {
            caFilePath = ProxyDynamicConf.getString("proxy.upstream.tls.ca.file.path", null);
        } else {
            caFilePath = SSLContextUtil.getFilePath(caFile);
        }
        if (caFilePath == null) {
            return;
        }
        String crtFilePath;
        String crtFile = ProxyDynamicConf.getString("proxy.upstream.tls.crt.file", null);
        if (crtFile == null) {
            crtFilePath = ProxyDynamicConf.getString("proxy.upstream.tls.crt.file.path", null);
        } else {
            crtFilePath = SSLContextUtil.getFilePath(crtFile);
        }
        if (crtFilePath == null) {
            return;
        }
        String keyFilePath;
        String keyFile = ProxyDynamicConf.getString("proxy.upstream.tls.key.file", null);
        if (keyFile == null) {
            keyFilePath = ProxyDynamicConf.getString("proxy.upstream.tls.key.file.path", null);
        } else {
            keyFilePath = SSLContextUtil.getFilePath(keyFile);
        }
        if (keyFilePath == null) {
            return;
        }
        String password = ProxyDynamicConf.getString("proxy.upstream.tls.password", null);
        defaultSSLContext = SSLContextUtil.genSSLContext(caFilePath, crtFilePath, keyFilePath, password);
        logger.info("proxy upstream default SSLContext init success, caFilePath = {}, crtFilePath = {}, keyFilePath = {}",
                caFilePath, crtFilePath, keyFilePath);
    }

    @Override
    public SSLContext createSSLContext(Resource resource) {
        SSLContext sslContext = sslContextMap.get(resource.getUrl());
        if (sslContext != null) {
            return sslContext;
        }
        return defaultSSLContext;
    }

    @Override
    public SslHandler createSslHandler(SSLContext sslContext) {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        return new SslHandler(sslEngine);
    }
}
