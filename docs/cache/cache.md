
## camellia-cache
基于spring-cache二次开发： 
* 支持redis，也支持本地缓存（Caffeine）
* 支持基于注解执行mget，mevict等批量操作
* 支持不同的过期时间、支持设置是否缓存null值
* 支持自定义的序列化/反序列化，默认使用jackson，并且支持缓存值的压缩
* 支持一键刷新缓存（动态调整缓存key的前缀）

## 快速开始（spring-boot工程）
### 引入依赖
* 使用jedis-2.9.3
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-cache-spring-boot-starter</artifactId>
    <version>1.2.8</version>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-spring-boot-starter</artifactId>
    <version>1.2.8</version>
</dependency>
```
* 使用jedis-3.6.3
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-cache-spring-boot-starter</artifactId>
    <version>1.2.8</version>
    <exclusions>
        <exclusion>
            <groupId>com.netease.nim</groupId>
            <artifactId>camellia-redis</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis3-spring-boot-starter</artifactId>
    <version>1.2.8</version>
</dependency>
```

### 启用
```java
@SpringBootApplication
@EnableCamelliaCaching
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 配置
```yaml
camellia-cache:
  multi-op-batch-size: 500 #批量操作时，分批的大小，比如800的批量，会拆成500+300两次请求，默认500
  sync-load-expire-millis: 1000 #使用sync=true模式时，分布式锁的超时时间，默认1000ms
  sync-load-max-retry: 1 #使用sync=true模式时，等待线程尝试获取缓存的次数，默认1次
  sync-load-sleep-millis: 100 #使用sync=true模式时，等待线程在每次尝试获取缓存的sleep间隔，默认100ms
  compress-enable: false #是否开启缓存value的压缩，默认false
  compress-threshold: 1024 #缓存value压缩的阈值，超过阈值才会压缩，默认1024个字节
  max-cache-calue: 2097152 #缓存value的最大值，超过阈值，则不会写入缓存（仅针对中心化缓存，如redis），默认2M
  local:
    initial-capacity: 10000 #本地缓存的初始大小
    max-capacity: 100000 #本地缓存的最大大小

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

### 示例代码
#### @Cacheable
* 查询请求，如果缓存有则走缓存，如果缓存没有，则查数据库，并回填缓存
```java
//不同的cacheNames代表不同的缓存策略，REMOTE_DAY_1_CACHE_NULL表示中心化缓存（redis），ttl为1天，并且要缓存null值
@Service
@CacheConfig(cacheNames = {CamelliaCacheName.REMOTE_DAY_1_CACHE_NULL}) 
public class UserDao {
    @Cacheable(key = "'user_'.concat(#appid).concat('_').concat(#accid)")
    public User get(long appid, String accid) {
        // select from db    
    }
}
```
#### @Cacheable with sync=true
* sync=true表示缓存穿透的情况下，只允许一个请求去底层捞数据
* 其他的会等待获取到锁的请求操作完成缓存回填完成后，直接从缓存里取；若等待一段时间之后缓存中依然没有数据，此时会穿透到底层去捞数据
```java
@Service
@CacheConfig(cacheNames = {CamelliaCacheName.REMOTE_DAY_1_CACHE_NULL}) 
public class UserDao {

    @Cacheable(key = "'user_'.concat(#appid).concat('_').concat(#accid)", sync = true)
    public User get(long appid, String accid) {
        // select from db    
    }
}
```
#### @CacheEvict
* 删除缓存
```java
@Service
@CacheConfig(cacheNames = {CamelliaCacheName.REMOTE_DAY_1_CACHE_NULL}) 
public class UserDao {
    @CacheEvict(key = "'user_'.concat(#appid).concat('_').concat(#accid)")
    public int delete(long appid, String accid) {
        //delete from db
        return ret;
    }
}
```
#### @Cacheable with mget
* 批量查询一批数据，其中可能有一部分在缓存中有，一部分没有
* 缓存没有命中的部分，会穿透到db去查询，并且和缓存命中的部分合并之后返回
* 缓存没有命中的部分在查询db后会回填到缓存中
* 语法（在spring-spl表达式基础上添加如下定义）：
* 前缀是mget，以|分隔为4部分
* 第一部分固定为mget
* 第二部分表示第几个参数是List类型的（用于表达批量语义），从0开始算
* 第三部分表示缓存key的拼接方法，()用于分割字符串segment（底层会去掉）
* --其中#.*表示这一个segment为List类型参数的每一个元素的toString结果
* --其中#.abc表示这一个segment为List类型参数的每一个元素的abc这个字段的toString结果
* 第四部分表示缓存回填时的key的拼接方法，()用于分割字符串segment（底层会去掉）
* --方法回包要求必须是List
* --#.appid表示方法回包（List类型）中每一个元素的appid字段
* --#.*表示方法回包（List类型）中每一个元素的toString结果
```java
@Service
@CacheConfig(cacheNames = {CamelliaCacheName.REMOTE_DAY_1_CACHE_NULL}) 
public class UserDao {
    @Cacheable(key = "'mget|1|(user_'.concat(#appid).concat('_)(#.*)|(user_)(#.appid)(_)(#.accid)')")
    public List<User> getBatch(long appid, List<String> accidList) {
        //select from db batch
        return new ArrayList<>();
    }
}
```

