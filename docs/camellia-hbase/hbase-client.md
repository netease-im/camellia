
# camellia-hbase 
## 简介
基于camellia-core和hbase-client开发的hbase客户端CamelliaHBaseTemplate  

## feature
* enhanced hbase client
* base on camellia-core and hbase-client，main class is CamelliaHBaseTemplate
* support dynamic-conf
* support client read-write-separate/double-write
* support aliyun-lindorm
* support obkv-hbase

## 特性
* 多读多写（如读写分离、双写等）  
* 支持配置在线修改
* 提供了一个spring-boot-starter，快速接入
* 支持阿里云lindorm
* 支持obkv-hbase

## maven依赖
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-hbase</artifactId>
  <version>a.b.c</version>
</dependency>
```

## 支持的命令
参考ICamelliaHBaseTemplate接口定义

## 客户端初始化（手动构造CamelliaHBaseTemplate对象）

### hbase

```java
String url = "hbase://nim-xx1.163.org,nim-xx2.163.org,nim-xx3.163.org/hbase1";
HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
CamelliaHBaseTemplate template = new CamelliaHBaseTemplate(hBaseResource);
```

### lindorm

先引入lindorm依赖
```
<dependency>
    <groupId>com.alibaba.hbase</groupId>
    <artifactId>alihbase-connector</artifactId>
    <version>x.x.x</version>
</dependency>
```
```java
String url = "hbase://ld-xxxx-lindorm.lindorm.rds.aliyuncs.com:30020/?userName=nim_lindorm_1&password=xxabc&lindorm=true";
HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
CamelliaHBaseTemplate template = new CamelliaHBaseTemplate(hBaseResource);
```

### obkv

先引入obkv-hbase依赖
```
<dependency>
    <groupId>com.oceanbase</groupId>
    <artifactId>obkv-hbase-client</artifactId>
    <version>2.x.x</version>
</dependency>
```
```java
String url = "hbase://obkv%http://10.1.1.1:8080/services?Action=ObRootServiceInfo&ObRegion=nimtestob&database=obkv%obkvFullUserName=kvtest@testkv#nimtestob&obkvPassword=abcdef&obkvSysUserName=root&obkvSysPassword=111234abc";
HBaseResource hBaseResource = HBaseResourceUtil.parseResourceByUrl(new Resource(url));
CamelliaHBaseTemplate template = new CamelliaHBaseTemplate(hBaseResource);
```

## 客户端初始化（使用spring-boot自动注入）

具体见：[spring-boot](springboot.md)

## 客户端使用
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
