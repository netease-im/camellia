package com.netease.nim.camellia.redis.proxy.kv.hbase;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KVClient;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.KeyValue;
import com.netease.nim.camellia.redis.proxy.upstream.kv.kv.Sort;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import org.apache.hadoop.hbase.client.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by caojiajun on 2024/4/8
 */
public class HBaseKVClient implements KVClient {

    private static final byte[] cf = "d".getBytes(StandardCharsets.UTF_8);
    private static final byte[] column = "v".getBytes(StandardCharsets.UTF_8);

    private final String tableName;
    private final CamelliaHBaseTemplate template;

    public HBaseKVClient() {
        String string = ProxyDynamicConf.getString("kv.store.hbase.url", null);
        HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(string));
        template = new CamelliaHBaseTemplate(hBaseResource);
        tableName = ProxyDynamicConf.getString("kv.store.hbase.table.name", "camellia_kv");
    }

    @Override
    public void put(byte[] key, byte[] value) {
        Put put = new Put(key);
        put.addColumn(cf, column, value);
        template.put(tableName, put);
    }

    @Override
    public void batchPut(List<KeyValue> list) {
        List<Put> putList = new ArrayList<>();
        for (KeyValue keyValue : list) {
            Put put = new Put(keyValue.getKey());
            put.addColumn(cf, column, keyValue.getValue());
            putList.add(put);
        }
        template.put(tableName, putList);
    }

    @Override
    public KeyValue get(byte[] key) {
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
    }

    @Override
    public boolean exists(byte[] key) {
        Get get = new Get(key);
        return template.exists(tableName, get);
    }

    @Override
    public boolean[] exists(byte[]... keys) {
        List<Get> getList = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            getList.add(new Get(key));
        }
        return template.existsAll(tableName, getList);
    }

    @Override
    public List<KeyValue> batchGet(byte[]... keys) {
        List<Get> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            Get get = new Get(key);
            list.add(get);
        }
        List<KeyValue> keyValues = new ArrayList<>(list.size());
        Result[] results = template.get(tableName, list);
        for (Result result : results) {
            if (result == null) continue;
            byte[] key = result.getRow();
            byte[] value = result.getValue(cf, column);
            keyValues.add(new KeyValue(key, value));
        }
        return keyValues;
    }

    @Override
    public void delete(byte[] key) {
        Delete delete = new Delete(key);
        template.delete(tableName, delete);
    }

    @Override
    public void batchDelete(byte[]... keys) {
        List<Delete> list = new ArrayList<>(keys.length);
        for (byte[] key : keys) {
            list.add(new Delete(key));
        }
        template.delete(tableName, list);
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] prefix, int limit, Sort sort, boolean includeStartKey) {
        Scan scan = new Scan();
        scan.setStartRow(startKey);
        scan.setCaching(limit);
        scan.setSmall(true);
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
    }

    @Override
    public long count(byte[] startKey, byte[] prefix, boolean includeStartKey) {
        Scan scan = new Scan();
        scan.setStartRow(startKey);
        scan.setSmall(true);
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
    }

    @Override
    public List<KeyValue> scan(byte[] startKey, byte[] endKey, int limit, Sort sort, boolean includeStartKey, boolean includeEndKey) {
        Scan scan = new Scan();
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.setCaching(limit);
        scan.setSmall(true);
        scan.setReversed(sort != Sort.ASC);
        try (ResultScanner scanner = template.scan(tableName, scan)) {
            List<KeyValue> list = new ArrayList<>();
            for (Result result : scanner) {
                byte[] row = result.getRow();
                if (!includeStartKey && Arrays.equals(row, startKey)) {
                    continue;
                }
                if (!includeEndKey && Arrays.equals(row, endKey)) {
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
    }

    @Override
    public long count(byte[] startKey, byte[] endKey, boolean includeStartKey, boolean includeEndKey) {
        Scan scan = new Scan();
        scan.setStartRow(startKey);
        scan.setStopRow(endKey);
        scan.setSmall(true);
        int count = 0;
        try (ResultScanner scanner = template.scan(tableName, scan)) {
            for (Result result : scanner) {
                byte[] row = result.getRow();
                if (!includeStartKey && Arrays.equals(row, startKey)) {
                    continue;
                }
                if (!includeEndKey && Arrays.equals(row, endKey)) {
                    continue;
                }
                count ++;
            }
            return count;
        }
    }
}
