

## 严格递增的id生成算法（基于数据库+redis）
### 特性
* id全局严格递增
* 支持根据tag维护多条序列，彼此独立
* 支持设置region标记（比特位数可以自定义），从而可以在单元化部署中保证不同单元之间id不冲突（每个单元内严格递增）
* 支持peek操作（获取当前最新id，但是不使用）
* 提供了一个spring-boot-starter，快速搭建一个基于数据库和redis的严格递增的发号器集群

### 原理
* 数据库记录每个tag当前分配到的id
* 每个发号器节点会从数据库中取一段id后塞到redis的list中（不同节点会通过分布式锁保证id不会乱序）
* 每个发号器节点先从redis中取id，如果取不到则穿透到数据库进行load
* redis中的id即将耗尽时会提前从db中load最新一批的id
* 发号器节点会统计每个批次分配完毕消耗的时间来动态调整批次大小
* 核心源码参见CamelliaStrictIdGen

### id构成（二进制，设置了regionId且设置了regionId左移的情况下）
<img src="id-gen-strict-region.png" width="70%" height="70%">

### 快速部署一（使用安装包部署独立服务）
具体见：[quick-start-package](quick-start-package-strict.md)

### 快速部署二（使用spring-boot-starter部署独立服务）
具体见：[quick-start-spring-boot](quick-start-spring-boot-starter-strict.md)

### 数据库建表语句

```sql
CREATE TABLE `camellia_id_info` (
  `tag` varchar(512) NOT NULL COMMENT 'tag',
  `id` bigint(20) DEFAULT NULL COMMENT 'id',
  `createTime` bigint(20) DEFAULT NULL COMMENT '创建时间',
  `updateTime` bigint(20) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='id生成表';
```

### api接口
#### 返回一个id（GET请求）  
http://127.0.0.1:8082/camellia/id/gen/strict/genId?tag=a  
返回示例：  
```json
{
  "code": 200,
  "data": 5071,
  "msg": "success"
}
```

#### 返回最新的id，但是不使用（GET请求）  
http://127.0.0.1:8082/camellia/id/gen/strict/peekId?tag=a    
返回示例：  
```json
{
    "code": 200,
    "data": 5023,
    "msg": "success"
}
```
#### 更新id起始值（POST请求）
```
curl -d "tag=a&id=100" http://127.0.0.1:8082/camellia/id/gen/strict/update
```

#### 解析regionId（GET请求）  
http://127.0.0.1:8082/camellia/id/gen/strict/decodeRegionId?id=11111  
返回示例：
```json
{
    "code": 200,
    "data": 10,
    "msg": "success"
}
```

### 使用java-sdk访问api接口

先引入maven依赖：  
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-sdk</artifactId>
    <version>1.3.4</version>
</dependency>
```
示例代码如下：  
```java
public class CamelliaStrictIdGenSdkTest {
    public static void main(String[] args) {
        CamelliaIdGenSdkConfig config = new CamelliaIdGenSdkConfig();
        config.setUrl("http://127.0.0.1:8082");
        config.setMaxRetry(5);//重试次数
        CamelliaStrictIdGenSdk idGenSdk = new CamelliaStrictIdGenSdk(config);
        System.out.println(idGenSdk.peekId("a"));
        System.out.println(idGenSdk.genId("a"));

        long target = 10*10000;
        int i = 0;
        long start = System.currentTimeMillis();
        while (true) {
            idGenSdk.genId("a");
            i++;
            if (i % 1000 == 0) {
                System.out.println("i=" + i);
            }
            if (i >= target) break;
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (target / ((end - start)/1000.0)));
        //###idea里直接运行的简单测试结果：
        //QPS=3526.9636370049025
    }
}
```




