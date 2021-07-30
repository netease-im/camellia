
# camellia-redis 
## 简介
基于camellia-core和jedis（2.9.3）开发的Redis客户端CamelliaRedisTemplate  

## 特性
* 支持redis、redis-sentinel、redis-cluster，对外暴露统一的api（方法和参数同普通jedis）
* 支持pipeline（原生JedisCluster不支持）
* 支持mget/mset等multiKey的命令（原生JedisCluster不支持）    
* 支持配置客户端分片，从而可以多个redis/redis-sentinel/redis-cluster当做一个使用
* 支持配置多读多写（如：双写/读写分离）
* 支持读redis-sentinel的从节点，并自动感知主从切换、从节点扩容、从节点宕机等
* 支持Jedis适配器，修改一行代码从Jedis迁移到CamelliaRedisTemplate
* 支持配置动态变更
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
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis</artifactId>
  <version>a.b.c</version>
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
### 更详细的一个例子
如何设置超时时间、连接池、ResourceTable等相关参数，参见：[sample](sample.md)

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

### 使用Jedis适配器接入
如果你本来使用的是原生的Jedis，则使用适配器可以修改一行代码即可切换成CamelliaRedisTemplate，参见：[adaptor](adaptor.md)  

### 适配SpringRedisTemplate
如果你本来使用的是SpringRedisTemplate，但是也想拥有CamelliaRedisTemplate的分片、读写分离、双写等能力，参见：[spring-adaptor](spring-adaptor.md)

### 示例源码
[示例源码](/camellia-samples/camellia-redis-samples)