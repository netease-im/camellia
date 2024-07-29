
# camellia-redis 
## 简介
基于camellia-core和jedis（2.9.3/3.6.3）开发并增强的Redis客户端CamelliaRedisTemplate  

## feature
* enhanced-redis-client
* base on camellia-core and jedis(2.9.3)，main class is CamelliaRedisTemplate, can invoke redis-standalone/redis-sentinel/redis-cluster in identical way，support pipeline
* support client sharding/read-write-separate/double-write
* support read from slave(redis-sentinel)
* provide CamelliaRedisLock、CamelliaFreq utils

## 特性
* 支持redis、redis-sentinel、redis-cluster，对外暴露统一的api（方法和参数同普通jedis）
* 支持pipeline（原生JedisCluster不支持）
* 支持mget/mset等multiKey的命令（原生JedisCluster不支持）    
* 支持配置客户端分片，从而可以多个redis/redis-sentinel/redis-cluster当做一个使用
* 支持配置多读多写（如：双写/读写分离）
* 支持透明的读redis-sentinel的从节点，并自动感知主从切换、从节点扩容、从节点宕机等
* 支持配置动态变更
* 提供了一些常用的工具类，如分布式锁、计数器缓存、频控等
* 提供了一个spring-boot-starter，快速接入

## 使用场景
* 需要从redis/redis-sentinel迁移到redis-cluster，CamelliaRedisTemplate的接口定义和Jedis一致（或者直接使用Jedis适配器），并且支持了mget/mset/pipeline等批量命令    
* 需要让数据在redis/redis-sentinel/redis-cluster之间进行迁移，可以使用CamelliaRedisTemplate的双写功能    
* 单个集群容量不够（比如redis-cluster单集群容量超过1T可能会崩溃），可以使用分片和双写，逐步迁移到N个集群进行客户端分片
* 可以使用CamelliaRedisTemplate的读写分离功能/双（多）读功能来提升整体的读写能力，特别是存在热点的场景  

## 支持的命令
参考ICamelliaRedisTemplate和ICamelliaRedisPipeline两个接口定义

## 教程

### maven
* 底层依赖jedis-2.9.3
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis</artifactId>
  <version>1.2.28</version>
</dependency>
```
* 底层依赖jedis-3.6.3
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis3</artifactId>
  <version>1.2.28</version>
</dependency>
```

### 最简单示例
```java
public class TestSamples {
    
    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate("redis://pass@127.0.0.1:6379");
        String k1 = template.get("k1");
        System.out.println(k1);
        String setex = template.setex("k1", 100, "v1");
        System.out.println(setex);
        
        try (ICamelliaRedisPipeline pipelined = template.pipelined()) {
            Response<Long> response1 = pipelined.sadd("sk1", "sv1");
            Response<Long> response2 = pipelined.zadd("zk1", 1.0, "zv1");
            pipelined.sync();
            System.out.println(response1.get());
            System.out.println(response2.get());
        }
    }
}
```
### 更详细的一个说明
如何设置超时时间、连接池、ResourceTable等相关参数，参见：[detail](detail.md)

### 生成其他复杂的ResourceTable的方法
如何配置双写、分片、读写分离等，参见：[resource-table](resource-table.md)

### 动态配置
* 整合camellia-dashboard  
使用camellia-dashboard来动态变更ResourceTable配置，参见：[dynamic-dashboard](dynamic-dashboard.md)

* 使用独立配置文件  
使用独立配置文件方式来动态变更ResourceTable配置，参见：[dynamic-conf](dynamic-conf.md)

* 自定义  
自定义动态变更ResourceTable配置的方法，参见：[dynamic-custom](dynamic-custom.md)

### 使用spring-boot-starter快速接入
使用spring-boot-starter自动注入CamelliaRedisTemplate实例，参见：[spring-boot-starter](spring-boot-starter.md)

### 几个工具类
参见：[tools](/docs/camellia-tools/tools.md)

### 示例源码
[示例源码](/camellia-samples/camellia-redis-samples)