package com.netease.nim.camellia.hot.key.server.callback;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/5/10
 */
public class LoggingHotKeyCallback implements HotKeyCallback {

    private static final Logger logger = LoggerFactory.getLogger("camellia-hot-key");

    @Override
    public void newHotKey(HotKeyInfo hotKeyInfo) {
        logger.info("hotKeyInfo = {}", JSONObject.toJSONString(hotKeyInfo));
    }
}
