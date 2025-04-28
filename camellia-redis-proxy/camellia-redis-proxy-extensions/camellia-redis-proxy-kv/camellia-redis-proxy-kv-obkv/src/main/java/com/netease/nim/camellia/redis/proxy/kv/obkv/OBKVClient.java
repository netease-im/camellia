package com.netease.nim.camellia.redis.proxy.kv.obkv;

import com.alipay.oceanbase.rpc.ObTableClient;
import com.alipay.oceanbase.rpc.filter.ObCompareOp;
import com.alipay.oceanbase.rpc.filter.ObTableValueFilter;
import com.alipay.oceanbase.rpc.mutation.Row;
import com.alipay.oceanbase.rpc.mutation.result.MutationResult;
import com.alipay.oceanbase.rpc.protocol.payload.impl.execute.mutate.ObTableQueryAndMutateRequest;
import com.alipay.oceanbase.rpc.stream.QueryResultSet;
import com.alipay.oceanbase.rpc.table.api.TableBatchOps;
import com.alipay.oceanbase.rpc.table.api.TableQuery;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.conf.RedisKvConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.exception.KvException;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/9/4
 */
public class OBKVClient implements KVClient {

    private static final Logger logger = LoggerFactory.getLogger(OBKVClient.class);

    private String tableName;
    private ObTableClient obTableClient;

    private ThreadPoolExecutor executor;
    private int batchSplitSize;

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

