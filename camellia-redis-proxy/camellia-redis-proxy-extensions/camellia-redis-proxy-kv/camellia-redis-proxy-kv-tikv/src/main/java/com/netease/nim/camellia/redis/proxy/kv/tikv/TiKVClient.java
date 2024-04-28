package com.netease.nim.camellia.redis.proxy.kv.tikv;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
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

    private final RawKVClient tikvClient;

    public TiKVClient() {
        try {
            String string = ProxyDynamicConf.getString("kv.tikv.pd.address", null);
            TiConfiguration conf = TiConfiguration.createRawDefault(string);
            TiSession session = TiSession.create(conf);
            tikvClient = session.createRawClient();
        } catch (Exception e) {
            logger.error("OBKVClient init error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportTTL() {
        return true;
    }

    @Override
    public void put(byte[] key, byte[] value, long ttl) {
        try {
            tikvClient.put(ByteString.copyFrom(key), ByteString.copyFrom(value), ttl);
        } catch (Exception e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void put(byte[] key, byte[] value) {
        try {
            tikvClient.put(ByteString.copyFrom(key), ByteString.copyFrom(value));
        } catch (Exception e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchPut(List<KeyValue> list) {
        try {
            Map<ByteString, ByteString> map = new HashMap<>(list.size());
            for (KeyValue keyValue : list) {
                map.put(ByteString.copyFrom(keyValue.getKey()), ByteString.copyFrom(keyValue.getValue()));
            }
            tikvClient.batchPut(map);
        } catch (Exception e) {
            logger.error("batch put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public KeyValue get(byte[] key) {
        try {
            Optional<ByteString> bytes = tikvClient.get(ByteString.copyFrom(key));
            return bytes.map(byteString -> new KeyValue(key, byteString.toByteArray())).orElse(null);
        } catch (Exception e) {
            logger.error("get error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean exists(byte[] key) {
        try {
            Optional<ByteString> bytes = tikvClient.get(ByteString.copyFrom(key));
            return bytes.isPresent();
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean[] exists(byte[]... keys) {
        try {
            List<ByteString> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                list.add(ByteString.copyFrom(key));
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
    public List<KeyValue> batchGet(byte[]... keys) {
        try {
            List<ByteString> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                list.add(ByteString.copyFrom(key));
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
    public void delete(byte[] key) {
        try {
            tikvClient.delete(ByteString.copyFrom(key));
        } catch (Exception e) {
            logger.error("delete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchDelete(byte[]... keys) {
        try {
            List<ByteString> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                list.add(ByteString.copyFrom(key));
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
    public void checkAndDelete(byte[] key, byte[] value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
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
            logger.error("scan error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long count(byte[] startKey, byte[] prefix, boolean includeStartKey) {
        try {
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
            logger.error("count error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] endKey, int limit, Sort sort, boolean includeStartKey, boolean includeEndKey) {
        try {
            Iterator<Kvrpcpb.KvPair> iterator = tikvClient.scan0(ByteString.copyFrom(startKey), ByteString.copyFrom(endKey), limit);
            List<KeyValue> list = new ArrayList<>();
            while (iterator.hasNext()) {
                Kvrpcpb.KvPair kvPair = iterator.next();
                byte[] byteArray = kvPair.getKey().toByteArray();
                if (!includeStartKey && Arrays.equals(startKey, byteArray)) {
                    continue;
                }
                if (!includeEndKey && Arrays.equals(endKey, byteArray)) {
                    continue;
                }
                list.add(toKeyValue(kvPair));
                if (list.size() >= limit) {
                    break;
                }
            }
            return list;
        } catch (Exception e) {
            logger.error("scan error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long count(byte[] startKey, byte[] endKey, boolean includeStartKey, boolean includeEndKey) {
        try {
            List<Kvrpcpb.KvPair> scan = tikvClient.scan(ByteString.copyFrom(startKey), ByteString.copyFrom(endKey), true);
            long count = 0;
            for (Kvrpcpb.KvPair kvPair : scan) {
                byte[] byteArray = kvPair.getKey().toByteArray();
                if (!includeStartKey && Arrays.equals(startKey, byteArray)) {
                    continue;
                }
                if (!includeEndKey && Arrays.equals(endKey, byteArray)) {
                    continue;
                }
                count ++;
            }
            return count;
        } catch (Exception e) {
            logger.error("count error", e);
            throw new KvException(e);
        }
    }

    private KeyValue toKeyValue(Kvrpcpb.KvPair kvPair) {
        if (kvPair == null) {
            return null;
        }
        return new KeyValue(kvPair.getKey().toByteArray(), kvPair.getValue().toByteArray());
    }
}
