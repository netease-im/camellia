package com.netease.nim.camellia.hbase.samples;

import com.netease.nim.camellia.hbase.CamelliaHBaseTemplate;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Created by caojiajun on 2020/4/14.
 */
@SpringBootApplication
public class Application {

    @Autowired
    private CamelliaHBaseTemplate template;

    public void test() {
        //PUT操作
        Put put = new Put(Bytes.toBytes("rowKey"));
        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("qualifier"), Bytes.toBytes("value"));
        template.put("hbase_table_name", put);

        //DELETE操作
        Delete delete = new Delete(Bytes.toBytes("rowKey"));
        template.delete("hbase_table_name", delete);

        //GET操作
        Get get = new Get(Bytes.toBytes("rowKey"));
        Result result = template.get("hbase_table_name", get);
        //parse result

        //SCAN操作
        Scan scan = new Scan(Bytes.toBytes("startRowKey"), Bytes.toBytes("endRowKey"));
        scan.setCaching(50);
        scan.setSmall(true);
        ResultScanner resultScanner = template.scan("hbase_table_name", scan);
        for (Result result1 : resultScanner) {
            //parse result1
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}