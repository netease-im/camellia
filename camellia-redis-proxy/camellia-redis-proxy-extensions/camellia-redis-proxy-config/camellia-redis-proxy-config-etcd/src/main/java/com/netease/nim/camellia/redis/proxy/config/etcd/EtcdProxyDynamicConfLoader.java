package com.netease.nim.camellia.redis.proxy.config.etcd;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.tools.utils.ConfigContentType;
import com.netease.nim.camellia.tools.utils.ConfigurationUtil;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConfLoader;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
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
 * Created by caojiajun on 2023/8/2
 */
public class EtcdProxyDynamicConfLoader implements ProxyDynamicConfLoader {

    private static final Logger logger = LoggerFactory.getLogger(EtcdProxyDynamicConfLoader.class);
    private static final ExecutorService reloadExecutor = Executors.newSingleThreadScheduledExecutor(new CamelliaThreadFactory(EtcdProxyDynamicConfLoader.class));

    private Map<String, String> initConf = new HashMap<>();
    private ByteSequence configKey;
    private Client client;
    private ConfigContentType contentType = ConfigContentType.properties;
    private Map<String, String> conf = new HashMap<>();

    @Override
    public Map<String, String> load() {
        //reload
        reload();
        //conf
        Map<String, String> map = new HashMap<>(initConf);
        map.putAll(conf);
        return map;
    }

    @Override
    public void init(Map<String, String> initConf) {
        this.initConf = new HashMap<>(initConf);
        String etcdServer = null;
        try {
            // Get etcd config by prefix.
            String target = initConf.get("etcd.target");
            Client client;
            ClientBuilder builder;
            if (target != null) {
                //e.g  ip:///etcd0:2379,etcd1:2379,etcd2:2379
                builder = Client.builder().target(target);
                etcdServer = target;
            } else {
                //e.g http://etcd0:2379,http://etcd1:2379,http://etcd2:2379
                String endpoints = initConf.get("etcd.endpoints");
                if (endpoints == null) {
                    throw new IllegalArgumentException("missing 'etcd.target' or 'etcd.endpoints'");
                }
                String[] split = endpoints.split(",");
                builder = Client.builder().endpoints(split);
                etcdServer = endpoints;
            }
            String user = initConf.get("etcd.user");
            String password = initConf.get("etcd.password");
            String namespace = initConf.get("etcd.namespace");
            String authority = initConf.get("etcd.authority");
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
            for (Map.Entry<String, String> entry : initConf.entrySet()) {
                String prefix = "etcd.header.";
                if (entry.getKey().startsWith(prefix)) {
                    String header = entry.getKey().substring(prefix.length());
                    builder.authHeader(header, entry.getValue());
                }
            }
            client = builder.build();
            this.client = client;
            String key = initConf.get("etcd.config.key");
            if (key == null) {
                throw new IllegalArgumentException("missing 'etcd.config.key'");
            }
            contentType = ConfigContentType.getByValue(initConf.get("etcd.config.type"), ConfigContentType.properties);
            configKey = ByteSequence.from(key.getBytes(StandardCharsets.UTF_8));
            boolean success = reload();
            if (!success) {
                throw new IllegalStateException("reload from etcd error");
            }
            client.getWatchClient().watch(configKey, response -> reloadExecutor.submit(() -> {
                logger.info("etcd conf update!");
                reload();
                ProxyDynamicConf.reload();
            }));
            logger.info("EtcdProxyDynamicConfLoader init success, etcdServer = {}", etcdServer);
        } catch (Exception e) {
            logger.info("EtcdProxyDynamicConfLoader init error, etcdServer = {}", etcdServer, e);
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
            this.conf = ConfigurationUtil.contentToMap(content, contentType);
            return true;
        } catch (Exception e) {
            logger.error("reload from etcd error, configKey = {}", configKey, e);
            return false;
        }
    }

}