### @CacheEvict with mevict
* 批量更新/删除一批数据，并且同时删除对应的缓存
* 语法（在spring-spl表达式基础上添加如下定义）：
* 前缀是mevict，以|分隔为3部分
* 第一部分固定为mevict
* 第二部分表示第几个参数是List类型的（用于表达批量语义），从0开始算
* 第三部分表示要删除的缓存的key的拼接方法，()用于分割字符串segment（底层会去掉）
* --其中#.*表示这一个segment为List类型参数的每一个元素的toString结果
* --其中#.abc表示这一个segment为List类型参数的每一个元素的abc这个字段的toString结果
```java
@Service
@CacheConfig(cacheNames = {CamelliaCacheName.REMOTE_DAY_1_CACHE_NULL}) 
public class UserDao {
    @CacheEvict(key = "'mevict|1|(user_)('.concat(#appid).concat('_)(#.*)')")
    public int deleteBatch(long appid, List<String> accidList) {
        //delete from db batch
        return ret;
    }
}
```

### 如何动态设置缓存前缀
* 你可以在工程中实现一个@Service，并且实现CamelliaCachePrefixGetter接口，则camellia-cache会使用他来获取到缓存的前缀
* 缺省情况下，或者CamelliaCachePrefixGetter返回null，则缓存的key没有前缀
示例：  
```java
@Service
public class CustomCachePrefixGetter implements CamelliaCachePrefixGetter {
    @Override
    public String get() {
        return "v1";
    }
}
```

