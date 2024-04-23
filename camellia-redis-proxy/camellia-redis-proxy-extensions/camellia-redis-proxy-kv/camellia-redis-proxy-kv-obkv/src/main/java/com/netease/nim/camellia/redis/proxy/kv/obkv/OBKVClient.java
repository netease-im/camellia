package com.netease.nim.camellia.redis.proxy.kv.obkv;

import com.alipay.oceanbase.rpc.ObTableClient;
import com.alipay.oceanbase.rpc.stream.QueryResultSet;
import com.alipay.oceanbase.rpc.table.api.TableBatchOps;
import com.alipay.oceanbase.rpc.table.api.TableQuery;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 *
 * Created by caojiajun on 2024/4/16
 */
public class OBKVClient implements KVClient {

    private static final Logger logger = LoggerFactory.getLogger(OBKVClient.class);

    private static final String row = "row";
    private static final String val = "val";

    private final ObTableClient obTableClient;
    private final String tableName;

    public OBKVClient() {
        try {
            obTableClient = new ObTableClient();
            obTableClient.setParamURL(ProxyDynamicConf.getString("kv.obkv.param.url", null));
            obTableClient.setFullUserName(ProxyDynamicConf.getString("kv.obkv.full.user.name", null));
            obTableClient.setPassword(ProxyDynamicConf.getString("kv.obkv.password", null));
            obTableClient.setSysUserName(ProxyDynamicConf.getString("kv.obkv.sys.user.name", null));
            obTableClient.setSysPassword(ProxyDynamicConf.getString("kv.obkv.sys.password", null));
            obTableClient.init();
            tableName = ProxyDynamicConf.getString("kv.obkv.table.name", "camellia_kv");
        } catch (Exception e) {
            logger.error("OBKVClient init error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void put(byte[] key, byte[] value) {
        try {
            obTableClient.insert(tableName, key, new String[]{val}, new Object[]{value});
        } catch (Exception e) {
            logger.error("put error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchPut(List<KeyValue> list) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (KeyValue keyValue : list) {
                batch.insert(keyValue.getKey(), new String[]{val}, new Object[]{keyValue.getValue()});
            }
            batch.execute();
        } catch (Exception e) {
            logger.error("batch put error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public KeyValue get(byte[] key) {
        try {
            Map<String, Object> map = obTableClient.get(tableName, key, new String[]{"val"});
            byte[] value = (byte[]) map.get("val");
            return new KeyValue(key, value);
        } catch (Exception e) {
            logger.error("get error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean exists(byte[] key) {
        try {
            Map<String, Object> map = obTableClient.get(tableName, key, new String[]{"val"});
            byte[] value = (byte[]) map.get("val");
            return value != null;
        } catch (Exception e) {
            logger.error("exists error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean[] exists(byte[]... keys) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (byte[] key : keys) {
                batch.get(key, new String[]{"val"});
            }
            List<Object> list = batch.executeWithResult();
            boolean[] result = new boolean[keys.length];
            for (int i=0; i<list.size(); i++) {
                Object o = list.get(i);
                if (o instanceof Map) {
                    result[i] = !((Map<?, ?>) o).isEmpty();
                } else {
                    result[i] = false;
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("exists error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> batchGet(byte[]... keys) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (byte[] key : keys) {
                batch.get(key, new String[]{"val"});
            }
            List<Object> list = batch.executeWithResult();
            List<KeyValue> result = new ArrayList<>(keys.length);
            for (int i=0; i<list.size(); i++) {
                Object o = list.get(i);
                if (o instanceof Map) {
                    Object val = ((Map<?, ?>) o).get("val");
                    if (val instanceof byte[]) {
                        result.add(new KeyValue(keys[i], (byte[]) val));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.error("batchGet error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public void delete(byte[] key) {
        try {
            obTableClient.delete(tableName, key);
        } catch (Exception e) {
            logger.error("delete error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchDelete(byte[]... keys) {
        try {
            TableBatchOps batch = obTableClient.batch(tableName);
            for (byte[] key : keys) {
                batch.delete(key);
            }
            batch.execute();
        } catch (Exception e) {
            logger.error("batchDelete error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
            TableQuery query = obTableClient.query(tableName);
            query.addScanRangeStartsWith(startKey);
            if (sort == Sort.ASC) {
                query.scanOrder(true);
            } else if (sort == Sort.DESC) {
                query.scanOrder(false);
            }
            List<KeyValue> list = new ArrayList<>(limit);
            query.addScanRangeStartsWith(new Object[]{startKey}, includeStartKey);
            query.select("row", "val");
            query.limit(limit);
            QueryResultSet execute = query.execute();
            while (execute.isHasMore()) {
                Map<String, Object> rowMap = execute.getRow();
                Object row = rowMap.get("row");
                Object val = rowMap.get("val");
                if (row instanceof byte[] && val instanceof byte[]) {
                    byte[] key = (byte[]) row;
                    byte[] value = (byte[]) val;
                    if (!includeStartKey && Arrays.equals(key, startKey)) {
                        continue;
                    }
                    if (BytesUtils.startWith(key, prefix)) {
                        list.add(new KeyValue(key, value));
                        if (list.size() >= limit) {
                            break;
                        }
                        boolean next = execute.next();
                        if (!next) {
                            break;
                        }
                        continue;
                    }
                }
                break;
            }
            return list;
        } catch (Exception e) {
            logger.error("scan error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public long count(byte[] startKey, byte[] prefix, boolean includeStartKey) {
        try {
            TableQuery query = obTableClient.query(tableName);
            query.addScanRangeStartsWith(startKey);
            long result = 0;
            query.addScanRangeStartsWith(new Object[]{startKey}, includeStartKey);
            query.select(row);
            QueryResultSet execute = query.execute();
            while (execute.isHasMore()) {
                Map<String, Object> rowMap = execute.getRow();
                Object rowObj = rowMap.get(row);
                if (rowObj instanceof byte[]) {
                    byte[] key = (byte[]) rowObj;
                    if (!includeStartKey && Arrays.equals(key, startKey)) {
                        continue;
                    }
                    if (BytesUtils.startWith(key, prefix)) {
                        result ++;
                        boolean next = execute.next();
                        if (!next) {
                            break;
                        }
                        continue;
                    }
                }
                break;
            }
            return result;
        } catch (Exception e) {
            logger.error("count error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] endKey, int limit, Sort sort, boolean includeStartKey, boolean includeEndKey) {
        try {
            TableQuery query = obTableClient.query(tableName);
            query.addScanRangeStartsWith(startKey);
            if (sort == Sort.ASC) {
                query.scanOrder(true);
            } else if (sort == Sort.DESC) {
                query.scanOrder(false);
            }
            List<KeyValue> list = new ArrayList<>(limit);
            query.addScanRangeStartsWith(new Object[]{startKey}, includeStartKey);
            query.addScanRangeEndsWith(new Object[]{endKey}, includeEndKey);
            query.select(row, val);
            query.limit(limit);
            QueryResultSet execute = query.execute();
            while (execute.isHasMore()) {
                Map<String, Object> rowMap = execute.getRow();
                Object rowObj = rowMap.get(row);
                Object valueObj = rowMap.get(val);
                if (rowObj instanceof byte[] && valueObj instanceof byte[]) {
                    byte[] key = (byte[]) rowObj;
                    byte[] value = (byte[]) valueObj;
                    if (!includeStartKey && Arrays.equals(key, startKey)) {
                        continue;
                    }
                    if (!includeEndKey && Arrays.equals(key, endKey)) {
                        continue;
                    }
                    list.add(new KeyValue(key, value));
                    if (list.size() >= limit) {
                        break;
                    }
                    boolean next = execute.next();
                    if (!next) {
                        break;
                    }
                    continue;
                }
                break;
            }
            return list;
        } catch (Exception e) {
            logger.error("scan error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }

    @Override
    public long count(byte[] startKey, byte[] endKey, boolean includeStartKey, boolean includeEndKey) {
        try {
            TableQuery query = obTableClient.query(tableName);
            query.addScanRangeStartsWith(startKey);
            long result = 0;
            query.addScanRangeStartsWith(new Object[]{startKey}, includeStartKey);
            query.addScanRangeEndsWith(new Object[]{endKey}, includeEndKey);
            query.select(row);
            QueryResultSet execute = query.execute();
            while (execute.isHasMore()) {
                Map<String, Object> rowMap = execute.getRow();
                Object rowObj = rowMap.get(row);
                if (rowObj instanceof byte[]) {
                    byte[] key = (byte[]) rowObj;
                    if (!includeStartKey && Arrays.equals(key, startKey)) {
                        continue;
                    }
                    if (!includeEndKey && Arrays.equals(key, endKey)) {
                        continue;
                    }
                    result ++;
                    boolean next = execute.next();
                    if (!next) {
                        break;
                    }
                    continue;
                }
                break;
            }
            return result;
        } catch (Exception e) {
            logger.error("count error, tableName = {}", tableName, e);
            throw new KvException(e);
        }
    }
}
