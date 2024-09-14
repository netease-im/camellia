package com.netease.nim.camellia.redis.proxy.plugin.misc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *  key.prefix.multi.write.func.config={"abc": ["redis://@127.0.0.1:6379"], "def": ["redis://@127.0.0.1:6379", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379"]}
 *  bid.bgroup.key.prefix.multi.write.func.config={"abc": ["redis://@127.0.0.1:6379"], "def": ["redis://@127.0.0.1:6379", "redis-cluster://@127.0.0.1:6379,127.0.0.2:6379"]}
 * <p>
 * Created by caojiajun on 2024/9/14
 */
public class KeyPrefixMultiWriteFunc implements MultiWriteProxyPlugin.MultiWriteFunc {

    private static final Logger logger = LoggerFactory.getLogger(KeyPrefixMultiWriteFunc.class);

    private KeyPrefixMatcher defaultMatcher = new KeyPrefixMatcher(Collections.emptyList());
    private Map<String, KeyPrefixMatcher> bidBgroupMap = new HashMap<>();

    public KeyPrefixMultiWriteFunc() {
        reload();
        ProxyDynamicConf.registerCallback(this::reload);
    }

    private void reload() {
        try {
            final String configKey = "key.prefix.multi.write.func.config";
            String config = ProxyDynamicConf.getString(configKey, null);
            defaultMatcher = toKeyPrefixMatcher(config);
            Map<String, String> map = ProxyDynamicConf.getAll();
            Map<String, KeyPrefixMatcher> bidBgroupMap = new HashMap<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("." + configKey)) {
                    String bidBgroup = key.substring(0, key.length() - configKey.length() - 1);
                    KeyPrefixMatcher keyPrefixMatcher = toKeyPrefixMatcher(entry.getValue());
                    bidBgroupMap.put(bidBgroup, keyPrefixMatcher);
                }
            }
            this.bidBgroupMap = bidBgroupMap;
        } catch (Exception e) {
            logger.error("KeyPrefixMultiWriteFunc reload error", e);
        }
    }

    private KeyPrefixMatcher toKeyPrefixMatcher(String config) {
        try {
            if (config == null || config.isEmpty()) {
                return new KeyPrefixMatcher(Collections.emptyList());
            }
            List<Node> list = new ArrayList<>();
            JSONObject jsonObject = JSONObject.parseObject(config);
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                String prefix = entry.getKey();
                JSONArray array = JSONArray.parseArray(entry.getValue().toString());
                List<String> urls = new ArrayList<>();
                for (Object o : array) {
                    urls.add(String.valueOf(o));
                }
                MultiWriteProxyPlugin.MultiWriteInfo multiWriteInfo = new MultiWriteProxyPlugin.MultiWriteInfo(true, urls);
                Node node = new Node(prefix, multiWriteInfo);
                list.add(node);
            }
            return new KeyPrefixMatcher(list);
        } catch (Exception e) {
            logger.error("toKeyPrefixMatcher error, config = {}", config, e);
            return new KeyPrefixMatcher(Collections.emptyList());
        }
    }


    @Override
    public MultiWriteProxyPlugin.MultiWriteInfo multiWriteInfo(MultiWriteProxyPlugin.KeyInfo keyInfo) {
        MultiWriteProxyPlugin.KeyContext keyContext = keyInfo.getKeyContext();
        Long bid = keyContext.getBid();
        String bgroup = keyContext.getBgroup();
        if (bid == null || bid < 0 || bgroup == null) {
            String key = Utils.bytesToString(keyInfo.getKey());
            return defaultMatcher.match(key);
        }
        String key = Utils.bytesToString(keyInfo.getKey());
        KeyPrefixMatcher matcher = bidBgroupMap.get(bid + "." + bgroup);
        if (matcher == null) {
            return defaultMatcher.match(key);
        }
        return matcher.match(key);
    }

    private static class KeyPrefixMatcher {
        private final List<Node> list;

        public KeyPrefixMatcher(List<Node> list) {
            this.list = list;
        }

        public MultiWriteProxyPlugin.MultiWriteInfo match(String key) {
            if (list.isEmpty()) {
                return MultiWriteProxyPlugin.MultiWriteInfo.SKIP_MULTI_WRITE;
            }
            for (Node node : list) {
                if (key.startsWith(node.getPrefix())) {
                    return node.getMultiWriteInfo();
                }
            }
            return MultiWriteProxyPlugin.MultiWriteInfo.SKIP_MULTI_WRITE;
        }
    }

    private static class Node {
        private final String prefix;
        private final MultiWriteProxyPlugin.MultiWriteInfo multiWriteInfo;

        public Node(String prefix, MultiWriteProxyPlugin.MultiWriteInfo multiWriteInfo) {
            this.prefix = prefix;
            this.multiWriteInfo = multiWriteInfo;
        }

        public String getPrefix() {
            return prefix;
        }

        public MultiWriteProxyPlugin.MultiWriteInfo getMultiWriteInfo() {
            return multiWriteInfo;
        }
    }
}
