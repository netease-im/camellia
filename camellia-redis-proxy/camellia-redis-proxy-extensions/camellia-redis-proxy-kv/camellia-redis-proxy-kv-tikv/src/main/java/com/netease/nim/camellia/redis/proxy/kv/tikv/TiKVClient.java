package com.netease.nim.camellia.redis.proxy.kv.tikv;

import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.kvproto.Kvrpcpb;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;

import java.util.*;

/**
 *
 * Created by caojiajun on 2024/4/16
 */
public class TiKVClient implements KVClient {

    private static final Logger logger = LoggerFactory.getLogger(TiKVClient.class);

    private RawKVClient tikvClient;

    @Override
    public void init(String namespace) {
        String string = RedisKvConf.getString(namespace, "kv.tikv.pd.address", null);
        try {
            TiConfiguration conf = TiConfiguration.createRawDefault(string);
            conf.setWarmUpEnable(false);
            TiSession session = TiSession.create(conf);
            tikvClient = session.createRawClient();
            logger.info("TiKVClient init success, namespace = {}, pd.address = {}", namespace, string);
        } catch (Throwable e) {
            logger.error("TiKVClient init error, namespace = {}, pd.address = {}", namespace, string, e);
            throw new KvException(e);
        }
    }

    private byte[] encode(int slot, byte[] key) {
        return BytesUtils.merge(BytesUtils.toBytes(slot), key);
    }

    private byte[] decode(byte[] key) {
        if (key == null) {
            return null;
        }
        if (key.length == 0) {
            return key;
        }
        if (key.length < 4) {
            throw new KvException("key.len < 4");
        }
        byte[] result = new byte[key.length - 4];
        System.arraycopy(key, 4, result, 0, result.length);
        return result;
    }

    @Override
    public boolean supportTTL() {
        return true;
    }

