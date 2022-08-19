package com.netease.nim.camellia.console.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.exception.AppException;
import com.netease.nim.camellia.console.model.TableDetail;
import com.netease.nim.camellia.core.model.ResourceTable;
import com.netease.nim.camellia.core.model.operation.ResourceOperation;
import com.netease.nim.camellia.core.model.operation.ResourceReadOperation;
import com.netease.nim.camellia.core.model.operation.ResourceWriteOperation;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class TableCheckUtil {
    private static final Logger logger = LoggerFactory.getLogger(TableCheckUtil.class);

    private static TableDetail.WriteSource generateConsoleWriteSource(JSONObject operation) {
        Object write = operation.get("write");
        if (write == null) {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
        }
        if (write instanceof String) {
            TableDetail.WriteSource writeSource = new TableDetail.WriteSource();
            writeSource.setResources(new String[]{(String) write});
            writeSource.setType("simple");
            return writeSource;
        } else if (write instanceof JSONObject) {
            JSONObject jsonRead = (JSONObject) write;
            String type = jsonRead.getString("type");
            if (type == null) {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
            } else if (type.equalsIgnoreCase(ResourceWriteOperation.Type.SIMPLE.name())) {
                Object readResources = jsonRead.get("resources");
                if (readResources instanceof String) {
                    TableDetail.WriteSource writeSource = new TableDetail.WriteSource();
                    writeSource.setResources(new String[]{(String) readResources});
                    writeSource.setType("simple");
                    return writeSource;
                } else {
                    throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
                }
            } else if (type.equalsIgnoreCase(ResourceWriteOperation.Type.MULTI.name())) {
                TableDetail.WriteSource writeSource = jsonRead.toJavaObject(TableDetail.WriteSource.class);
                String[] resources = writeSource.getResources();
                if (resources == null || resources.length == 0) {
                    throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
                }
                return writeSource;
            } else {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
            }
        } else {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
        }

    }

    private static TableDetail.ReadSource generateConsoleReadSource(JSONObject operation) {
        Object read = operation.get("read");
        if (read == null) {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
        }
        if (read instanceof String) {
            TableDetail.ReadSource readSource = new TableDetail.ReadSource();
            readSource.setResources(new String[]{(String) read});
            readSource.setType("simple");
            return readSource;
        } else if (read instanceof JSONObject) {
            JSONObject jsonRead = (JSONObject) read;
            String type = jsonRead.getString("type");
            if (type == null) {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
            } else if (type.equalsIgnoreCase(ResourceReadOperation.Type.SIMPLE.name())) {
                Object readResources = jsonRead.get("resources");
                if (readResources instanceof String) {
                    TableDetail.ReadSource readSource = new TableDetail.ReadSource();
                    readSource.setResources(new String[]{(String) readResources});
                    readSource.setType("simple");
                    return readSource;
                } else {
                    throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
                }
            } else if (type.equalsIgnoreCase(ResourceReadOperation.Type.ORDER.name()) || type.equalsIgnoreCase(ResourceReadOperation.Type.RANDOM.name())) {
                TableDetail.ReadSource readSource = jsonRead.toJavaObject(TableDetail.ReadSource.class);
                String[] resources = readSource.getResources();
                if (resources == null || resources.length == 0) {
                    throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
                }
                return readSource;
            } else {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
            }
        } else {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "解析配置失败");
        }

    }


    public static String parseConsoleTableToDashboard(TableDetail detail) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (detail == null) {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
            }
            String type = detail.getType();
            if (type.equalsIgnoreCase(ResourceTable.Type.SIMPLE.name())) {
                jsonObject.put("type", "simple");
                Object operation = detail.getOperation();
                TableDetail.SimpleTable simpleTable = JSONObject.parseObject(JSON.toJSONString(operation), TableDetail.SimpleTable.class);
                if (simpleTable.getType().equalsIgnoreCase(ResourceOperation.Type.SIMPLE.name())) {
                    if (StringUtil.isNullOrEmpty(simpleTable.getResource())) {
                        throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
                    } else {
                        return simpleTable.getResource();
                    }
                } else if (simpleTable.getType().equalsIgnoreCase(ResourceOperation.Type.RW_SEPARATE.name())) {
                    JSONObject jsonOperation = new JSONObject();
                    jsonOperation.put("type", "rw_separate");
                    jsonOperation.put("read", generateDashboardReadSource(simpleTable));
                    jsonOperation.put("write", generateDashboardWriteSource(simpleTable));
                    jsonObject.put("operation", jsonOperation);
                } else {
                    throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
                }
            } else if (type.equalsIgnoreCase(ResourceTable.Type.SHADING.name())||type.equalsIgnoreCase("sharding")) {
                jsonObject.put("type", "sharding");
                TableDetail.ShardingTableConsole shardingTableConsole = JSONObject.parseObject(JSON.toJSONString(detail.getOperation()), TableDetail.ShardingTableConsole.class);
                if (shardingTableConsole.getBucketSize() == null || shardingTableConsole.getBucketSize() <= 0) {
                    throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
                }
                TableDetail.ShardingSimpleTable[] operationMap = shardingTableConsole.getOperationMap();
                Map<String, Object> map = new HashMap<>();
                for (TableDetail.ShardingSimpleTable shardingSimpleTable : operationMap) {
                    if (shardingSimpleTable.getType().equalsIgnoreCase(ResourceOperation.Type.SIMPLE.name())) {
                        map.put(shardingSimpleTable.getKey(), shardingSimpleTable.getResource());
                    } else if (shardingSimpleTable.getType().equalsIgnoreCase(ResourceOperation.Type.RW_SEPARATE.name())) {
                        JSONObject jsonOperation = new JSONObject();
                        jsonOperation.put("type", "rw_separate");
                        jsonOperation.put("read", generateDashboardReadSource(shardingSimpleTable));
                        jsonOperation.put("write", generateDashboardWriteSource(shardingSimpleTable));
                        map.put(shardingSimpleTable.getKey(), jsonOperation);
                    } else {
                        throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
                    }
                }
                JSONObject operation = new JSONObject();
                operation.put("bucketSize", shardingTableConsole.getBucketSize());
                operation.put("operationMap", map);
                jsonObject.put("operation", operation);
            }else {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
            }
            return jsonObject.toJSONString();
        }catch (Exception e){
            logger.error("配置解析错误", e);
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
        }
    }

    private static Object generateDashboardWriteSource(TableDetail.SimpleTable simpleTable) {
        TableDetail.WriteSource write = simpleTable.getWrite();
        if (write.getResources() == null || write.getResources().length == 0) {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
        }
        if (write.getType().equalsIgnoreCase(ResourceWriteOperation.Type.SIMPLE.name())) {
            if (write.getResources().length > 1) {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
            } else {
                return write.getResources()[0];
            }
        } else if (write.getType().equalsIgnoreCase(ResourceWriteOperation.Type.MULTI.name())) {
            return write;
        } else {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
        }
    }

    private static Object generateDashboardReadSource(TableDetail.SimpleTable simpleTable) {
        TableDetail.ReadSource read = simpleTable.getRead();
        if (read.getResources() == null || read.getResources().length == 0) {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
        }
        if (read.getType().equalsIgnoreCase(ResourceReadOperation.Type.SIMPLE.name())) {
            if (read.getResources().length > 1) {
                throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
            } else {
                return read.getResources()[0];
            }
        } else if (read.getType().equalsIgnoreCase(ResourceReadOperation.Type.RANDOM.name()) || read.getType().equalsIgnoreCase(ResourceReadOperation.Type.ORDER.name())) {
            return read;
        } else {
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
        }

    }

    public static TableDetail parseDashboardTableToConsole(String table) {
        TableDetail tableDetail = new TableDetail();
        try {
            try {
                tableDetail = JSONObject.parseObject(table, TableDetail.class);
            } catch (Exception e) {
                TableDetail.SimpleTable simpleTable = new TableDetail.SimpleTable();

                simpleTable.setResource(table);
                simpleTable.setType("simple");
                tableDetail.setType("simple");
                tableDetail.setOperation(simpleTable);
                return tableDetail;
            }
            if (tableDetail.getType().equalsIgnoreCase(ResourceTable.Type.SIMPLE.name())) {
                TableDetail.SimpleTable simpleTable = new TableDetail.SimpleTable();
                JSONObject operation = (JSONObject) tableDetail.getOperation();
                if (operation.getString("type").equalsIgnoreCase(ResourceOperation.Type.RW_SEPARATE.name())) {
                    simpleTable.setType("rw_separate");
                    TableDetail.ReadSource readSource = generateConsoleReadSource(operation);
                    simpleTable.setRead(readSource);
                    TableDetail.WriteSource writeSource = generateConsoleWriteSource(operation);
                    simpleTable.setWrite(writeSource);
                } else if (operation.getString("type").equalsIgnoreCase(ResourceOperation.Type.SIMPLE.name())) {
                    String resource = operation.getString("resource");
                    simpleTable.setType("simple");
                    simpleTable.setResource(resource);
                }
                tableDetail.setOperation(simpleTable);
            } else if (tableDetail.getType().equalsIgnoreCase("sharding") || tableDetail.getType().equalsIgnoreCase(ResourceTable.Type.SHADING.name())) {
                JSONObject operation = (JSONObject) tableDetail.getOperation();
                List<TableDetail.ShardingSimpleTable> shardingTableConsoleList = new ArrayList<>();
                Map operationMap = operation.getObject("operationMap", Map.class);
                Set<String> strings = operationMap.keySet();
                for (String key : strings) {
                    TableDetail.ShardingSimpleTable shardingSimpleTable = new TableDetail.ShardingSimpleTable();
                    shardingSimpleTable.setKey(key);

                    Object o = operationMap.get(key);
                    if (o instanceof String) {
                        shardingSimpleTable.setType("simple");
                        shardingSimpleTable.setResource((String) o);
                    } else if (o instanceof JSONObject) {
                        JSONObject jsonObject = (JSONObject) o;
                        String type = jsonObject.getString("type");
                        if (type == null) {
                            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
                        } else if (type.equalsIgnoreCase(ResourceOperation.Type.SIMPLE.name())) {
                            String resource = jsonObject.getString("resource");
                            shardingSimpleTable.setType("simple");
                            shardingSimpleTable.setResource(resource);
                        } else if (type.equalsIgnoreCase(ResourceOperation.Type.RW_SEPARATE.name())) {
                            shardingSimpleTable.setType("rw_separate");
                            TableDetail.ReadSource readSource = generateConsoleReadSource(jsonObject);
                            shardingSimpleTable.setRead(readSource);
                            TableDetail.WriteSource writeSource = generateConsoleWriteSource(jsonObject);
                            shardingSimpleTable.setWrite(writeSource);
                        }
                    }
                    shardingTableConsoleList.add(shardingSimpleTable);
                }
                TableDetail.ShardingTableConsole shardingTableConsole = new TableDetail.ShardingTableConsole();
                shardingTableConsole.setBucketSize(operation.getInteger("bucketSize"));
                shardingTableConsole.setOperationMap(shardingTableConsoleList.toArray(new TableDetail.ShardingSimpleTable[0]));
                tableDetail.setOperation(shardingTableConsole);
            }
            return tableDetail;
        } catch (Exception e) {
            logger.error("配置解析错误", e);
            throw new AppException(AppCode.CONFIG_PARSE_WRONG, "配置解析错误");
        }
    }

    public static void main(String[] args) {
        JSONObject jsonObject = JSONObject.parseObject("asdsad");
        System.out.println(jsonObject);
    }

    public void checkUrl(String url) {

    }
}
