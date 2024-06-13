package com.netease.nim.camellia.redis.proxy.kv.obkv;

import com.alipay.oceanbase.hbase.OHTableClient;
import com.alipay.oceanbase.hbase.constants.OHConstants;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 *
 * Created by caojiajun on 2024/4/16
 */
public class OBKVHBaseClient implements KVClient {

    private static final Logger logger = LoggerFactory.getLogger(OBKVHBaseClient.class);

    private static final byte[] cf = "d".getBytes(StandardCharsets.UTF_8);
    private static final byte[] column = "v".getBytes(StandardCharsets.UTF_8);

    private final OHTableClient tableClient;

    public OBKVHBaseClient() {
        try {
            String fullUserName = ProxyDynamicConf.getString("kv.obkv.full.user.name", null);
            String paramUrl = ProxyDynamicConf.getString("kv.obkv.param.url", null);
            String password = ProxyDynamicConf.getString("kv.obkv.password", null);
            String sysUserName = ProxyDynamicConf.getString("kv.obkv.sys.user.name", null);
            String sysPassword = ProxyDynamicConf.getString("kv.obkv.sys.password", null);

            Configuration conf = new Configuration();
            conf.set(OHConstants.HBASE_OCEANBASE_PARAM_URL, paramUrl);
            conf.set(OHConstants.HBASE_OCEANBASE_FULL_USER_NAME, fullUserName);
            conf.set(OHConstants.HBASE_OCEANBASE_PASSWORD, password);
            conf.set(OHConstants.HBASE_OCEANBASE_SYS_USER_NAME, sysUserName);
            conf.set(OHConstants.HBASE_OCEANBASE_SYS_PASSWORD, sysPassword);

            String tableName = ProxyDynamicConf.getString("kv.obkv.table.name", "camellia_kv");

            tableClient = new OHTableClient(tableName, conf);
            tableClient.init();
        } catch (Exception e) {
            logger.error("OHTableClient init error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportTTL() {
        return false;
    }

    @Override
    public void put(byte[] key, byte[] value, long ttl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(byte[] key, byte[] value) {
        try {
            Put put = new Put(key);
            put.add(cf, column, value);
            tableClient.put(put);
        } catch (IOException e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchPut(List<KeyValue> list) {
        try {
            List<Put> putList = new ArrayList<>();
            for (KeyValue keyValue : list) {
                Put put = new Put(keyValue.getKey());
                put.add(cf, column, keyValue.getValue());
                putList.add(put);
            }
            tableClient.put(putList);
        } catch (IOException e) {
            logger.error("batchPut error", e);
            throw new KvException(e);
        }
    }

    @Override
    public KeyValue get(byte[] key) {
        try {
            Get get = new Get(key);
            Result result = tableClient.get(get);
            if (result == null) {
                return null;
            }
            byte[] value = result.getValue(cf, column);
            if (value == null) {
                return null;
            }
            return new KeyValue(key, value);
        } catch (IOException e) {
            logger.error("get error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean exists(byte[] key) {
        try {
            Get get = new Get(key);
            return tableClient.exists(get);
        } catch (IOException e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean[] exists(byte[]... keys) {
        try {
            boolean[] results = new boolean[keys.length];
            int i=0;
            for (byte[] key : keys) {
                results[i] = tableClient.exists(new Get(key));
                i++;
            }
            return results;
        } catch (IOException e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> batchGet(byte[]... keys) {
        try {
            List<Get> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Get get = new Get(key);
                list.add(get);
            }
            List<KeyValue> keyValues = new ArrayList<>(list.size());
            Result[] results = tableClient.get(list);
            for (Result result : results) {
                if (result == null) continue;
                byte[] key = result.getRow();
                byte[] value = result.getValue(cf, column);
                keyValues.add(new KeyValue(key, value));
            }
            return keyValues;
        } catch (IOException e) {
            logger.error("batchGet error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void delete(byte[] key) {
        try {
            Delete delete = new Delete(key);
            tableClient.delete(delete);
        } catch (IOException e) {
            logger.error("delete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchDelete(byte[]... keys) {
        try {
            List<Delete> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                list.add(new Delete(key));
            }
            tableClient.delete(list);
        } catch (IOException e) {
            logger.error("batchDelete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportCheckAndDelete() {
        return true;
    }

    @Override
    public void checkAndDelete(byte[] key, byte[] value) {
        try {
            tableClient.checkAndDelete(key, cf, column, value, new Delete(key));
        } catch (IOException e) {
            logger.error("checkAndDelete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportReverseScan() {
        return false;
    }

    @Override
    public List<KeyValue> scanByPrefix(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try {
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setCaching(limit);
            scan.setSmall(true);
            try (ResultScanner scanner = tableClient.getScanner(scan)) {
                List<KeyValue> list = new ArrayList<>();
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    if (BytesUtils.startWith(row, prefix)) {
                        byte[] key = result.getRow();
                        byte[] value = result.getValue(cf, column);
                        list.add(new KeyValue(key, value));
                        if (list.size() >= limit) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                return list;
            }
        } catch (IOException e) {
            logger.error("scan error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long countByPrefix(byte[] startKey, byte[] prefix, boolean includeStartKey) {
        try {
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setSmall(true);
            int count = 0;
            try (ResultScanner scanner = tableClient.getScanner(scan)) {
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    if (BytesUtils.startWith(row, prefix)) {
                        count++;
                    } else {
                        break;
                    }
                }
                return count;
            }
        } catch (IOException e) {
            logger.error("count error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> scanByStartEnd(byte[] startKey, byte[] endKey, int limit, Sort sort, boolean includeStartKey) {
        try {
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setStopRow(endKey);
            scan.setCaching(limit);
            scan.setSmall(true);
            try (ResultScanner scanner = tableClient.getScanner(scan)) {
                List<KeyValue> list = new ArrayList<>();
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    byte[] key = result.getRow();
                    byte[] value = result.getValue(cf, column);
                    list.add(new KeyValue(key, value));
                    if (list.size() >= limit) {
                        break;
                    }
                }
                return list;
            }
        } catch (IOException e) {
            logger.error("scan error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long countByStartEnd(byte[] startKey, byte[] endKey, boolean includeStartKey) {
        try {
            Scan scan = new Scan();
            scan.setStartRow(startKey);
            scan.setStopRow(endKey);
            scan.setSmall(true);
            int count = 0;
            try (ResultScanner scanner = tableClient.getScanner(scan)) {
                for (Result result : scanner) {
                    byte[] row = result.getRow();
                    if (!includeStartKey && Arrays.equals(row, startKey)) {
                        continue;
                    }
                    count++;
                }
                return count;
            }
        } catch (IOException e) {
            logger.error("count error", e);
            throw new KvException(e);
        }
    }
}
