
### 引入依赖

```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-hbase-spring-boot-starter</artifactId>
  <version>a.b.c</version>
</dependency>
```

如果是lindorm或者obkv，则引入关联依赖

### 使用本地配置（基于xml）
```
camellia-hbase:
  type: local
  local:
    conf-type: xml
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