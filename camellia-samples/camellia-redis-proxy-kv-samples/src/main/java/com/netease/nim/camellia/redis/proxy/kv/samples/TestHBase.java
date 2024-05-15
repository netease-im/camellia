package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import org.apache.hadoop.hbase.client.*;

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

        byte[] row0 = new byte[] {1, 1, 1, -2};//次大 00
        byte[] row1 = new byte[] {1, 1, 1, -1};//最大 11
        byte[] row2 = new byte[] {1, 1, 1, 0};//小 22
        byte[] row3 = new byte[] {1, 1, 1, 1};//中 33

        System.out.println(BytesUtils.compare(row0, row1));
        System.out.println(BytesUtils.compare(row0, row2));
        System.out.println(BytesUtils.compare(row0, row3));
        System.out.println(BytesUtils.compare(row1, row2));
        System.out.println(BytesUtils.compare(row2, row3));
        System.out.println(BytesUtils.compare(row1, row3));

        System.out.println("==");

        Put put0 = new Put(row0);
        put0.addColumn(cf, column1, "00".getBytes(StandardCharsets.UTF_8));

        Put put1 = new Put(row1);
        put1.addColumn(cf, column1, "11".getBytes(StandardCharsets.UTF_8));

        Put put2 = new Put(row2);
        put2.addColumn(cf, column1, "22".getBytes(StandardCharsets.UTF_8));

        Put put3 = new Put(row3);
        put3.addColumn(cf, column1, "33".getBytes(StandardCharsets.UTF_8));

        template.put(table, put0);
        template.put(table, put1);
        template.put(table, put2);
        template.put(table, put3);

//        for (int i=1; i<10; i++) {
//            Put put0 = new Put(("q" + i).getBytes(StandardCharsets.UTF_8));
//            put0.addColumn(cf, column1, ("v" + i).getBytes(StandardCharsets.UTF_8));
//            template.put(table, put0);
//        }

        Scan scan = new Scan();
        scan.setStartRow(row2);
//        scan.setStopRow("q5".getBytes(StandardCharsets.UTF_8));
        ResultScanner scan1 = template.scan(table, scan);
        for (Result result : scan1) {
            byte[] value = result.getValue(cf, column1);
            if (value == null) {
                break;
            }
            System.out.println(new String(value));
        }
//        put0.setTTL(10*1000L);

//        template.put(table, put0);
//
//        Put put1 = new Put("k1".getBytes(StandardCharsets.UTF_8));
//        put1.addColumn(cf, column1, "v1".getBytes(StandardCharsets.UTF_8));
//        put1.setTTL(5*1000L);
//
//        template.put(table, put1);
//
//        int i=10;
//        while (i-->0) {
//            Result result = template.get(table, new Get("k1".getBytes(StandardCharsets.UTF_8)));
//            System.out.println(parseValue(result));
//            Thread.sleep(1000);
//        }
//
//        template.delete(table, new Delete("k1".getBytes(StandardCharsets.UTF_8)));
//
//        int j=10;
//        while (j-->0) {
//            Result result = template.get(table, new Get("k1".getBytes(StandardCharsets.UTF_8)));
//            System.out.println(parseValue(result));
//            Thread.sleep(1000);
//        }

//        Put put11 = new Put("k1".getBytes(StandardCharsets.UTF_8));
//        put11.addColumn(cf, column2, "v11".getBytes(StandardCharsets.UTF_8));
//        put11.setTTL(20*1000L);
//
//        Put put2 = new Put("k2".getBytes(StandardCharsets.UTF_8));
//        put2.addColumn(cf, column1, "v2".getBytes(StandardCharsets.UTF_8));
//        put2.setTTL(20*1000L);
//
//        Put put3 = new Put("k3".getBytes(StandardCharsets.UTF_8));
//        put3.addColumn(cf, column1, "v3".getBytes(StandardCharsets.UTF_8));
//
//        template.put(table, put1);
//        template.put(table, put11);
//        template.put(table, put2);
//        template.put(table, put3);
//
//        while (true) {
//            Result result1 = template.get(table, new Get("k1".getBytes(StandardCharsets.UTF_8)));
//            System.out.println("k1=" + parseValue(result1));
//            Result result2 = template.get(table, new Get("k2".getBytes(StandardCharsets.UTF_8)));
//            System.out.println("k2=" + parseValue(result2));
//            Result result3 = template.get(table, new Get("k3".getBytes(StandardCharsets.UTF_8)));
//            System.out.println("k3=" + parseValue(result3));
//
//            Thread.sleep(3*1000L);
//        }
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
