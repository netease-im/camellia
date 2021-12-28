package com.netease.nim.camellia.redis.samples;

import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;

/**
 *
 * Created by caojiajun on 2021/4/20
 */
public class TestJsonResourceTable {

    public static void testJsonResourceTable() {
        String json = "{\n" +
                "  \"type\": \"sharding\",\n" +
                "  \"operation\": {\n" +
                "    \"operationMap\": {\n" +
                "      \"4\": {\n" +
                "        \"read\": \"redis://password1@127.0.0.1:6379\",\n" +
                "        \"type\": \"rw_separate\",\n" +
                "        \"write\": {\n" +
                "          \"resources\": [\n" +
                "            \"redis://password1@127.0.0.1:6379\",\n" +
                "            \"redis://password2@127.0.0.1:6380\"\n" +
                "          ],\n" +
                "          \"type\": \"multi\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"0-2\": \"redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381\",\n" +
                "      \"1-3-5\": \"redis://password2@127.0.0.1:6380\"\n" +
                "    },\n" +
                "    \"bucketSize\": 6\n" +
                "  }\n" +
                "}";
        //ReadableResourceTableUtil的parseTable方法传入的字符串也可以是单个的地址，如：
        //ReadableResourceTableUtil.parseTable("redis://@127.0.0.1:6379");
        ResourceTable resourceTable = ReadableResourceTableUtil.parseTable(json);
        System.out.println(ReadableResourceTableUtil.readableResourceTable(resourceTable));
    }

    public static void main(String[] args) {
        testJsonResourceTable();
    }
}
