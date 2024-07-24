
# camellia-core
## 简介  
基于cglib开发的支持**客户端多读多写**和**客户端分片**的代理框架  
通过在方法上添加注解的方式标识方法的读写类型  
通过方法参数上添加注解的方式标识分片字段，分片算法支持自定义  
可以和camellia-dashboard搭配使用，从而可以动态更新代理配置  
## maven依赖
```
<dependency>
  <groupId>com.netease.nim.camellia</groupId>
  <artifactId>camellia-core</artifactId>
  <version>a.b.c</version>
</dependency>
```
## 示例

### 业务代码
```
public class Cache {

    private Map<String, String> map = new HashMap<>();
    private Resource resource;

    public Cache(Resource resource) {
        this.resource = resource;
    }

    @ReadOp
    public String get(@Param String key) {
        System.out.println("get, resource = " + resource.getUrl() + ", key = " + key);
        return map.get(key);
    }

    @WriteOp
    public int set(@ShardingParam String key, String value) {
        System.out.println("set, resource = " + resource.getUrl() + ", key = " + key);
        map.put(key, value);
        return 1;
    }

    @WriteOp
    public int delete(@ShardingParam String key) {
        System.out.println("delete, resource = " + resource.getUrl() + ", key = " + key);
        map.remove(key);
        return 1;
    }

    @ReadOp
    public Map<String, String> getBulk(@ShardingParam(type = ShardingParam.Type.Collection) String... keys) {
        System.out.println("getBulk, resource = " + resource.getUrl() + ", keys = " + keys);
        Map<String, String> ret = new HashMap<>();
        for (String key : keys) {
            String value = map.get(key);
            if (value != null) {
                ret.put(key, value);
            }
        }
        return ret;
    }

    @WriteOp
    public int setBulk(@ShardingParam(type = ShardingParam.Type.Collection) Map<String, String> kvs) {
        System.out.println("setBulk, resource = " + resource.getUrl() + ", keys = " + JSONObject.toJSONString(kvs.keySet()));
        for (Map.Entry<String, String> entry : kvs.entrySet()) {
            this.map.put(entry.getKey(), entry.getValue());
        }
        return kvs.size();
    }
}

```
### 使用本地静态配置
```

Resource rw = new Resource("rw");
Resource w = new Resource("w");
ResourceTable resourceTable = ResourceTableUtil.simple2W1RTable(rw, rw, w);
StandardProxyGenerator<Cache> generator = new StandardProxyGenerator<>(Cache.class, resourceTable);

Cache proxy = generator.generate();

System.out.println(proxy.get("k1"));
System.out.println(proxy.set("k1", "v1"));
System.out.println(proxy.getBulk("k1", "k2"));
Map<String, String> kvs = new HashMap<>();
kvs.put("k2", "v2");
kvs.put("k3", "v3");
System.out.println(proxy.setBulk(kvs));

```
### 使用dashboard动态配置
```
ReloadableProxyFactory<Cache> factory = new ReloadableProxyFactory.Builder<Cache>()
                .service(CamelliaApiUtil.init("http://127.0.0.1:8080"))//dashboard的地址
                .bid(1L)//业务类型
                .bgroup("default")//业务分组
                .clazz(Cache.class)//代理对象
                .monitorEnable(true)//是否上报统计信息
                .checkIntervalMillis(5000)//配置检查间隔，单位ms
                .build();
                
Cache proxy = factory.getDynamicProxy();

System.out.println(proxy.get("k1"));
System.out.println(proxy.set("k1", "v1"));
System.out.println(proxy.getBulk("k1", "k2"));
Map<String, String> kvs = new HashMap<>();
kvs.put("k2", "v2");
kvs.put("k3", "v3");
System.out.println(proxy.setBulk(kvs));

```

### 示例源码
[示例源码](/camellia-samples/camellia-core-samples)