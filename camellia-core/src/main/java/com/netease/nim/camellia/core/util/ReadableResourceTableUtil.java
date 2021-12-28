package com.netease.nim.camellia.core.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.core.model.Resource;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;

import java.util.*;

/**
 *
 * Created by caojiajun on 2019/11/20.
 */
public class ReadableResourceTableUtil {

    public static ResourceTable parseTable(String string) {
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(string);
        } catch (Exception e) {
            return ResourceTableUtil.simpleTable(new Resource(string));
        }
        if (jsonObject == null) {
            throw new IllegalArgumentException();
        }
        String type = jsonObject.getString("type");
        ResourceTable resourceTable = new ResourceTable();
        if (type.equalsIgnoreCase(ResourceTable.Type.SIMPLE.name())) {
            resourceTable.setType(ResourceTable.Type.SIMPLE);
            JSONObject operation = jsonObject.getJSONObject("operation");
            ResourceOperation resourceOperation = parseResourceOperation(operation);
            ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
            simpleTable.setResourceOperation(resourceOperation);
            resourceTable.setSimpleTable(simpleTable);
            if (!CheckUtil.checkResourceTable(resourceTable)) {
                throw new IllegalArgumentException("check table fail");
            }
            return resourceTable;
        } else if (type.equalsIgnoreCase(ResourceTable.Type.SHADING.name()) || type.equalsIgnoreCase("sharding")) {//之前的错别字，这里兼容一下
            resourceTable.setType(ResourceTable.Type.SHADING);
            ResourceTable.ShadingTable shardingTable = new ResourceTable.ShadingTable();
            JSONObject operation = jsonObject.getJSONObject("operation");
            shardingTable.setBucketSize(operation.getInteger("bucketSize"));
            JSONObject operationMap = operation.getJSONObject("operationMap");
            Map<Integer, ResourceOperation> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : operationMap.entrySet()) {
                String key = entry.getKey();
                ResourceOperation resourceOperation = parseResourceOperation(entry.getValue());
                String[] split = key.split("-");
                for (String str : split) {
                    int i = Integer.parseInt(str);
                    map.put(i, resourceOperation);
                }
            }
            shardingTable.setResourceOperationMap(map);
            resourceTable.setShadingTable(shardingTable);
            if (!CheckUtil.checkResourceTable(resourceTable)) {
                throw new IllegalArgumentException("check table fail");
            }
            return resourceTable;
        }
        throw new IllegalArgumentException();
    }

    public static String readableResourceTable(ResourceTable resourceTable) {
        if (resourceTable.getType() == ResourceTable.Type.SIMPLE) {
            ResourceTable.SimpleTable simpleTable = resourceTable.getSimpleTable();
            ResourceOperation resourceOperation = simpleTable.getResourceOperation();
            if (resourceOperation.getType() == ResourceOperation.Type.SIMPLE) {
                return resourceOperation.getResource().getUrl();
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", resourceTable.getType().name().toLowerCase());
            jsonObject.put("operation", readableResourceOperation(resourceOperation));
            return jsonObject.toJSONString();
        } else if (resourceTable.getType() == ResourceTable.Type.SHADING) {
            ResourceTable.ShadingTable shardingTable = resourceTable.getShadingTable();
            JSONObject shardingJson = new JSONObject();
            shardingJson.put("bucketSize", shardingTable.getBucketSize());
            Map<ResourceOperation, List<Integer>> map = new HashMap<>();
            for (Map.Entry<Integer, ResourceOperation> entry : shardingTable.getResourceOperationMap().entrySet()) {
                List<Integer> list = map.get(entry.getValue());
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(entry.getKey());
                map.put(entry.getValue(), list);
            }
            JSONObject operationMap = new JSONObject();
            for (Map.Entry<ResourceOperation, List<Integer>> entry : map.entrySet()) {
                List<Integer> list = entry.getValue();
                Collections.sort(list);
                StringBuilder builder = new StringBuilder();
                for (Integer integer : list) {
                    builder.append(integer).append("-");
                }
                builder.deleteCharAt(builder.length() - 1);
                operationMap.put(builder.toString(), readableResourceOperation(entry.getKey()));
            }
            shardingJson.put("operationMap", operationMap);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "sharding");
            jsonObject.put("operation", shardingJson);
            return jsonObject.toJSONString();
        }
        throw new IllegalArgumentException();
    }

    public static ResourceOperation parseResourceOperation(Object object) {
        if (object instanceof String) {
            ResourceOperation resourceOperation = new ResourceOperation();
            resourceOperation.setType(ResourceOperation.Type.SIMPLE);
            resourceOperation.setResource(new Resource(object.toString()));
            return resourceOperation;
        } else if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            ResourceOperation resourceOperation = new ResourceOperation();
            String type = jsonObject.getString("type");
            if (type.equalsIgnoreCase(ResourceOperation.Type.SIMPLE.name())) {
                resourceOperation.setType(ResourceOperation.Type.SIMPLE);
                String resource = jsonObject.getString("resource");
                resourceOperation.setResource(new Resource(resource));
            } else if (type.equalsIgnoreCase(ResourceOperation.Type.RW_SEPARATE.name())) {
                resourceOperation.setType(ResourceOperation.Type.RW_SEPARATE);
                ResourceReadOperation readOperation = parseResourceReadOperation(jsonObject.get("read"));
                ResourceWriteOperation writeOperation = parseResourceWriteOperation(jsonObject.get("write"));
                resourceOperation.setReadOperation(readOperation);
                resourceOperation.setWriteOperation(writeOperation);
            } else {
                throw new IllegalArgumentException();
            }
            return resourceOperation;
        }
        throw new IllegalArgumentException();
    }

    public static Object readableResourceOperation(ResourceOperation resourceOperation) {
        if (resourceOperation.getType() == ResourceOperation.Type.SIMPLE) {
            return resourceOperation.getResource().getUrl();
        } else if (resourceOperation.getType() == ResourceOperation.Type.RW_SEPARATE) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", resourceOperation.getType().name().toLowerCase());
            jsonObject.put("read", readableResourceReadOperation(resourceOperation.getReadOperation()));
            jsonObject.put("write", readableResourceWriteOperation(resourceOperation.getWriteOperation()));
            return jsonObject;
        }
        throw new IllegalArgumentException();
    }

    public static ResourceReadOperation parseResourceReadOperation(Object object) {
        if (object instanceof String) {
            Resource resource = new Resource((String) object);
            ResourceReadOperation readOperation = new ResourceReadOperation();
            readOperation.setType(ResourceReadOperation.Type.SIMPLE);
            readOperation.setReadResource(resource);
            return readOperation;
        } else if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            ResourceReadOperation readOperation = new ResourceReadOperation();
            String type = jsonObject.getString("type");
            if (type.equalsIgnoreCase(ResourceReadOperation.Type.SIMPLE.name())) {
                readOperation.setType(ResourceReadOperation.Type.SIMPLE);
                JSONArray resources = jsonObject.getJSONArray("resources");
                if (resources.size() != 1) {
                    throw new IllegalArgumentException();
                }
                readOperation.setReadResource(new Resource(resources.get(0).toString()));
            } else if (type.equalsIgnoreCase(ResourceReadOperation.Type.ORDER.name())) {
                readOperation.setType(ResourceReadOperation.Type.ORDER);
                JSONArray resources = jsonObject.getJSONArray("resources");
                List<Resource> list = new ArrayList<>();
                for (Object resource : resources) {
                    list.add(new Resource(resource.toString()));
                }
                readOperation.setReadResources(list);
            } else if (type.equalsIgnoreCase(ResourceReadOperation.Type.RANDOM.name())) {
                readOperation.setType(ResourceReadOperation.Type.RANDOM);
                JSONArray resources = jsonObject.getJSONArray("resources");
                List<Resource> list = new ArrayList<>();
                for (Object resource : resources) {
                    list.add(new Resource(resource.toString()));
                }
                readOperation.setReadResources(list);
            } else {
                throw new IllegalArgumentException();
            }
            return readOperation;
        }
        throw new IllegalArgumentException();
    }

    public static ResourceWriteOperation parseResourceWriteOperation(Object object) {
        if (object instanceof String) {
            Resource resource = new Resource((String) object);
            ResourceWriteOperation readOperation = new ResourceWriteOperation();
            readOperation.setType(ResourceWriteOperation.Type.SIMPLE);
            readOperation.setWriteResource(resource);
            return readOperation;
        } else if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            ResourceWriteOperation writeOperation = new ResourceWriteOperation();
            String type = jsonObject.getString("type");
            if (type.equalsIgnoreCase(ResourceWriteOperation.Type.SIMPLE.name())) {
                writeOperation.setType(ResourceWriteOperation.Type.SIMPLE);
                JSONArray resources = jsonObject.getJSONArray("resources");
                if (resources.size() != 1) {
                    throw new IllegalArgumentException();
                }
                writeOperation.setWriteResource(new Resource(resources.get(0).toString()));
            } else if (type.equalsIgnoreCase(ResourceWriteOperation.Type.MULTI.name())) {
                writeOperation.setType(ResourceWriteOperation.Type.MULTI);
                JSONArray resources = jsonObject.getJSONArray("resources");
                List<Resource> list = new ArrayList<>();
                for (Object resource : resources) {
                    list.add(new Resource(resource.toString()));
                }
                writeOperation.setWriteResources(list);
            } else {
                throw new IllegalArgumentException();
            }
            return writeOperation;
        }
        throw new IllegalArgumentException();
    }

    public static Object readableResourceReadOperation(ResourceReadOperation resourceReadOperation) {
        if (resourceReadOperation.getType() == ResourceReadOperation.Type.SIMPLE) {
            Resource readResource = resourceReadOperation.getReadResource();
            return readResource.getUrl();
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", resourceReadOperation.getType().name().toLowerCase());
            JSONArray jsonArray = new JSONArray();
            List<Resource> readResources = resourceReadOperation.getReadResources();
            for (Resource resource : readResources) {
                jsonArray.add(resource.getUrl());
            }
            jsonObject.put("resources", jsonArray);
            return jsonObject;
        }
    }

    public static Object readableResourceWriteOperation(ResourceWriteOperation resourceWriteOperation) {

        if (resourceWriteOperation.getType() == ResourceWriteOperation.Type.SIMPLE) {
            Resource resource = resourceWriteOperation.getWriteResource();
            return resource.getUrl();
        } else {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", resourceWriteOperation.getType().name().toLowerCase());
            JSONArray jsonArray = new JSONArray();
            List<Resource> readResources = resourceWriteOperation.getWriteResources();
            for (Resource resource : readResources) {
                jsonArray.add(resource.getUrl());
            }
            jsonObject.put("resources", jsonArray);
            return jsonObject;
        }
    }
}
