package com.netease.nim.camellia.redis.proxy.kv.obkv;

import com.alipay.oceanbase.hbase.OHTablePool;
import com.alipay.oceanbase.hbase.constants.OHConstants;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.PoolMap;
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

    private OHTablePool ohTablePool;
    private String tableName;

    @Override
    public void init(String namespace) {
        String paramUrl = RedisKvConf.getString(namespace, "kv.obkv.param.url", null);
        String fullUserName = RedisKvConf.getString(namespace, "kv.obkv.full.user.name", null);
        String password = RedisKvConf.getString(namespace, "kv.obkv.password", null);
        String sysUserName = RedisKvConf.getString(namespace, "kv.obkv.sys.user.name", null);
        String sysPassword = RedisKvConf.getString(namespace, "kv.obkv.sys.password", null);
        try {
            Configuration conf = new Configuration();
            conf.set(OHConstants.HBASE_OCEANBASE_PARAM_URL, paramUrl);
            conf.set(OHConstants.HBASE_OCEANBASE_FULL_USER_NAME, fullUserName);
            conf.set(OHConstants.HBASE_OCEANBASE_PASSWORD, password);
            conf.set(OHConstants.HBASE_OCEANBASE_SYS_USER_NAME, sysUserName);
            conf.set(OHConstants.HBASE_OCEANBASE_SYS_PASSWORD, sysPassword);

            tableName = RedisKvConf.getString(namespace, "kv.obkv.table.name", "camellia_kv");

            int maxSize = RedisKvConf.getInt(namespace, "kv.obkv.max.size", SysUtils.getCpuNum() * 4 + 10);
            PoolMap.PoolType poolType;
            String string = RedisKvConf.getString(namespace, "kv.obkv.poolType", PoolMap.PoolType.ThreadLocal.name());
            if (string.equalsIgnoreCase(PoolMap.PoolType.ThreadLocal.name())) {
                poolType = PoolMap.PoolType.ThreadLocal;
            } else if (string.equalsIgnoreCase(PoolMap.PoolType.RoundRobin.name())) {
                poolType = PoolMap.PoolType.RoundRobin;
            } else if (string.equalsIgnoreCase(PoolMap.PoolType.Reusable.name())) {
                poolType = PoolMap.PoolType.Reusable;
            } else {
                poolType = PoolMap.PoolType.ThreadLocal;
            }
            ohTablePool = new OHTablePool(conf, maxSize, poolType);

            logger.info("OHTableClient init success, namespace = {}, paramUrl = {}, tableName = {}, poolType = {}, maxSize = {}",
                    namespace, paramUrl, tableName, poolType, maxSize);
        } catch (Throwable e) {
            logger.error("OHTableClient init error, namespace = {}, paramUrl = {}", namespace, paramUrl, e);
            throw new KvException(e);
        }
    }

    private HTableInterface getTable() {
        return ohTablePool.getTable(tableName);
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
        try (HTableInterface table = getTable()) {
            Put put = new Put(key);
            put.add(cf, column, value);
            table.put(put);
        } catch (IOException e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchPut(List<KeyValue> list) {
        try (HTableInterface table = getTable()) {
            List<Put> putList = new ArrayList<>();
            for (KeyValue keyValue : list) {
                Put put = new Put(keyValue.getKey());
                put.add(cf, column, keyValue.getValue());
                putList.add(put);
            }
            table.put(putList);
        } catch (IOException e) {
            logger.error("batchPut error", e);
            throw new KvException(e);
        }
    }

    @Override
    public KeyValue get(byte[] key) {
        try (HTableInterface table = getTable()) {
            Get get = new Get(key);
            get.addFamily(cf);
            Result result = table.get(get);
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
        try (HTableInterface table = getTable()) {
            Get get = new Get(key);
            get.addFamily(cf);
            return table.exists(get);
        } catch (IOException e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean[] exists(byte[]... keys) {
        try (HTableInterface table = getTable()) {
            boolean[] results = new boolean[keys.length];
            int i=0;
            for (byte[] key : keys) {
                Get get = new Get(key);
                get.addFamily(cf);
                results[i] = table.exists(get);
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
        try (HTableInterface table = getTable()) {
            List<Get> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Get get = new Get(key);
                get.addFamily(cf);
                list.add(get);
            }
            List<KeyValue> keyValues = new ArrayList<>(list.size());
            Result[] results = table.get(list);
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
        try (HTableInterface table = getTable()) {
            Delete delete = new Delete(key);
            delete.deleteFamily(cf);
            table.delete(delete);
        } catch (IOException e) {
            logger.error("delete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchDelete(byte[]... keys) {
        try (HTableInterface table = getTable()) {
            List<Delete> list = new ArrayList<>(keys.length);
            for (byte[] key : keys) {
                Delete delete = new Delete(key);
                delete.deleteFamily(cf);
                list.add(delete);
            }
            table.delete(list);
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
        try (HTableInterface table = getTable()) {
            Delete delete = new Delete(key);
            delete.deleteFamily(cf);
            table.checkAndDelete(key, cf, column, value, delete);
        } catch (IOException e) {
            logger.error("checkAndDelete error", e);
            throw new KvException(e);
        }
    }

    @Override
    public boolean supportReverseScan() {
        return true;
    }

    @Override
    public List<KeyValue> scanByPrefix(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try (HTableInterface table = getTable()) {
            Scan scan = new Scan();
            scan.addFamily(cf);
            scan.setStartRow(startKey);
            scan.setCaching(includeStartKey ? limit : (limit + 1));
            scan.setSmall(true);
            if (sort == Sort.ASC) {
                scan.setStopRow(BytesUtils.nextBytes(prefix));
            } else {
                scan.setStopRow(BytesUtils.lastBytes(prefix));
            }
            scan.setReversed(sort != Sort.ASC);
            try (ResultScanner scanner = table.getScanner(scan)) {
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
            logger.error("scanByPrefix error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long countByPrefix(byte[] startKey, byte[] prefix, boolean includeStartKey) {
        try (HTableInterface table = getTable()) {
            Scan scan = new Scan();
            scan.addFamily(cf);
            scan.setStartRow(startKey);
            scan.setSmall(true);
            scan.setStopRow(BytesUtils.nextBytes(prefix));
            int count = 0;
            try (ResultScanner scanner = table.getScanner(scan)) {
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
            logger.error("countByPrefix error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> scanByStartEnd(byte[] startKey, byte[] endKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        try (HTableInterface table = getTable()) {
            Scan scan = new Scan();
            scan.addFamily(cf);
            scan.setStartRow(startKey);
            scan.setStopRow(endKey);
            scan.setCaching(includeStartKey ? limit : (limit + 1));
            scan.setSmall(true);
            scan.setReversed(sort != Sort.ASC);
            try (ResultScanner scanner = table.getScanner(scan)) {
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
                    list.add(new KeyValue(key, value));
                    if (list.size() >= limit) {
                        break;
                    }
                }
                return list;
            }
        } catch (IOException e) {
            logger.error("scanByStartEnd error", e);
            throw new KvException(e);
        }
    }

    @Override
    public long countByStartEnd(byte[] startKey, byte[] endKey, byte[] prefix, boolean includeStartKey) {
        try (HTableInterface table = getTable()) {
            Scan scan = new Scan();
            scan.addFamily(cf);
            scan.setStartRow(startKey);
            scan.setStopRow(endKey);
            scan.setSmall(true);
            int count = 0;
            try (ResultScanner scanner = table.getScanner(scan)) {
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
        } catch (IOException e) {
            logger.error("countByStartEnd error", e);
            throw new KvException(e);
        }
    }
}
