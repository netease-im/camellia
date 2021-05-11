
# camellia-redis 
## 简介
基于camellia-core和jedis开发的Redis客户端CamelliaRedisTemplate  

## 特性
* 支持redis、redis-sentinel、redis-cluster，对外暴露统一的api（方法和参数同普通jedis）
* 支持pipeline（原生JedisCluster不支持）
* 支持mget/mset等multiKey的命令（原生JedisCluster不支持）    
* 支持配置客户端分片，从而可以多个redis/redis-sentinel/redis-cluster当做一个使用
* 支持配置多读多写（如：双写/读写分离）
* 支持读redis-sentinel的从节点，并自动感知主从切换、从节点扩容、从节点宕机等
* 支持配置在线修改
* 提供了一个spring-boot-starter，快速接入

## 使用场景
* 需要从redis/redis-sentinel迁移到redis-cluster，CamelliaRedisTemplate的接口定义和Jedis一致，并且支持了mget/mset/pipeline等批量命令    
* 需要让数据在redis/redis-sentinel/redis-cluster之间进行迁移，可以使用CamelliaRedisTemplate的双写功能    
* 单个集群容量不够（比如redis-cluster单集群容量超过1T可能会崩溃），可以使用分片和双写，逐步迁移到N个集群进行客户端分片
* 可以使用CamelliaRedisTemplate的读写分离功能/双（多）读功能来提升整体的读写能力，特别是存在热点的场景  

## 支持的命令
参考ICamelliaRedisTemplate和ICamelliaRedisPipeline两个接口定义

## 示例

### maven示例
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis</artifactId>
  <version>1.0.24</version>
</dependency>
```

### 示例代码
```java
public class TestCamelliaRedisTemplate {

    public static void test() {
        //设置连接池和超时参数
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMinIdle(0);
        jedisPoolConfig.setMaxIdle(32);
        jedisPoolConfig.setMaxTotal(32);
        jedisPoolConfig.setMaxWaitMillis(2000);
        int timeout = 2000;
        int maxAttempts = 5;
        CamelliaRedisEnv redisEnv = new CamelliaRedisEnv.Builder()
                .jedisPoolFactory(new JedisPoolFactory.DefaultJedisPoolFactory(jedisPoolConfig, timeout))
                .jedisClusterFactory(new JedisClusterFactory.DefaultJedisClusterFactory(jedisPoolConfig, timeout, timeout, maxAttempts))
                .build();

        //1、访问单点redis
        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis://passwd@127.0.0.1:6379"));
        //2、访问redis-sentinel
//        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:26379/master"));
        //3、访问redis-cluster
//        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379"));

        //传入CamelliaRedisEnv和ResourceTable，初始化CamelliaRedisTemplate对象
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, resourceTable);

        //所有方法的入参和返回均和jedis保持一致
        String value = template.get("k1");
        System.out.println(value);

        //pipeline也和jedis的pipeline类似，不同点在于，每次使用完pipeline对象，务必调用close方法；可以使用try-resource语法自动close（因为ICamelliaRedisPipeline实现了Closeable接口）
        try (ICamelliaRedisPipeline pipelined = template.pipelined();) {
            Response<String> r1 = pipelined.get("k1");
            Response<Long> r2 = pipelined.hset("hk1", "hv1", "1");
            pipelined.sync();
            System.out.println(r1.get());
            System.out.println(r2.get());
        }
    }

    public static void main(String[] args) {
        test();
    }
}
```
CamelliaRedisTemplate初始化需要两个参数：
* CamelliaRedisEnv  
描述了一些配置信息，包括连接池参数、超时、redis-cluster的重试次数等     
CamelliaRedisEnv会管理底层的redis连接，因此不同CamelliaRedisTemplate可以共用同一个CamelliaRedisEnv实例，此时相同的redis后端会共用同一组连接（即使是不同的CamelliaRedisTemplate实例）      
* ResourceTable  
表示了路由表，表示CamelliaRedisTemplate的请求指向哪个redis地址(支持的后端redis类型，参见：[resource-samples](resource-samples.md))，支持单点redis、redis-sentinel、redis-cluster，此外也支持配置分片、读写分离等      
上面的示例中表示了使用ResourceTableUtil去生成了指向单个地址的ResourceTable     

### 生成其他复杂的ResourceTable的方法
ResourceTableUtil提供几个常用的工具方法去生成不同的ResourceTable，如：单写单读、双写单读、读写分离、简单分片等，如下：  
```java
public class ResourceTableUtil {

