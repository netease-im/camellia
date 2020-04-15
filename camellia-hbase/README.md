
# camellia-hbase 
## 简介
基于camellia-core和hbase-client开发的hbase客户端CamelliaHBaseTemplate
可以基于本地的静态配置构造客户端  
也可以基于远程dashboard的动态配置构造客户端
支持多读多写、双写等特性  
提供了一个spring boot starter  

## 使用场景
* 需要进行hbase双写迁移  

## maven依赖
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-hbase-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
```

## 支持的命令
参考ICamelliaHBaseTemplate接口定义

## 示例（详细配置参考CamelliaHBaseProperties）

### 使用本地配置（基于xml）
```
camellia-hbase:
  type: local
  local:
    xml:
      xml-file: hbase.xml
```
### 使用本地配置（基于yml）
```
camellia-hbase:
  type: local
  local:
    conf-type: yml
    yml:
      resource: hbase://xxx1.163.org,xxx2.163.org,xxx3.163.org/hbase
```
### 使用本地配置（复杂配置，单独的一个json文件）  
```
camellia-hbase:
  type: local
  local:
    conf-type: yml
    yml:
      type: complex
      json-file: resource-table.json
```
```
{
  "type": "simple",
  "operation": {
    "read": "hbase://xxx1.163.org,xxx2.163.org,xxx3.163.org/hbase",
    "type": "rw_separate",
    "write": {
      "resources": [
        "hbase://xxx1.163.org,xxx2.163.org,xxx3.163.org/hbase",
        "hbase://yyy1.163.org,yyy2.163.org,yyy3.163.org/hbase"
      ],
      "type": "multi"
    }
  }
}
```
### 使用dashboard动态配置
```
camellia-hbase:
  type: remote
  remote:
    bid: 1
    bgroup: default
    url: http://127.0.0.1:8080
```

### 客户端使用
```
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
```
### 示例源码
[示例源码](/camellia-samples/camellia-redis-samples)