    @Override
    public void put(int slot, byte[] key, byte[] value, long ttl) {
        try {
            key = encode(slot, key);
            if (ttl / 1000 * 1000 < ttl) {
                ttl = ttl / 1000 + 1;// +1 seconds
            }
            tikvClient.put(ByteString.copyFrom(key), ByteString.copyFrom(value), ttl);
        } catch (Exception e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void put(int slot, byte[] key, byte[] value) {
        try {
            key = encode(slot, key);
            tikvClient.put(ByteString.copyFrom(key), ByteString.copyFrom(value));
        } catch (Exception e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchPut(int slot, List<KeyValue> list) {
        try {
            Map<ByteString, ByteString> map = new HashMap<>(list.size());
            for (KeyValue keyValue : list) {
                byte[] key = encode(slot, keyValue.getKey());
                map.put(ByteString.copyFrom(key), ByteString.copyFrom(keyValue.getValue()));
            }
            tikvClient.batchPut(map);
        } catch (Exception e) {
            logger.error("batch put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public KeyValue get(int slot, byte[] key) {
        try {
            Optional<ByteString> bytes = tikvClient.get(ByteString.copyFrom(encode(slot, key)));
            return bytes.map(byteString -> new KeyValue(key, byteString.toByteArray())).orElse(null);
        } catch (Exception e) {
            logger.error("get error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean exists(int slot, byte[] key) {
        try {
            Optional<ByteString> bytes = tikvClient.get(ByteString.copyFrom(encode(slot, key)));
            return bytes.isPresent();
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean[] exists(int slot, byte[]... keys) {
        try {
            List<ByteString> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                list.add(ByteString.copyFrom(encode(slot, key)));
            }
            boolean[] exists = new boolean[keys.length];
            List<Kvrpcpb.KvPair> kvPairs = tikvClient.batchGet(list);
            for (int i=0; i<kvPairs.size(); i++) {
                Kvrpcpb.KvPair kvPair = kvPairs.get(i);
                if (kvPair == null) {
                    exists[i] = false;
                    continue;
                }
                ByteString key = kvPair.getKey();
                ByteString value = kvPair.getValue();
                exists[i] = key != null && value != null;
            }
            return exists;
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> batchGet(int slot, byte[]... keys) {
        try {
            List<ByteString> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                list.add(ByteString.copyFrom(encode(slot, key)));
            }
            List<KeyValue> result = new ArrayList<>();
            List<Kvrpcpb.KvPair> kvPairs = tikvClient.batchGet(list);
            for (Kvrpcpb.KvPair kvPair : kvPairs) {
                result.add(toKeyValue(kvPair));
            }
            return result;
        } catch (Exception e) {
            logger.error("batchGet error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void delete(int slot, byte[] key) {
        try {
            tikvClient.delete(ByteString.copyFrom(encode(slot, key)));
        } catch (Exception e) {
            logger.error("delete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchDelete(int slot, byte[]... keys) {
        try {
            List<ByteString> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                list.add(ByteString.copyFrom(encode(slot, key)));
            }
            tikvClient.batchDelete(list);
        } catch (Exception e) {
            logger.error("batchDelete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportCheckAndDelete() {
        return false;
    }

    @Override
    public void checkAndDelete(int slot, byte[] key, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportReverseScan() {
        return false;
    }

    @Override
    public List<KeyValue> scanByPrefix(int slot, byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
            //
            startKey = encode(slot, startKey);
            prefix = encode(slot, prefix);
            //
            if (!includeStartKey) {
                limit = limit + 1;
            }
            Iterator<Kvrpcpb.KvPair> iterator = tikvClient.scan0(ByteString.copyFrom(startKey), limit);
            List<KeyValue> list = new ArrayList<>();
            while (iterator.hasNext()) {
                Kvrpcpb.KvPair kvPair = iterator.next();
                byte[] byteArray = kvPair.getKey().toByteArray();
                if (!includeStartKey && Arrays.equals(startKey, byteArray)) {
                    continue;
                }
                if (BytesUtils.startWith(byteArray, prefix)) {
                    list.add(toKeyValue(kvPair));
                    if (list.size() >= limit) {
                        break;
                    }
                } else {
                    break;
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("scanByPrefix error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long countByPrefix(int slot, byte[] startKey, byte[] prefix, boolean includeStartKey) {
        try {
            //
            startKey = encode(slot, startKey);
            prefix = encode(slot, prefix);
            //
            List<Kvrpcpb.KvPair> kvPairs = tikvClient.scanPrefix(ByteString.copyFrom(prefix), true);
            long count = 0;
            for (Kvrpcpb.KvPair kvPair : kvPairs) {
                if (!includeStartKey && Arrays.equals(kvPair.getKey().toByteArray(), startKey)) {
                    continue;
                }
                count ++;
            }
            return count;
        } catch (Exception e) {
            logger.error("countByPrefix error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> scanByStartEnd(int slot, byte[] startKey, byte[] endKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
            //
            startKey = encode(slot, startKey);
            endKey = encode(slot, endKey);
            prefix = encode(slot, prefix);
            //
            if (!includeStartKey) {
                limit = limit + 1;
            }
            Iterator<Kvrpcpb.KvPair> iterator = tikvClient.scan0(ByteString.copyFrom(startKey), ByteString.copyFrom(endKey), limit);
            List<KeyValue> list = new ArrayList<>();
            while (iterator.hasNext()) {
                Kvrpcpb.KvPair kvPair = iterator.next();
                byte[] byteArray = kvPair.getKey().toByteArray();
                if (!includeStartKey && Arrays.equals(startKey, byteArray)) {
                    continue;
                }
                if (!BytesUtils.startWith(byteArray, prefix)) {
                    break;
                }
                list.add(toKeyValue(kvPair));
                if (list.size() >= limit) {
                    break;
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("scanByStartEnd error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long countByStartEnd(int slot, byte[] startKey, byte[] endKey, byte[] prefix, boolean includeStartKey) {
        try {
            //
            startKey = encode(slot, startKey);
            endKey = encode(slot, endKey);
            prefix = encode(slot, prefix);
            //
            List<Kvrpcpb.KvPair> scan = tikvClient.scan(ByteString.copyFrom(startKey), ByteString.copyFrom(endKey), true);
            long count = 0;
            for (Kvrpcpb.KvPair kvPair : scan) {
                byte[] byteArray = kvPair.getKey().toByteArray();
                if (!includeStartKey && Arrays.equals(startKey, byteArray)) {
                    continue;
                }
                if (!BytesUtils.startWith(byteArray, prefix)) {
                    break;
                }
                count ++;
            }
            return count;
        } catch (Exception e) {
            logger.error("countByStartEnd error", e);
            throw new KvException(e);
        }
    }

    private KeyValue toKeyValue(Kvrpcpb.KvPair kvPair) {
        if (kvPair == null) {
            return null;
        }
        return new KeyValue(decode(kvPair.getKey().toByteArray()), kvPair.getValue().toByteArray());
    }
}