### 支持哪些CamelliaCacheName
```java
public class CamelliaCacheName {

    /**
     * 分布式缓存，如Redis、Memcache等
     */
    //##不缓存Null
    public static final String REMOTE_MINUTE_1 = "REMOTE_MINUTE_1";
    public static final String REMOTE_MINUTE_10 = "REMOTE_MINUTE_10";
    public static final String REMOTE_MINUTE_30 = "REMOTE_MINUTE_30";
    public static final String REMOTE_HOUR_1 = "REMOTE_HOUR_1";
    public static final String REMOTE_HOUR_4 = "REMOTE_HOUR_4";
    public static final String REMOTE_HOUR_12 = "REMOTE_HOUR_12";
    public static final String REMOTE_DAY_1 = "REMOTE_DAY_1";
    public static final String REMOTE_DAY_3 = "REMOTE_DAY_3";
    public static final String REMOTE_DAY_7 = "REMOTE_DAY_7";
    public static final String REMOTE_DAY_30 = "REMOTE_DAY_30";
    public static final String REMOTE_DAY_365 = "REMOTE_DAY_365";
    public static final String REMOTE_FOREVER = "REMOTE_FOREVER";
    //##缓存Null
    public static final String REMOTE_MINUTE_1_CACHE_NULL = "REMOTE_MINUTE_1_CACHE_NULL";
    public static final String REMOTE_MINUTE_10_CACHE_NULL = "REMOTE_MINUTE_10_CACHE_NULL";
    public static final String REMOTE_MINUTE_30_CACHE_NULL = "REMOTE_MINUTE_30_CACHE_NULL";
    public static final String REMOTE_HOUR_1_CACHE_NULL = "REMOTE_HOUR_1_CACHE_NULL";
    public static final String REMOTE_HOUR_4_CACHE_NULL = "REMOTE_HOUR_4_CACHE_NULL";
    public static final String REMOTE_HOUR_12_CACHE_NULL = "REMOTE_HOUR_12_CACHE_NULL";
    public static final String REMOTE_DAY_1_CACHE_NULL = "REMOTE_DAY_1_CACHE_NULL";
    public static final String REMOTE_DAY_3_CACHE_NULL = "REMOTE_DAY_3_CACHE_NULL";
    public static final String REMOTE_DAY_7_CACHE_NULL = "REMOTE_DAY_7_CACHE_NULL";
    public static final String REMOTE_DAY_30_CACHE_NULL = "REMOTE_DAY_30_CACHE_NULL";
    public static final String REMOTE_DAY_365_CACHE_NULL = "REMOTE_DAY_365_CACHE_NULL";
    public static final String REMOTE_FOREVER_CACHE_NULL = "REMOTE_FOREVER_CACHE_NULL";

    /**
     * 本地缓存，如LRU_Local_Cache，典型的如Google ConcurrentLinkedHashMap
     * 所谓的安全和不安全指的是缓存中获取的对象在外部程序中被改变的情况下，缓存中的对象是否改变
     */
    //##不缓存Null，不安全的
    public static final String LOCAL_MILLIS_10 = "LOCAL_MILLIS_10";
    public static final String LOCAL_MILLIS_100 = "LOCAL_MILLIS_100";
    public static final String LOCAL_MILLIS_500 = "LOCAL_MILLIS_500";
    public static final String LOCAL_SECOND_1 = "LOCAL_SECOND_1";
    public static final String LOCAL_SECOND_5 = "LOCAL_SECOND_5";
    public static final String LOCAL_SECOND_10 = "LOCAL_SECOND_10";
    public static final String LOCAL_SECOND_30 = "LOCAL_SECOND_30";
    public static final String LOCAL_MINUTE_1 = "LOCAL_MINUTE_1";
    public static final String LOCAL_MINUTE_5 = "LOCAL_MINUTE_5";
    public static final String LOCAL_MINUTE_10 = "LOCAL_MINUTE_10";
    public static final String LOCAL_MINUTE_30 = "LOCAL_MINUTE_30";
    public static final String LOCAL_HOUR_1 = "LOCAL_HOUR_1";
    public static final String LOCAL_HOUR_24 = "LOCAL_DAY_1";
    public static final String LOCAL_FOREVER = "LOCAL_FOREVER";
    //##缓存Null，不安全的
    public static final String LOCAL_MILLIS_10_CACHE_NULL = "LOCAL_MILLIS_10_CACHE_NULL";
    public static final String LOCAL_MILLIS_100_CACHE_NULL = "LOCAL_MILLIS_100_CACHE_NULL";
    public static final String LOCAL_MILLIS_500_CACHE_NULL = "LOCAL_MILLIS_500_CACHE_NULL";
    public static final String LOCAL_SECOND_1_CACHE_NULL = "LOCAL_SECOND_1_CACHE_NULL";
    public static final String LOCAL_SECOND_5_CACHE_NULL = "LOCAL_SECOND_5_CACHE_NULL";
    public static final String LOCAL_SECOND_10_CACHE_NULL = "LOCAL_SECOND_10_CACHE_NULL";
    public static final String LOCAL_SECOND_30_CACHE_NULL = "LOCAL_SECOND_30_CACHE_NULL";
    public static final String LOCAL_MINUTE_1_CACHE_NULL = "LOCAL_MINUTE_1_CACHE_NULL";
    public static final String LOCAL_MINUTE_5_CACHE_NULL = "LOCAL_MINUTE_5_CACHE_NULL";
    public static final String LOCAL_MINUTE_10_CACHE_NULL = "LOCAL_MINUTE_10_CACHE_NULL";
    public static final String LOCAL_MINUTE_30_CACHE_NULL = "LOCAL_MINUTE_30_CACHE_NULL";
    public static final String LOCAL_HOUR_1_CACHE_NULL = "LOCAL_HOUR_1_CACHE_NULL";
    public static final String LOCAL_HOUR_24_CACHE_NULL = "LOCAL_DAY_1_CACHE_NULL";
    public static final String LOCAL_FOREVER_CACHE_NULL = "LOCAL_FOREVER_CACHE_NULL";
    //##不缓存Null，安全的
    public static final String SAFE_LOCAL_MILLIS_10 = "SAFE_LOCAL_MILLIS_10";
    public static final String SAFE_LOCAL_MILLIS_100 = "SAFE_LOCAL_MILLIS_100";
    public static final String SAFE_LOCAL_MILLIS_500 = "SAFE_LOCAL_MILLIS_500";
    public static final String SAFE_LOCAL_SECOND_1 = "SAFE_LOCAL_SECOND_1";
    public static final String SAFE_LOCAL_SECOND_5 = "SAFE_LOCAL_SECOND_5";
    public static final String SAFE_LOCAL_SECOND_10 = "SAFE_LOCAL_SECOND_10";
    public static final String SAFE_LOCAL_SECOND_30 = "SAFE_LOCAL_SECOND_30";
    public static final String SAFE_LOCAL_MINUTE_1 = "SAFE_LOCAL_MINUTE_1";
    public static final String SAFE_LOCAL_MINUTE_5 = "SAFE_LOCAL_MINUTE_5";
    public static final String SAFE_LOCAL_MINUTE_10 = "SAFE_LOCAL_MINUTE_10";
    public static final String SAFE_LOCAL_MINUTE_30 = "SAFE_LOCAL_MINUTE_30";
    public static final String SAFE_LOCAL_HOUR_1 = "SAFE_LOCAL_HOUR_1";
    public static final String SAFE_LOCAL_HOUR_24 = "SAFE_LOCAL_DAY_1";
    public static final String SAFE_LOCAL_FOREVER = "SAFE_LOCAL_FOREVER";
    //##缓存Null，安全的
    public static final String SAFE_LOCAL_MILLIS_10_CACHE_NULL = "SAFE_LOCAL_MILLIS_10_CACHE_NULL";
    public static final String SAFE_LOCAL_MILLIS_100_CACHE_NULL = "SAFE_LOCAL_MILLIS_100_CACHE_NULL";
    public static final String SAFE_LOCAL_MILLIS_500_CACHE_NULL = "SAFE_LOCAL_MILLIS_500_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_1_CACHE_NULL = "SAFE_LOCAL_SECOND_1_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_5_CACHE_NULL = "SAFE_LOCAL_SECOND_5_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_10_CACHE_NULL = "SAFE_LOCAL_SECOND_10_CACHE_NULL";
    public static final String SAFE_LOCAL_SECOND_30_CACHE_NULL = "SAFE_LOCAL_SECOND_30_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_1_CACHE_NULL = "SAFE_LOCAL_MINUTE_1_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_5_CACHE_NULL = "SAFE_LOCAL_MINUTE_5_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_10_CACHE_NULL = "SAFE_LOCAL_MINUTE_10_CACHE_NULL";
    public static final String SAFE_LOCAL_MINUTE_30_CACHE_NULL = "SAFE_LOCAL_MINUTE_30_CACHE_NULL";
    public static final String SAFE_LOCAL_HOUR_1_CACHE_NULL = "SAFE_LOCAL_HOUR_1_CACHE_NULL";
    public static final String SAFE_LOCAL_HOUR_24_CACHE_NULL = "SAFE_LOCAL_DAY_1_CACHE_NULL";
    public static final String SAFE_LOCAL_FOREVER_CACHE_NULL = "SAFE_LOCAL_FOREVER_CACHE_NULL";
}

```

### 其他
* 支持对缓存value进行压缩，可以中途开启压缩，也可以中途关闭压缩，会自动检测并兼容
* 会对缓存value的大小做限制，默认超过2M时不写入缓存

### 自定义
* 可以自定义序列化/反序列化，实现CamelliaCacheSerializer接口即可
* 可以自定义REMOTE缓存，默认是redis，实现RemoteNativeCacheInitializer接口即可
* 可以自定义LOCAL缓存，默认是caffeine，实现LocalNativeCacheInitializer接口即可