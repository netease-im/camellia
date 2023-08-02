package com.netease.nim.camellia.hot.key.server.config.etcd;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.hot.key.common.model.HotKeyConfig;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyConfigService;
import com.netease.nim.camellia.hot.key.server.conf.HotKeyServerProperties;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by caojiajun on 2023/5/15
 */
public class EtcdHotKeyConfigService extends HotKeyConfigService {

    private static final Logger logger = LoggerFactory.getLogger(EtcdHotKeyConfigService.class);
    private static final ExecutorService reloadExecutor = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(EtcdHotKeyConfigService.class));
    private Map<String, String> configMap = new HashMap<>();
    private Client client;
    private ByteSequence configKey;
    private ConfigContentType contentType;

    @Override
    public HotKeyConfig get(String namespace) {
        String configStr = configMap.get(namespace);
        return JSONObject.parseObject(configStr, HotKeyConfig.class);
    }

    @Override
    public void init(HotKeyServerProperties properties) {
        Map<String, String> config = properties.getConfig();
        String etcdServer = null;
        try {
            String target = config.get("etcd.target");
            Client client;
            ClientBuilder builder;
            if (target != null) {
                //e.g  ip:///etcd0:2379,etcd1:2379,etcd2:2379
                builder = Client.builder().target(target);
                etcdServer = target;
            } else {
                //e.g http://etcd0:2379,http://etcd1:2379,http://etcd2:2379
                String endpoints = config.get("etcd.endpoints");
                if (endpoints == null) {
                    throw new IllegalArgumentException("missing 'etcd.target' or 'etcd.endpoints'");
                }
                String[] split = endpoints.split(",");
                builder = Client.builder().endpoints(split);
                etcdServer = endpoints;
            }
            String user = config.get("etcd.user");
            String password = config.get("etcd.password");
            String namespace = config.get("etcd.namespace");
            String authority = config.get("etcd.authority");
            if (user != null) {
                builder.user(ByteSequence.from(user, StandardCharsets.UTF_8));
            }
            if (password != null) {
                builder.password(ByteSequence.from(password, StandardCharsets.UTF_8));
            }
            if (namespace != null) {
                builder.namespace(ByteSequence.from(namespace, StandardCharsets.UTF_8));
            }
            if (authority != null) {
                builder.authority(authority);
            }
            for (Map.Entry<String, String> entry : config.entrySet()) {
                String prefix = "etcd.header.";
                if (entry.getKey().startsWith(prefix)) {
                    String header = entry.getKey().substring(prefix.length());
                    builder.authHeader(header, entry.getValue());
                }
            }
            client = builder.build();
            this.client = client;
            String key = config.get("etcd.config.key");
            if (key == null) {
                throw new IllegalArgumentException("missing 'etcd.config.key'");
            }
            contentType = ConfigContentType.getByValue(config.get("etcd.config.type"), ConfigContentType.json);
            configKey = ByteSequence.from(key.getBytes(StandardCharsets.UTF_8));
            boolean success = reload();
            if (!success) {
                throw new IllegalStateException("reload from etcd error");
            }
            client.getWatchClient().watch(configKey, response -> reloadExecutor.submit(() -> {
                logger.info("etcd conf update!");
                reload();
                for (String ns : configMap.keySet()) {
                    EtcdHotKeyConfigService.this.invokeUpdate(ns);
                }
            }));
            logger.info("EtcdHotKeyConfigService init success, etcdServer = {}", etcdServer);
        } catch (Exception e) {
            logger.info("EtcdHotKeyConfigService init error, etcdServer = {}", etcdServer, e);
            throw new IllegalArgumentException(e);
        }
    }

    private boolean reload() {
        try {
            CompletableFuture<GetResponse> future = client.getKVClient().get(configKey);
            GetResponse response = future.get();
            List<KeyValue> kvs = response.getKvs();
            if (kvs.isEmpty()) {
                throw new IllegalArgumentException("config not found");
            }
            KeyValue value = kvs.get(0);
            String content = value.getValue().toString();
            this.configMap = ConfigurationUtil.contentToMap(content, contentType);
            return true;
        } catch (Exception e) {
            logger.error("reload from etcd error, configKey = {}", configKey, e);
            return false;
        }
    }
}
