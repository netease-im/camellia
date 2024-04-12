package com.netease.nim.camellia.redis.proxy.kv.core.kv;

import com.netease.nim.camellia.redis.proxy.kv.core.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.BytesKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by caojiajun on 2024/4/9
 */
public class DummyKVClient implements KVClient {

    private static final Logger logger = LoggerFactory.getLogger(DummyKVClient.class);

    private final Map<BytesKey, byte[]> map = new ConcurrentHashMap<>();

    @Override
    public void put(byte[] key, byte[] value) {
        logger.info("put");
        map.put(new BytesKey(key), value);
    }

    @Override
    public void batchPut(List<KeyValue> list) {
        logger.info("batchPut, size = {}", list.size());
        for (KeyValue keyValue : list) {
            map.put(new BytesKey(keyValue.getKey()), keyValue.getValue());
        }
    }

    @Override
    public KeyValue get(byte[] key) {
        logger.info("get");
        byte[] bytes = map.get(new BytesKey(key));
        return new KeyValue(key, bytes);
    }

    @Override
    public boolean exists(byte[] key) {
        logger.info("exists");
        return map.containsKey(new BytesKey(key));
    }

    @Override
    public boolean[] exists(byte[]... keys) {
        return new boolean[0];
    }

    @Override
    public List<KeyValue> batchGet(byte[]... keys) {
        logger.info("batchGet, size = {}", keys.length);
        List<KeyValue> list = new ArrayList<>();
        for (byte[] key : keys) {
            byte[] bytes = map.get(new BytesKey(key));
            list.add(new KeyValue(key, bytes));
        }
        return list;
    }

    @Override
    public void delete(byte[] key) {
        logger.info("delete");
        map.remove(new BytesKey(key));
    }

    @Override
    public void batchDelete(byte[]... keys) {
        logger.info("batchDelete, size = {}", keys.length);
        for (byte[] key : keys) {
            map.remove(new BytesKey(key));
        }
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        List<KeyValue> list = new ArrayList<>();
        for (Map.Entry<BytesKey, byte[]> entry : map.entrySet()) {
            if (BytesUtils.startWith(entry.getKey().getKey(), prefix)) {
                list.add(new KeyValue(entry.getKey().getKey(), entry.getValue()));
            }
        }
        logger.info("scan, size = {}", list.size());
        return list;
    }

    @Override
    public long count(byte[] startKey, byte[] prefix, boolean includeStartKey) {
        return 0;
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] endKey, int limit, Sort sort, boolean includeStartKey, boolean includeEndKey) {
        return null;
    }

    @Override
    public long count(byte[] startKey, byte[] endKey, boolean includeStartKey, boolean includeEndKey) {
        return 0;
    }
}
