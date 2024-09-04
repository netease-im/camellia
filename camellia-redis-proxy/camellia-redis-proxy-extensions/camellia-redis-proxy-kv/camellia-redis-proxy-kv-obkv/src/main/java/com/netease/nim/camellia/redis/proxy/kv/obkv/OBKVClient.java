package com.netease.nim.camellia.redis.proxy.kv.obkv;

import com.alipay.oceanbase.rpc.ObTableClient;
import com.alipay.oceanbase.rpc.mutation.Row;
import com.alipay.oceanbase.rpc.mutation.result.MutationResult;
import com.alipay.oceanbase.rpc.stream.QueryResultSet;
import com.alipay.oceanbase.rpc.table.api.TableBatchOps;
import com.alipay.oceanbase.rpc.table.api.TableQuery;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.TimeCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by caojiajun on 2024/9/4
 */
public class OBKVClient implements KVClient {

    private static final Logger logger = LoggerFactory.getLogger(OBKVClient.class);

    private String tableName;
    private ObTableClient obTableClient;

    @Override
    public void init(String namespace) {
        String paramUrl = RedisKvConf.getString(namespace, "kv.obkv.param.url", null);
        String fullUserName = RedisKvConf.getString(namespace, "kv.obkv.full.user.name", null);
        String password = RedisKvConf.getString(namespace, "kv.obkv.password", null);
        String sysUserName = RedisKvConf.getString(namespace, "kv.obkv.sys.user.name", null);
        String sysPassword = RedisKvConf.getString(namespace, "kv.obkv.sys.password", null);

        try {
            this.tableName = RedisKvConf.getString(namespace, "kv.obkv.table.name", "camellia_kv");

            ObTableClient obTableClient = new ObTableClient();
            obTableClient.setFullUserName(fullUserName);
            obTableClient.setParamURL(paramUrl);
            obTableClient.setPassword(password);
            obTableClient.setSysUserName(sysUserName);
            obTableClient.setSysPassword(sysPassword);
            obTableClient.init();

            this.obTableClient = obTableClient;

            this.obTableClient.addRowKeyElement(tableName, new String[]{"slot", "k"});

            logger.info("OBKVClient init success, namespace = {}, paramUrl = {}, tableName = {}", namespace, paramUrl, tableName);
        } catch (Throwable e) {
            logger.error("OBKVClient init error, namespace = {}, paramUrl = {}", namespace, paramUrl, e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportTTL() {
        return true;
    }

    @Override
    public void put(int slot, byte[] key, byte[] value, long ttl) {
        try {
            Date t = new Date(TimeCache.currentMillis + ttl);
            obTableClient.insertOrUpdate(tableName, new Object[]{slot, key}, new String[]{"v", "t"}, new Object[]{value, t});
        } catch (Exception e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void put(int slot, byte[] key, byte[] value) {
        try {
            obTableClient.insertOrUpdate(tableName, new Object[]{slot, key}, new String[]{"v", "t"}, new Object[]{value, null});
        } catch (Exception e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchPut(int slot, List<KeyValue> list) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (KeyValue keyValue : list) {
                batch.insertOrUpdate(new Object[]{slot, keyValue.getKey()}, new String[]{"v", "t"}, new Object[]{keyValue.getValue(), null});
            }
            batch.execute();
        } catch (Exception e) {
            logger.error("batch put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public KeyValue get(int slot, byte[] key) {
        try {
            Map<String, Object> map = obTableClient.get(tableName, new Object[]{slot, key}, new String[]{"v"});
            if (map == null || map.isEmpty()) {
                return null;
            }
            Object o = map.get("v");
            return new KeyValue(key, (byte[]) o);
        } catch (Exception e) {
            logger.error("get error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean exists(int slot, byte[] key) {
        try {
            Map<String, Object> map = obTableClient.get(tableName, new Object[]{slot, key}, new String[]{"v"});
            if (map == null || map.isEmpty()) {
                return false;
            }
            Object o = map.get("v");
            return o != null;
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean[] exists(int slot, byte[]... keys) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (byte[] key : keys) {
                batch.get(new Object[]{slot, key}, new String[]{"v"});
            }
            List<Object> objects = batch.executeWithResult();

            boolean[] booleans = new boolean[keys.length];
            int index = 0;
            for (Object object : objects) {
                MutationResult result = (MutationResult) object;
                Row operationRow = result.getOperationRow();
                Map<String, Object> map = operationRow.getMap();
                if (map == null || map.isEmpty()) {
                    booleans[index] = false;
                } else {
                    Object o = map.get("v");
                    booleans[index] = o != null;
                }
                index ++;
            }
            return booleans;
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> batchGet(int slot, byte[]... keys) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (byte[] key : keys) {
                batch.get(new Object[]{slot, key}, new String[]{"v"});
            }
            List<Object> objects = batch.executeWithResult();

            List<KeyValue> list = new ArrayList<>(keys.length);
            int index = 0;
            for (Object object : objects) {
                MutationResult result = (MutationResult) object;
                Row operationRow = result.getOperationRow();
                Map<String, Object> map = operationRow.getMap();
                if (map == null || map.isEmpty()) {
                    index ++;
                    continue;
                } else {
                    Object o = map.get("v");
                    if (o == null) {
                        index ++;
                        continue;
                    }
                    list.add(new KeyValue(keys[index], (byte[]) o));
                }
                index ++;
            }
            return list;
        } catch (Exception e) {
            logger.error("batchGet error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void delete(int slot, byte[] key) {
        try {
            obTableClient.delete(tableName, new Object[]{slot, key});
        } catch (Exception e) {
            logger.error("delete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchDelete(int slot, byte[]... keys) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (byte[] key : keys) {
                batch.delete(new Object[]{slot, key});
            }
            batch.execute();
        } catch (Exception e) {
            logger.error("delete error", e);
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
        return true;
    }

    @Override
    public List<KeyValue> scanByPrefix(int slot, byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
            List<KeyValue> list = new ArrayList<>();
            if (sort == Sort.ASC) {
                TableQuery query = obTableClient.query(tableName)
                        .select("k", "v")
                        .scanOrder(true)
                        .addScanRange(new Object[]{slot, startKey}, includeStartKey, new Object[]{slot, BytesUtils.nextBytes(prefix)}, false)
                        .limit(limit);
                QueryResultSet execute = query.execute();
                while (execute.next()) {
                    Map<String, Object> row = execute.getRow();
                    if (!row.isEmpty()) {
                        byte[] key = (byte[]) row.get("k");
                        byte[] value = (byte[]) row.get("v");
                        if (key != null) {
                            if (BytesUtils.startWith(key, prefix)) {
                                list.add(new KeyValue(key, value));
                                if (list.size() >= limit) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } else {
                TableQuery query = obTableClient.query(tableName)
                        .select("k", "v")
                        .scanOrder(false)
                        .addScanRange(new Object[]{slot, BytesUtils.lastBytes(prefix)}, false, new Object[]{slot, startKey}, includeStartKey)
                        .limit(limit);
                QueryResultSet execute = query.execute();
                while (execute.next()) {
                    Map<String, Object> row = execute.getRow();
                    if (!row.isEmpty()) {
                        byte[] key = (byte[]) row.get("k");
                        byte[] value = (byte[]) row.get("v");
                        if (key != null) {
                            if (BytesUtils.startWith(key, prefix)) {
                                list.add(new KeyValue(key, value));
                                if (list.size() >= limit) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
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
            long count = 0;
            TableQuery query = obTableClient.query(tableName)
                    .select("k")
                    .scanOrder(true)
                    .addScanRange(new Object[]{slot, startKey}, includeStartKey, new Object[]{slot, BytesUtils.nextBytes(prefix)}, false);
            QueryResultSet execute = query.execute();
            while (execute.next()) {
                Map<String, Object> row = execute.getRow();
                if (!row.isEmpty()) {
                    byte[] key = (byte[]) row.get("k");
                    if (key != null) {
                        if (BytesUtils.startWith(key, prefix)) {
                            count ++;
                        } else {
                            break;
                        }
                    }
                }
            }
            return count;
        } catch (Exception e) {
            logger.error("scanByPrefix error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> scanByStartEnd(int slot, byte[] startKey, byte[] endKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
            List<KeyValue> list = new ArrayList<>();
            if (sort == Sort.ASC) {
                TableQuery query = obTableClient.query(tableName)
                        .select("k", "v")
                        .scanOrder(true)
                        .addScanRange(new Object[]{slot, startKey}, includeStartKey, new Object[]{slot, endKey}, false)
                        .limit(limit);
                QueryResultSet execute = query.execute();
                while (execute.next()) {
                    Map<String, Object> row = execute.getRow();
                    if (!row.isEmpty()) {
                        byte[] key = (byte[]) row.get("k");
                        byte[] value = (byte[]) row.get("v");
                        if (key != null) {
                            if (BytesUtils.startWith(key, prefix)) {
                                list.add(new KeyValue(key, value));
                                if (list.size() >= limit) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } else {
                TableQuery query = obTableClient.query(tableName)
                        .select("k", "v")
                        .scanOrder(false)
                        .addScanRange(new Object[]{slot, endKey}, false, new Object[]{slot, startKey}, includeStartKey)
                        .limit(limit);
                QueryResultSet execute = query.execute();
                while (execute.next()) {
                    Map<String, Object> row = execute.getRow();
                    if (!row.isEmpty()) {
                        byte[] key = (byte[]) row.get("k");
                        byte[] value = (byte[]) row.get("v");
                        if (key != null) {
                            if (BytesUtils.startWith(key, prefix)) {
                                list.add(new KeyValue(key, value));
                                if (list.size() >= limit) {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                    }
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
            long count = 0;
            TableQuery query = obTableClient.query(tableName)
                    .select("k")
                    .scanOrder(true)
                    .addScanRange(new Object[]{slot, startKey}, includeStartKey, new Object[]{slot, endKey}, false);
            QueryResultSet execute = query.execute();
            while (execute.next()) {
                Map<String, Object> row = execute.getRow();
                if (!row.isEmpty()) {
                    byte[] key = (byte[]) row.get("k");
                    if (key != null) {
                        if (BytesUtils.startWith(key, prefix)) {
                            count ++;
                        } else {
                            break;
                        }
                    }
                }
            }
            return count;
        } catch (Exception e) {
            logger.error("countByStartEnd error", e);
            throw new KvException(e);
        }
    }
}