    /**
     * 单写单读
     */
    public static ResourceTable simpleTable(Resource resource) {
        ResourceOperation resourceOperation = new ResourceOperation(resource);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 双写单读
     */
    public static ResourceTable simple2W1RTable(Resource readResource, Resource writeResource1, Resource writeResource2) {
        ResourceReadOperation readOperation = new ResourceReadOperation(readResource);
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(Arrays.asList(writeResource1, writeResource2));

        ResourceOperation resourceOperation = new ResourceOperation(readOperation, writeOperation);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 读写分离
     */
    public static ResourceTable simpleRwSeparateTable(Resource readResource, Resource writeResource) {
        ResourceReadOperation readOperation = new ResourceReadOperation(readResource);
        ResourceWriteOperation writeOperation = new ResourceWriteOperation(writeResource);

        ResourceOperation resourceOperation = new ResourceOperation(readOperation, writeOperation);

        ResourceTable.SimpleTable simpleTable = new ResourceTable.SimpleTable();
        simpleTable.setResourceOperation(resourceOperation);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SIMPLE);
        table.setSimpleTable(simpleTable);
        return table;
    }

    /**
     * 不带N读N写的分片
     */
    public static ResourceTable simpleShadingTable(Map<Integer, Resource> resourceMap, int bucketSize) {
        ResourceTable.ShadingTable shadingTable = new ResourceTable.ShadingTable();
        shadingTable.setBucketSize(bucketSize);
        Map<Integer, ResourceOperation> resourceOperationMap = new HashMap<>();
        for (int i=0; i<bucketSize; i++) {
            Resource resource = resourceMap.get(i);
            if (resource == null) {
                throw new IllegalArgumentException("resourceMap/bucketSize not match");
            }
            resourceOperationMap.put(i, new ResourceOperation(resource));
        }
        shadingTable.setResourceOperationMap(resourceOperationMap);

        ResourceTable table = new ResourceTable();
        table.setType(ResourceTable.Type.SHADING);
        table.setShadingTable(shadingTable);
        return table;
    }
}

```
更普遍的生成自定义的ResourceTable的方法是使用json去描述，然后调用ReadableResourceTableUtil的parse方法去解析，如下：  
```java
public class TestJsonResourceTable {
    public static void testJsonResourceTable() {
        String json = "{\n" +
                "  \"type\": \"shading\",\n" +
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

```
更多的json格式参见[resource-table-samples](resource-table-samples.md)

### 动态配置（整合camellia-dashboard）
* CamelliaRedisTemplate支持动态修改ResourceTable而不需要重新初始化新的CamelliaRedisTemplate实例，原理是CamelliaRedisTemplate将ResourceTable的配置托管给camellia-dashboard，CamelliaRedisTemplate会定时检查ResourceTable是否有变更     
* camellia-dashboard支持管理多组ResourceTable配置，CamelliaRedisTemplate使用bid/bgroup来指定需要使用哪组配置，如下：  
```java
public class TestCamelliaDashboard {

    public static void test() {
        String dashboardUrl = "http://127.0.0.1:8080";//dashboard地址
        long bid = 1;
        String bgroup = "default";
        boolean monitorEnable = true;//是否上报监控数据到dashboard
        long checkIntervalMillis = 5000;//检查resourceTable的间隔

        CamelliaRedisEnv redisEnv = CamelliaRedisEnv.defaultRedisEnv();
        
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, dashboardUrl, bid, bgroup, monitorEnable, checkIntervalMillis);
        String k1 = template.get("k1");
        System.out.println(k1);
    }

    public static void main(String[] args) {
        test();
    }
}
```

### 动态配置（使用独立配置文件）
如果你不想引入camellia-dashboard，但是又想CamelliaRedisTemplate可以动态变更ResourceTable，那么你可以将配置托管到某个文件，然后CamelliaRedisTemplate会定期检查文件是否有更新，如下：  
```java
public class TestJsonFile {

