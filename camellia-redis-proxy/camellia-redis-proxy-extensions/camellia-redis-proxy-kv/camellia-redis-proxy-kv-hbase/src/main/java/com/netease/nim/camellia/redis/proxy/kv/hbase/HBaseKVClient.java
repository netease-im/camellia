package com.netease.nim.camellia.redis.proxy.kv.hbase;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.api.CamelliaApi;
import com.netease.nim.camellia.core.api.CamelliaApiUtil;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.CamelliaHBaseEnv;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.conf.CamelliaHBaseConf;
import com.netease.nim.camellia.hbase.connection.CamelliaHBaseConnectionFactory;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.KeyOnlyFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by caojiajun on 2024/4/8
 */
public class HBaseKVClient implements KVClient {

    private static final Logger logger = LoggerFactory.getLogger(HBaseKVClient.class);

    private static final byte[] cf = "d".getBytes(StandardCharsets.UTF_8);
    private static final byte[] column = "v".getBytes(StandardCharsets.UTF_8);

    private String namespace;
    private String tableName;
    private CamelliaHBaseTemplate template;

    private Durability durability = Durability.USE_DEFAULT;

    @Override
    public void init(String namespace) {
        try {
            this.namespace = namespace;
            //config
            String conf = RedisKvConf.getString(namespace, "kv.store.hbase.conf", null);
            CamelliaHBaseConf camelliaHBaseConf = new CamelliaHBaseConf();
            if (conf != null) {
                JSONObject json = JSONObject.parseObject(conf);
                for (Map.Entry<String, Object> entry : json.entrySet()) {
                    camelliaHBaseConf.addConf(entry.getKey(), entry.getValue().toString());
                }
            }
            CamelliaHBaseEnv hBaseEnv = new CamelliaHBaseEnv.Builder()
                    .connectionFactory(new CamelliaHBaseConnectionFactory.DefaultHBaseConnectionFactory(camelliaHBaseConf))
                    .build();

            //client
            String configType = RedisKvConf.getString(namespace, "kv.store.hbase.config.type", "local");
            if (configType.equalsIgnoreCase("local")) {
                String string = RedisKvConf.getString(namespace, "kv.store.hbase.url", null);
                HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(string));
                template = new CamelliaHBaseTemplate(hBaseEnv, hBaseResource);
                logger.info("hbase template init success, namespace = {}, resource = {}", namespace, string);
            } else if (configType.equalsIgnoreCase("remote")) {
                String dashboardUrl = RedisKvConf.getString(namespace, "kv.store.hbase.camellia.dashboard.url", null);
                if (dashboardUrl == null) {
                    throw new KvException("illegal dashboardUrl, namespace = " + namespace);
                }
                boolean monitorEnable = RedisKvConf.getBoolean(namespace, "kv.store.hbase.camellia.dashboard.monitor.enable", true);
                long checkIntervalMillis = RedisKvConf.getLong(namespace, "kv.store.hbase.camellia.dashboard.check.interval.millis", 3000L);
                long bid = RedisKvConf.getLong(namespace, "kv.store.hbase.bid", -1);
                String bgroup = RedisKvConf.getString(namespace, "kv.store.hbase.bgroup", "default");
                if (bid <= 0) {
                    throw new KvException("illegal bid, namespace = " + namespace);
                }
                CamelliaApi camelliaApi = CamelliaApiUtil.init(dashboardUrl);
                template = new CamelliaHBaseTemplate(hBaseEnv, camelliaApi, bid, bgroup, monitorEnable, checkIntervalMillis);
                logger.info("hbase template init success, namespace = {}, dashboardUrl = {}, bid = {}, bgroup = {}", namespace, dashboardUrl, bid, bgroup);
            } else {
                throw new KvException("init hbase template error, namespace = " + namespace);
            }

            //table
            tableName = RedisKvConf.getString(namespace, "kv.store.hbase.table.name", "camellia_kv");
            logger.info("HBaseKVClient init success, namespace = {}, table = {}", namespace, tableName);

            reloadConfig();
            ProxyDynamicConf.registerCallback(this::reloadConfig);
        } catch (KvException e) {
            logger.error("HBaseKVClient init error, namespace = {}", namespace, e);
            throw e;
        } catch (Throwable e) {
            logger.error("HBaseKVClient init error, namespace = {}", namespace, e);
            throw new KvException(e);
        }
    }

    private void reloadConfig() {
        String string = RedisKvConf.getString(namespace, "kv.store.hbase.durability", "");
        Durability durability = this.durability;
        if (string.equalsIgnoreCase(Durability.USE_DEFAULT.name())) {
            durability = Durability.USE_DEFAULT;
        } else if (string.equalsIgnoreCase(Durability.FSYNC_WAL.name())) {
            durability = Durability.FSYNC_WAL;
        } else if (string.equalsIgnoreCase(Durability.SYNC_WAL.name())) {
            durability = Durability.SYNC_WAL;
        } else if (string.equalsIgnoreCase(Durability.ASYNC_WAL.name())) {
            durability = Durability.ASYNC_WAL;
        } else if (string.equalsIgnoreCase(Durability.SKIP_WAL.name())) {
            durability = Durability.SKIP_WAL;
        }
        if (!durability.equals(this.durability)) {
            logger.info("kv store hbase durability update, {}->{}", this.durability, durability);
            this.durability = durability;
        }
    }

    @Override
    public boolean supportTTL() {
        return false;
    }

    @Override
    public void put(int slot, byte[] key, byte[] value, long ttl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(int slot, byte[] key, byte[] value) {
        try {
            key = encode(slot, key);
            Put put = new Put(key);
            put.addColumn(cf, column, value);
            put.setDurability(durability);
            template.put(tableName, put);
        } catch (Exception e) {
            logger.error("put error", e);
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
    public void batchPut(int slot, List<KeyValue> list) {
        try {
            List<Put> putList = new ArrayList<>();
            for (KeyValue keyValue : list) {
                byte[] key = encode(slot, keyValue.getKey());
                Put put = new Put(key);
                put.addColumn(cf, column, keyValue.getValue());
                put.setDurability(durability);
                putList.add(put);
            }
            template.put(tableName, putList);
        } catch (Exception e) {
            logger.error("batchPut error", e);
            throw new KvException(e);
        }
    }

    @Override
    public KeyValue get(int slot, byte[] key) {
        try {
            key = encode(slot, key);
            Get get = new Get(key);
            Result result = template.get(tableName, get);
            if (result == null) {
                return null;
            }
            byte[] value = result.getValue(cf, column);
            if (value == null) {
                return null;
            }
            return new KeyValue(key, value);
        } catch (Exception e) {
            logger.error("get error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean exists(int slot, byte[] key) {
        try {
            key = encode(slot, key);
            Get get = new Get(key);
            return template.exists(tableName, get);
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean[] exists(int slot, byte[]... keys) {
        try {
            List<Get> getList = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                getList.add(new Get(encode(slot, key)));
            }
            return template.existsAll(tableName, getList);
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> batchGet(int slot, byte[]... keys) {
        try {
            List<Get> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Get get = new Get(encode(slot, key));
                list.add(get);
            }
            List<KeyValue> keyValues = new ArrayList<>(list.size());
            Result[] results = template.get(tableName, list);
            for (Result result : results) {
                if (result == null) continue;
                byte[] key = result.getRow();
                byte[] value = result.getValue(cf, column);
                keyValues.add(new KeyValue(decode(key), value));
            }
            return keyValues;
        } catch (Exception e) {
            logger.error("batchGet error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void delete(int slot, byte[] key) {
        try {
            Delete delete = new Delete(encode(slot, key));
            delete.setDurability(durability);
            template.delete(tableName, delete);
        } catch (Exception e) {
            logger.error("delete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchDelete(int slot, byte[]... keys) {
        try {
            List<Delete> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Delete delete = new Delete(encode(slot, key));
                delete.setDurability(durability);
                list.add(delete);
            }
            template.delete(tableName, list);
        } catch (Exception e) {
            logger.error("batchDelete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportCheckAndDelete() {
        return true;
    }

    @Override
    public void checkAndDelete(int slot, byte[] key, byte[] value) {
        try {
            key = encode(slot, key);
            Delete delete = new Delete(key);
            delete.setDurability(durability);
            template.checkAndDelete(tableName, key, cf, column, value, delete);
        } catch (Exception e) {
            logger.error("checkAndDelete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportReverseScan() {
        return true;
    }

    @Override
    public List<KeyValue> scanByPrefix(int slot, byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
            //
            startKey = encode(slot, startKey);
            prefix = encode(slot, prefix);
            //
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setCaching(includeStartKey ? limit : (limit + 1));
            scan.setSmall(true);
            if (sort == Sort.ASC) {
                scan.setStopRow(BytesUtils.nextBytes(prefix));
            } else {
                scan.setStopRow(BytesUtils.lastBytes(prefix));
            }
            scan.setReversed(sort != Sort.ASC);
            try (ResultScanner scanner = template.scan(tableName, scan)) {
                List<KeyValue> list = new ArrayList<>();
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    if (BytesUtils.startWith(row, prefix)) {
                        byte[] key = result.getRow();
                        byte[] value = result.getValue(cf, column);
                        list.add(new KeyValue(decode(key), value));
                        if (list.size() >= limit) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                return list;
            }
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
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setSmall(true);
            scan.setStopRow(BytesUtils.nextBytes(prefix));
            scan.setFilter(new KeyOnlyFilter());
            int count=0;
            try (ResultScanner scanner = template.scan(tableName, scan)) {
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    if (BytesUtils.startWith(row, prefix)) {
                        count ++;
                    } else {
                        break;
                    }
                }
                return count;
            }
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
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setStopRow(endKey);
            scan.setCaching(includeStartKey ? limit : (limit + 1));
            scan.setSmall(true);
            scan.setReversed(sort != Sort.ASC);
            try (ResultScanner scanner = template.scan(tableName, scan)) {
                List<KeyValue> list = new ArrayList<>();
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    if (!BytesUtils.startWith(row, prefix)) {
                        break;
                    }
                    byte[] key = result.getRow();
                    byte[] value = result.getValue(cf, column);
                    list.add(new KeyValue(decode(key), value));
                    if (list.size() >= limit) {
                        break;
                    }
                }
                return list;
            }
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
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setStopRow(endKey);
            scan.setSmall(true);
            scan.setFilter(new KeyOnlyFilter());
            int count = 0;
            try (ResultScanner scanner = template.scan(tableName, scan)) {
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    if (!BytesUtils.startWith(row, prefix)) {
                        break;
                    }
                    count ++;
                }
                return count;
            }
        } catch (Exception e) {
            logger.error("countByStartEnd error", e);
            throw new KvException(e);
        }
    }
}
