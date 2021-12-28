package com.netease.nim.camellia.dashboard.samples;

import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import com.netease.nim.camellia.core.util.CheckUtil;
import com.netease.nim.camellia.core.util.ReadableResourceTableUtil;
import com.netease.nim.camellia.core.util.ResourceTableUtil;

import java.util.*;

/**
 *
 * Created by caojiajun on 2019/12/25.
 */
public class ResourceTableJsonSamples {

    private static void simple(String url) {
        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource(url));

        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException();
        }
        System.out.println(ReadableResourceTableUtil.readableResourceTable(resourceTable));
    }

    private static void rwSeperate(String readUrl, String writeUrl) {
        ResourceTable resourceTable = ResourceTableUtil.simpleRwSeparateTable(new Resource(readUrl), new Resource(writeUrl));

        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException();
        }
        System.out.println(ReadableResourceTableUtil.readableResourceTable(resourceTable));
    }

    private static void w2r1(String readUrl, String writeUrl1, String writeUrl2) {
        ResourceTable resourceTable = ResourceTableUtil.simple2W1RTable(new Resource(readUrl), new Resource(writeUrl1), new Resource(writeUrl2));

        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException();
        }
        System.out.println(ReadableResourceTableUtil.readableResourceTable(resourceTable));
    }

    private static void simpleShading(Map<Integer, String> urlMap, int bucketSize) {
        Map<Integer, Resource> resourceMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : urlMap.entrySet()) {
            resourceMap.put(entry.getKey(), new Resource(entry.getValue()));
        }
        ResourceTable resourceTable = ResourceTableUtil.simpleShadingTable(resourceMap, bucketSize);

        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException();
        }
        System.out.println(ReadableResourceTableUtil.readableResourceTable(resourceTable));
    }

    private static void complexShading(Map<Integer, String> simpleUrlMap, Map<Integer, String[]> w2UrlMap, int bucketSize) {
        Map<Integer, ResourceOperation> operationMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : simpleUrlMap.entrySet()) {
            operationMap.put(entry.getKey(), new ResourceOperation(new Resource(entry.getValue())));
        }
        for (Map.Entry<Integer, String[]> entry : w2UrlMap.entrySet()) {
            String[] value = entry.getValue();
            ResourceReadOperation readOperation = new ResourceReadOperation(new Resource(value[0]));
            List<Resource> writeResources = new ArrayList<>();
            for (String url : value) {
                writeResources.add(new Resource(url));
            }
            ResourceWriteOperation writeOperation = new ResourceWriteOperation(writeResources);
            ResourceOperation resourceOperation = new ResourceOperation(readOperation, writeOperation);
            operationMap.put(entry.getKey(), resourceOperation);
        }

        ResourceTable.ShadingTable shadingTable = new ResourceTable.ShadingTable();
        shadingTable.setBucketSize(bucketSize);
        shadingTable.setResourceOperationMap(operationMap);
        ResourceTable resourceTable = new ResourceTable(shadingTable);

        boolean check = CheckUtil.checkResourceTable(resourceTable);
        if (!check) {
            throw new IllegalArgumentException();
        }
        System.out.println(ReadableResourceTableUtil.readableResourceTable(resourceTable));
    }

    public static void main(String[] args) {

        //
        simple("redis://password@127.0.0.1:6379");

        //
        rwSeperate("redis://read@127.0.0.1:6379", "redis://write@127.0.0.1:6380");

        //
        w2r1("redis://read@127.0.0.1:6379", "redis://read@127.0.0.1:6379", "redis://write@127.0.0.1:6380");

        //
        Map<Integer, String> urlMap = new HashMap<>();
        int bucketSize = 6;
        for (int i=0; i<bucketSize; i++) {
            if (i % 2 == 0) {
                urlMap.put(i, "redis://password1@127.0.0.1:6379");
            } else {
                urlMap.put(i, "redis://password2@127.0.0.1:6380");
            }
        }
        simpleShading(urlMap, bucketSize);

        //
        Map<Integer, String> simpleUrlMap = new HashMap<>();
        int bucketSize2 = 6;
        for (int i=0; i<bucketSize2; i++) {
            if (i == 4) continue;
            if (i % 2 == 0) {
                simpleUrlMap.put(i, "redis://password1@127.0.0.1:6379");
            } else {
                simpleUrlMap.put(i, "redis://password2@127.0.0.1:6380");
            }
        }
        Map<Integer, String[]> w2UrlMap = new HashMap<>();
        w2UrlMap.put(4, new String[] {"redis://password1@127.0.0.1:6379", "redis://password2@127.0.0.1:6380"});
        complexShading(simpleUrlMap, w2UrlMap, bucketSize2);
    }
}
