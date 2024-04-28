package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/28
 */
public class TestHBase {

    private static final byte[] cf = "d".getBytes(StandardCharsets.UTF_8);
    private static final byte[] column1 = "v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] column2 = "v2".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) throws InterruptedException {
        String url = "hbase://127.0.0.1:2181/hbase";

        HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
        CamelliaHBaseTemplate template = new CamelliaHBaseTemplate(hBaseResource);

        String table = "camellia_kv";

        Put put1 = new Put("k1".getBytes(StandardCharsets.UTF_8));
        put1.addColumn(cf, column1, "v1".getBytes(StandardCharsets.UTF_8));
        put1.setTTL(10*1000L);

        Put put11 = new Put("k1".getBytes(StandardCharsets.UTF_8));
        put11.addColumn(cf, column2, "v11".getBytes(StandardCharsets.UTF_8));
        put11.setTTL(20*1000L);

        Put put2 = new Put("k2".getBytes(StandardCharsets.UTF_8));
        put2.addColumn(cf, column1, "v2".getBytes(StandardCharsets.UTF_8));
        put2.setTTL(20*1000L);

        Put put3 = new Put("k3".getBytes(StandardCharsets.UTF_8));
        put3.addColumn(cf, column1, "v3".getBytes(StandardCharsets.UTF_8));

        template.put(table, put1);
        template.put(table, put11);
        template.put(table, put2);
        template.put(table, put3);

        while (true) {
            Result result1 = template.get(table, new Get("k1".getBytes(StandardCharsets.UTF_8)));
            System.out.println("k1=" + parseValue(result1));
            Result result2 = template.get(table, new Get("k2".getBytes(StandardCharsets.UTF_8)));
            System.out.println("k2=" + parseValue(result2));
            Result result3 = template.get(table, new Get("k3".getBytes(StandardCharsets.UTF_8)));
            System.out.println("k3=" + parseValue(result3));

            Thread.sleep(3*1000L);
        }
    }

    private static String parseValue(Result result) {
        if (result == null) {
            return null;
        }
        byte[] value1 = result.getValue(cf, column1);
        String value1Str = value1 == null ? null : new String(value1, StandardCharsets.UTF_8);
        byte[] value2 = result.getValue(cf, column2);
        String value2Str = value2 == null ? null : new String(value2, StandardCharsets.UTF_8);
        return value1Str + "_"  + value2Str;
    }
}
