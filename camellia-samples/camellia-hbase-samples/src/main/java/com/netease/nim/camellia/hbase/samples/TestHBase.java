package com.netease.nim.camellia.hbase.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import com.netease.nim.camellia.hbase.resource.HBaseResource;
import com.netease.nim.camellia.hbase.util.HBaseResourceUtil;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;

import java.nio.charset.StandardCharsets;

/**
 * Created by caojiajun on 2024/4/10
 */
public class TestHBase {

    public static void main(String[] args) {
        String string = "hbase://127.0.0.1:2181/hbase";
        HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(string));
        CamelliaHBaseTemplate template = new CamelliaHBaseTemplate(hBaseResource);

//        for (int i=1; i<120; i++) {
//            byte[] key = "abcqwer".getBytes(StandardCharsets.UTF_8);
//            key = BytesUtils.merge(key, BytesUtils.toBytes(i));
//            byte[] value = String.valueOf(i).getBytes(StandardCharsets.UTF_8);
//            put(template, key, value);
//        }


        int batch = 50;
        byte[] key = "abcqwer".getBytes(StandardCharsets.UTF_8);

//        put(template, key, "0".getBytes(StandardCharsets.UTF_8));

        for (int j=0; j<3; j++) {
            Scan scan = new Scan();
            scan.setStartRow(key);
            scan.setCaching(200);
            scan.setSmall(true);
            scan.setReversed(true);
            ResultScanner scanner = template.scan("test", scan);
            int i = 0;
            for (Result result : scanner) {
                byte[] row = result.getRow();
                byte[] value = result.getValue("d".getBytes(StandardCharsets.UTF_8), "v".getBytes(StandardCharsets.UTF_8));
                System.out.println(new String(value));
                i++;
                if (i == batch) {
                    key = row;
                    break;
                }
            }
            System.out.println("=====");
        }
    }

    private static void put(CamelliaHBaseTemplate template, byte[] key, byte[] value) {
        Put put = new Put(key);
        put.addColumn("d".getBytes(StandardCharsets.UTF_8), "v".getBytes(StandardCharsets.UTF_8), value);
        template.put("test", put);
    }
}