    public static void test() {
        String fileName = "resource-table.json";//文件可以是json，也可以是单个的redis地址
//        String fileName = "simple.conf";
        URL resource = TestJsonFile.class.getClassLoader().getResource(fileName);
        if (resource == null) {
            System.out.println(fileName + " not exists");
            return;
        }
        ReloadableLocalFileCamelliaApi localFileCamelliaApi = new ReloadableLocalFileCamelliaApi(resource.getPath());

        CamelliaRedisEnv redisEnv = CamelliaRedisEnv.defaultRedisEnv();
        long checkIntervalMillis = 5000;//检查文件是否产生变更的检查周期，单位ms
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, localFileCamelliaApi, checkIntervalMillis);

        String k1 = template.get("k1");
        System.out.println(k1);
    }

    public static void main(String[] args) {
        test();
    }
}


```
上面的例子中CamelliaRedisTemplate引用了classpath下一个叫resource-table.json的文件中的ResourceTable配置，并且当文件发生变更的时候，CamelliaRedisTemplate会在5000ms之内感知到并自动reloadResourceTable配置  
注：resource-table.json配置里可以只填一个地址（不是一个json），如：
```
redis://@127.0.0.1:6379
```
ReloadableLocalFileCamelliaApi会自动识别这种情况，此时ResourceTable配置就是一个没有分片也没有读写分离的简单配置

## 示例（使用spring-boot-starter自动注入）（详细配置参考CamelliaRedisProperties）

### maven依赖
```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-spring-boot-starter</artifactId>
  <version>1.0.23</version>
</dependency>
```

### 自动注入
使用camellia-redis-spring-boot-starter后，在application.yml里做好相关配置，则spring会自动注入一个CamelliaRedisTemplate实例，你可以直接使用@Autowired去使用，如下：
```java
@SpringBootApplication
public class Application {

    @Autowired
    private CamelliaRedisTemplate template;

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

}
```

### yml配置示例

#### 单点redis
```
camellia-redis:
  type: local
  local:
    resource: redis://@127.0.0.1:6379
  redis-conf:
    jedis:
      timeout: 2000
      min-idle: 0
      max-idle: 32
      max-active: 32
      max-wait-millis: 2000
    jedis-cluster:
      max-wait-millis: 2000
      min-idle: 0
      max-idle: 8
      max-active: 16
      max-attempts: 5
      timeout: 2000
```
### redis-cluster
```
camellia-redis:
  type: local
  local:
    resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
  redis-conf:
    jedis:
      timeout: 2000
      min-idle: 0
      max-idle: 32
      max-active: 32
      max-wait-millis: 2000
    jedis-cluster:
      max-wait-millis: 2000
      min-idle: 0
      max-idle: 8
      max-active: 16
      max-attempts: 5
      timeout: 2000
```
### 使用json自定义配置（需要单独的一个json文件）  
```
camellia-redis:
  type: local
  local:
    type: complex
    json-file: resource-table.json   #默认去classpath下寻找文件，也可以设置一个绝对路径的文件地址
    dynamic: true  #设置为true则会动态检查文件resource-table.json是否有变更，并自动reload
  redis-conf:
    jedis:
      timeout: 2000
      min-idle: 0
      max-idle: 32
      max-active: 32
      max-wait-millis: 2000
    jedis-cluster:
      max-wait-millis: 2000
      min-idle: 0
      max-idle: 8
      max-active: 16
      max-attempts: 5
      timeout: 2000
```
```
{
  "type": "shading",
  "operation": {
    "operationMap": {
      "4": {
        "read": "redis://password1@127.0.0.1:6379",
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "0-2": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381",
      "1-3-5": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```
### 使用dashboard动态配置
```
camellia-redis:
  type: remote
  remote:
    bid: 1
    bgroup: default
    url: http://127.0.0.1:8080
  redis-conf:
    jedis:
      timeout: 2000
      min-idle: 0
      max-idle: 32
      max-active: 32
      max-wait-millis: 2000
    jedis-cluster:
      max-wait-millis: 2000
      min-idle: 0
      max-idle: 8
      max-active: 16
      max-attempts: 5
      timeout: 2000
```

### 示例源码
[示例源码](/camellia-samples/camellia-redis-samples)