            int poolSize = RedisKvConf.getInt(namespace, "kv.obkv.batch.executor.pool.size", SysUtils.getCpuNum() * 2);
            int queueSize = RedisKvConf.getInt(namespace, "kv.obkv.batch.executor.queue.size", 32);
            executor = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(queueSize),
                    new CamelliaThreadFactory(OBKVClient.class), new ThreadPoolExecutor.CallerRunsPolicy());
            batchSplitSize = RedisKvConf.getInt(namespace, "kv.obkv.batch.split.size", 1000);
            ProxyDynamicConf.registerCallback(() -> {
                int newPoolSize = RedisKvConf.getInt(namespace, "kv.obkv.batch.executor.pool.size", SysUtils.getCpuNum() * 2);
                if (newPoolSize != executor.getCorePoolSize()) {
                    logger.info("kv.obkv.batch.executor.pool.size update, {} -> {}", executor.getCorePoolSize(), newPoolSize);
                }
                if (newPoolSize > executor.getCorePoolSize()) {
                    executor.setMaximumPoolSize(newPoolSize);
                    executor.setCorePoolSize(newPoolSize);
                } else if (newPoolSize < executor.getCorePoolSize()) {
                    executor.setCorePoolSize(newPoolSize);
                    executor.setMaximumPoolSize(newPoolSize);
                }
                int newBatchSplitSize = RedisKvConf.getInt(namespace, "kv.obkv.batch.split.size", 1000);
                if (batchSplitSize != newBatchSplitSize) {
                    logger.info("kv.obkv.batch.split.size update, {} -> {}", batchSplitSize, newBatchSplitSize);
                    batchSplitSize = newBatchSplitSize;
                }
            });

            int slowQueryMonitorThreshold = RedisKvConf.getInt(namespace, "kv.obkv.slow.query.monitor.threshold", 2000);
            obTableClient.setslowQueryMonitorThreshold(slowQueryMonitorThreshold);

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
        return false;
    }

    @Override
    public void put(int slot, byte[] key, byte[] value, long ttl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(int slot, byte[] key, byte[] value) {
        try {
            obTableClient.insertOrUpdate(tableName, new Object[]{slot, key}, new String[]{"v"}, new Object[]{value});
        } catch (Exception e) {
            logger.error("put error", e);
            throw new KvException(e);
        }
    }

    @Override
    public void batchPut(int slot, List<KeyValue> list) {
        try {
            if (list.size() > batchSplitSize) {
                List<List<KeyValue>> split = split(batchSplitSize, list);
                List<Future<?>> futures = new ArrayList<>();
                for (List<KeyValue> keyValues : split) {
                    Future<?> future = executor.submit(() -> {
                        try {
                            TableBatchOps batch = obTableClient.batch(tableName);
                            for (KeyValue keyValue : keyValues) {
                                batch.insertOrUpdate(new Object[]{slot, keyValue.getKey()}, new String[]{"v"}, new Object[]{keyValue.getValue()});
                            }
                            batch.execute();
                        } catch (Exception e) {
                            logger.error("batch put error", e);
                            throw new KvException(e);
                        }
                    });
                    futures.add(future);
                }
                for (Future<?> future : futures) {
                    future.get(30, TimeUnit.SECONDS);
                }
            } else {
                TableBatchOps batch = obTableClient.batch(tableName);
                for (KeyValue keyValue : list) {
                    batch.insertOrUpdate(new Object[]{slot, keyValue.getKey()}, new String[]{"v"}, new Object[]{keyValue.getValue()});
                }
                batch.execute();
            }
        } catch (Exception e) {
            logger.error("batch put error", e);
            throw new KvException(e);
        }
    }

    private List<List<KeyValue>> split(int splitSize, List<KeyValue> keyValueList) {
        List<List<KeyValue>> lists = new ArrayList<>();
        List<KeyValue> list = new ArrayList<>();
        for (KeyValue kv : keyValueList) {
            list.add(kv);
            if (list.size() >= splitSize) {
                lists.add(list);
                list = new ArrayList<>();
            }
        }
        if (!list.isEmpty()) {
            lists.add(list);
        }
        return lists;
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
            if (keys.length > batchSplitSize) {
                List<List<byte[]>> split = split(batchSplitSize, keys);
                List<Future<List<Boolean>>> futureList = new ArrayList<>();
                for (List<byte[]> subList : split) {
                    Future<List<Boolean>> future = executor.submit(() -> {
                        TableBatchOps batch = obTableClient.batch(tableName);
                        for (byte[] key : subList) {
                            batch.get(new Object[]{slot, key}, new String[]{"v"});
                        }
                        List<Object> objects = batch.executeWithResult();
                        List<Boolean> booleanList = new ArrayList<>(subList.size());
                        for (Object object : objects) {
                            MutationResult result = (MutationResult) object;
                            Row operationRow = result.getOperationRow();
                            Map<String, Object> map = operationRow.getMap();
                            if (map == null || map.isEmpty()) {
                                booleanList.add(false);
                            } else {
                                Object o = map.get("v");
                                booleanList.add(o != null);
                            }
                        }
                        return booleanList;
                    });
                    futureList.add(future);
                }
                boolean[] result = new boolean[keys.length];
                int index = 0;
                for (Future<List<Boolean>> future : futureList) {
                    List<Boolean> list = future.get(30, TimeUnit.SECONDS);
                    for (Boolean b : list) {
                        result[index] = b;
                        index ++;
                    }
                }
                return result;
            } else {
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
                    index++;
                }
                return booleans;
            }
        } catch (Exception e) {
            logger.error("exists error", e);
            throw new KvException(e);
        }
    }

    @Override
    public List<KeyValue> batchGet(int slot, byte[]... keys) {
        try {
            if (keys.length > batchSplitSize) {
                List<List<byte[]>> split = split(batchSplitSize, keys);
                List<Future<List<KeyValue>>> futureList = new ArrayList<>(split.size());
                for (List<byte[]> subList : split) {
                    Future<List<KeyValue>> future = executor.submit(() -> {
                        TableBatchOps batch = obTableClient.batch(tableName);
                        for (byte[] key : subList) {
                            batch.get(new Object[]{slot, key}, new String[]{"v"});
                        }
                        List<Object> objects = batch.executeWithResult();

                        List<KeyValue> list = new ArrayList<>(subList.size());
                        int index = 0;
                        for (Object object : objects) {
                            MutationResult result = (MutationResult) object;
                            Row operationRow = result.getOperationRow();
                            Map<String, Object> map = operationRow.getMap();
                            if (map == null || map.isEmpty()) {
                                index++;
                                continue;
                            } else {
                                Object o = map.get("v");
                                if (o == null) {
                                    index++;
                                    continue;
                                }
                                list.add(new KeyValue(subList.get(index), (byte[]) o));
                            }
                            index++;
                        }
                        return list;
                    });
                    futureList.add(future);
                }
                List<KeyValue> result = new ArrayList<>();
                for (Future<List<KeyValue>> future : futureList) {
                    result.addAll(future.get(30, TimeUnit.SECONDS));
                }
                return result;
            } else {
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
                        index++;
                        continue;
                    } else {
                        Object o = map.get("v");
                        if (o == null) {
                            index++;
                            continue;
                        }
                        list.add(new KeyValue(keys[index], (byte[]) o));
                    }
                    index++;
                }
                return list;
            }
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
            if (keys.length > batchSplitSize) {
                List<List<byte[]>> split = split(batchSplitSize, keys);
                List<Future<?>> futures = new ArrayList<>();
                for (List<byte[]> list : split) {
                    Future<?> future = executor.submit(() -> {
                        try {
                            TableBatchOps batch = obTableClient.batch(tableName);
                            for (byte[] key : list) {
                                batch.delete(new Object[]{slot, key});
                            }
                            batch.execute();
                        } catch (Exception e) {
                            logger.error("batchDelete error, slot = {}", slot, e);
                            throw new KvException(e);
                        }
                    });
                    futures.add(future);
                }
                for (Future<?> future : futures) {
                    future.get(30, TimeUnit.SECONDS);
                }
            } else {
                TableBatchOps batch = obTableClient.batch(tableName);
                for (byte[] key : keys) {
                    batch.delete(new Object[]{slot, key});
                }
                batch.execute();
            }
        } catch (Exception e) {
            logger.error("batchDelete error, slot = {}", slot, e);
            throw new KvException(e);
        }
    }

    private List<List<byte[]>> split(int splitSize, byte[]... keys) {
        List<List<byte[]>> lists = new ArrayList<>();
        List<byte[]> list = new ArrayList<>();
        for (byte[] key : keys) {
            list.add(key);
            if (list.size() >= splitSize) {
                lists.add(list);
                list = new ArrayList<>();
            }
        }
        if (!list.isEmpty()) {
            lists.add(list);
        }
        return lists;
    }

    @Override
    public boolean supportCheckAndDelete() {
        return true;
    }

    @Override
    public void checkAndDelete(int slot, byte[] key, byte[] value) {
        try {
            TableQuery tableQuery = obTableClient.query(tableName)
                    .select("slot", "k", "v")
                    .addScanRange(new Object[]{slot, key}, true, new Object[]{slot, key}, true)
                    .limit(1);
            ObTableValueFilter filter = new ObTableValueFilter(ObCompareOp.EQ, "v", value);
            tableQuery.setFilter(filter);
            ObTableQueryAndMutateRequest request = obTableClient.obTableQueryAndDelete(tableQuery);
            obTableClient.execute(request);
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
