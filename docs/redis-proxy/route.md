## 路由配置
路由配置表示了camellia-redis-proxy在收到客户端的redis命令之后的转发规则

## 大纲
* 最简单的示例
* 支持的后端redis类型
* 动态配置
* json-file配置示例（双写、读写分离、分片等）
* 集成camellia-dashboard
* 集成ProxyRouteConfUpdater自定义管理多组动态配置
* 使用自定义ClientAuthProvider来实现通过不同的登录密码来指向不同路由配置
* 不同的双（多）写模式
* 自定义分片函数

### 最简单的示例
在application.yml里配置如下信息：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: simple
      resource: redis-cluster://@127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377
```
上面的配置表示proxy的端口=6380，proxy的密码=pass123，代理到后端redis-cluster集群，地址串=127.0.0.1:6379,127.0.0.1:6378,127.0.0.1:6377

### 支持的后端redis类型
我们通过url的方式来描述后端redis服务器，支持普通单点redis、redis-sentinel、redis-cluster三种类型，此外还支持将读命令代理到redis-sentinel的从节点，具体的url格式如下：

* 普通单点redis
```
##有密码
redis://passwd@127.0.0.1:6379
##没有密码
redis://@127.0.0.1:6379
##有账号也有密码
redis://username:passwd@127.0.0.1:6379
```

* redis-sentinel
```
##有密码
redis-sentinel://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName
##没有密码
redis-sentinel://@127.0.0.1:16379,127.0.0.1:16379/masterName
##有账号也有密码
redis-sentinel://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName
```

* redis-cluster
```
##有密码
redis-cluster://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##没有密码
redis-cluster://@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码
redis-cluster://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
```

* redis-sentinel-slaves
```
##本类型的后端只能配置为读写分离模式下的读地址

##不读master，此时proxy会从slave集合中随机挑选一个slave进行命令的转发
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false
##有账号也有密码
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=false

##读master，此时proxy会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
redis-sentinel-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##没有密码
redis-sentinel-slaves://@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true
##有账号也有密码
redis-sentinel-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379/masterName?withMaster=true

##redis-sentinel-slaves会自动感知：节点宕机、主从切换和节点扩容
```

* redis-cluster-slaves
```
##本类型的后端只能配置为读写分离模式下的读地址，如果配置为写地址，proxy不会报错，但是每次写请求都会产生一次重定向，性能会大大受影响

##不读master，此时proxy会从slave集合中随机挑选一个slave进行命令的转发
##有密码
redis-cluster-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=false
##没有密码
redis-cluster-slaves://@127.0.0.1:16379,127.0.0.1:16379?withMaster=false
##有账号也有密码
redis-cluster-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=false

##读master，此时proxy会从master+slave集合中随机挑选一个节点进行命令的转发（可能是master也可能是slave，所有节点概率相同）
##有密码
redis-cluster-slaves://passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=true
##没有密码
redis-cluster-slaves://@127.0.0.1:16379,127.0.0.1:16379?withMaster=true
##有账号也有密码
redis-cluster-slaves://username:passwd@127.0.0.1:16379,127.0.0.1:16379?withMaster=true

##redis-cluster-slaves会自动感知：节点宕机、主从切换和节点扩容
```

* redis-proxies
```
##本类型主要是为了代理到多个无状态的proxy节点，如codis-proxy、twemproxy等，camellia-redis-proxy会从配置的多个node中随机选择一个进行转发
##当后端的proxy node有宕机时，camellia-redis-proxy会动态剔除相关节点，如果节点恢复了则会动态加回

##有密码
redis-proxies://passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##没有密码
redis-proxies://@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
##有账号也有密码
redis-proxies://username:passwd@127.0.0.1:6379,127.0.0.2:6379,127.0.0.3:6379
```

### 动态配置
如果你希望你的proxy的路由配置可以动态变更，比如本来路由到redisA，然后动态的切换成redisB，那么你需要一个额外的配置文件，并且在application.yml中引用，如下：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      dynamic: true
      check-interval-millis: 3000
      json-file: resource-table.json
```
上面的配置表示：
* proxy的路由转发规则来自于一个配置文件（因为在文件里可以自定以配置双写、分片以及各种组合等，所以叫复杂的complex），叫resource-table.json  
* dynamic=true表示配置是动态更新的，此时proxy会定时检查resource-table.json文件是否有变更（默认5s间隔，上图配置了3s），如果有变更，则会重新reload    
* proxy默认会优先去classpath下寻找名称叫resource-table.json的配置文件    
* 此外，你也可以直接配置一个绝对路径，proxy会自动识别这种情况，如下：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      dynamic: true
      check-interval-millis: 3000
      json-file: /home/xxx/resource-table.json
```

### json-file配置示例

#### 配置单个地址
使用单独配置文件方式进行配置时，文件一般来说是一个json文件，但是如果你的配置文件里只写一个地址，也是允许的，proxy会识别这种情况，如下：
```
redis://passwd@127.0.0.1:6379
```
配置文件里只有一行数据，就是一个后端redis地址，表示proxy的路由转发规则是最简单的形式，也就是直接转发给该redis实例  
此时的配置效果和在application.yml里直接配置resource地址url效果是一样的，但是区别在于使用独立配置文件时，该地址是支持动态更新的

#### 配置读写分离一
```json
{
  "type": "simple",
  "operation": {
    "read": "redis://passwd123@127.0.0.1:6379",
    "type": "rw_separate",
    "write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"
  }
}
```
上面的配置表示：  
* 写命令会代理到redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master  
* 读命令会代理到redis://passwd123@127.0.0.1:6379

可以看到json里可以混用redis、redis-sentinel、redis-cluster  

#### 配置读写分离二
```json
{
  "type": "simple",
  "operation": {
    "read": "redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true",
    "type": "rw_separate",
    "write": "redis-sentinel://passwd123@127.0.0.1:26379/master"
  }
}
```
上面的配置表示：  
* 写命令会代理到redis-sentinel://passwd123@127.0.0.1:26379/master  
* 读命令会代理到redis-sentinel-slaves://passwd123@127.0.0.1:26379/master?withMaster=true，也就是redis-sentinel://passwd123@127.0.0.1:26379/master的主节点和所有从节点

#### 配置分片（因为之前命名错误，1.0.45及之前的版本，使用shading，1.0.46及之后的版本兼容sharding/shading）
```json
{
  "type": "sharding",
  "operation": {
    "operationMap": {
      "0-2-4": "redis://password1@127.0.0.1:6379",
      "1-3-5": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381"
    },
    "bucketSize": 6
  }
}
```
上面的配置表示key划分为6个分片，其中： 
* 分片[0,2,4]代理到redis://password1@127.0.0.1:6379
* 分片[1,3,5]代理到redis-cluster://@127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381

#### 配置双（多）写
```json
{
  "type": "simple",
  "operation": {
    "read": "redis://passwd1@127.0.0.1:6379",
    "type": "rw_separate",
    "write": {
      "resources": [
        "redis://passwd1@127.0.0.1:6379",
        "redis://passwd2@127.0.0.1:6380"
      ],
      "type": "multi"
    }
  }
}
```
上面的配置表示：  
* 所有的写命令（如setex/zadd/hset）代理到redis://passwd1@127.0.0.1:6379和redis://passwd2@127.0.0.1:6380（即双写），特别的，客户端的回包是看的配置的第一个写地址  
* 所有的读命令（如get/zrange/mget）代理到redis://passwd1@127.0.0.1:6379  

#### 配置多读
```json
{
  "type": "simple",
  "operation": {
    "read": {
      "resources": [
        "redis://password1@127.0.0.1:6379",
        "redis://password2@127.0.0.1:6380"
      ],
      "type": "random"
    },
    "type": "rw_separate",
    "write": "redis://passwd1@127.0.0.1:6379"
  }
}
```
上面的配置表示：  
* 所有的写命令（如setex/zadd/hset）代理到redis://passwd1@127.0.0.1:6379  
* 所有的读命令（如get/zrange/mget）随机代理到redis://passwd1@127.0.0.1:6379或者redis://password2@127.0.0.1:6380

#### 混合各种分片、双写逻辑（因为之前命名错误，1.0.45及之前的版本，使用shading，1.0.46及之后的版本兼容sharding/shading）
```json
{
  "type": "sharding",
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
      "5": {
        "read": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "random"
        },
        "type": "rw_separate",
        "write": {
          "resources": [
            "redis://password1@127.0.0.1:6379",
            "redis://password2@127.0.0.1:6380"
          ],
          "type": "multi"
        }
      },
      "0-2": "redis://password1@127.0.0.1:6379",
      "1-3": "redis://password2@127.0.0.1:6380"
    },
    "bucketSize": 6
  }
}
```
上面的配置表示key被划分为6个分片，其中分片4配置了读写分离和双写的逻辑，分片5设置了读写分离和双写多读的逻辑

### 集成camellia-dashboard
上述不管是通过yml还是json文件配置的方式，路由信息均是在本地，此外你也可以将路由信息托管到远程的camellia-dashboard（见[camellia-dashboard](/docs/dashboard/dashboard.md)）  
camellia-dashboard是一个web服务，proxy会定期去检查camellia-dashboard里的配置是否有变更，如果有，则会更新proxy的路由  
以下是一个配置示例：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: remote
    remote:
      bid: 1
      bgroup: default
      url: http://127.0.0.1:8080
      check-interval-millis: 5000
      dynamic: false
```
上面的配置表示proxy的路由配置会从camellia-dashboard获取，获取的是bid=1以及bgroup=default的那份配置  
此外，proxy会定时检查camellia-dashboard上的配置是否更新了，若更新了，则会更新本地配置，默认检查的间隔是5s

特别的，当你使用camellia-dashboard来托管你的proxy配置之后，proxy就有了同时服务多个业务的能力  
比如A业务访问proxy，proxy将其代理到redis1；B业务访问同一个proxy，proxy可以将其代理到redis2，此时proxy的配置如下：  
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: remote
    remote:
      url: http://127.0.0.1:8080 #camellia-dashbaord的地址
      check-interval-millis: 5000 #到camellia-dashbaord的轮询周期
      dynamic: true #表示支持多组配置，默认就是true
      bid: 1 #默认的bid，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      bgroup: default #默认的bgroup，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
```   
proxy是通过clientName来识别不同的业务，如下：
```
➜ ~ ./redis-cli -h 127.0.0.1 -p 6380 -a pass123
127.0.0.1:6379> client setname camellia_10_default
OK
127.0.0.1:6380> set k1 v1
OK
127.0.0.1:6380> get k1
"v1"
127.0.0.1:6380> mget k1 k2 k3
1) "v1"
2) (nil)
3) (nil)
```
上面示例表示，请求proxy之后，要求proxy按照camellia-dashboard中bid=10，bgroup=default的那份配置进行路由转发  
特别的，如果端侧是Java，且使用了Jedis，则可以这样调用：
```java
public class Test {
    public static void main(String[] args) {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1", 6380,
                2000, "pass123", 0, "camellia_10_default");
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.setex("k1", 10, "v1");
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}
```

### 集成ProxyRouteConfUpdater自定义管理多组动态配置
集成camellia-dashboard后，proxy就有了同时服务多个业务的能力（通过bid/bgroup来区分业务），如果你不想使用camellia-dashboard，那么你可以自定义ProxyRouteConfUpdater来实现相关逻辑   
ProxyRouteConfUpdater是一个抽象类，你需要自己去实现一个子类，在ProxyRouteConfUpdater对象实例的内部，你至少需要实现以下方法：
```
public abstract ResourceTable getResourceTable(long bid, String bgroup);
```
proxy在启动时会默认调用该方法去获取初始的路由配置。（备注：配置都可以用前文说过的json字符串去描述，你可以用ReadableResourceTableUtil.parseTable(String conf)方法来转成ResourceTable对象）  
此外，当路由配置发生了变更，你可以调用ProxyRouteConfUpdater提供的回调方法去实时变更，回调方法如下：
```
public final void invokeUpdateResourceTable(long bid, String bgroup, ResourceTable resourceTable)
```
开启ProxyRouteConfUpdater的示例配置如下：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.command.async.route.DynamicConfProxyRouteConfUpdater
      dynamic: true #表示支持多组配置，默认就是true
      bid: 1 #默认的bid，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      bgroup: default #默认的bgroup，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      reload-interval-millis: 600000 #使用ProxyRouteConfUpdater时，配置变更会通过回调自动更新，为了防止更新出现丢失，会有一个兜底轮询机制，本配置表示兜底轮询的间隔，默认10分钟
```
上面的配置表示我们使用DynamicConfProxyRouteConfUpdater这个ProxyRouteConfUpdater的实现类，这个实现类下，配置托管给了ProxyDynamicConf(camellia-redis-proxy.properties)   
使用DynamicConfProxyRouteConfUpdater时的配置方式是以k-v的形式进行配置，如下：
```
#表示bid=1/bgroup=default的路由配置
1.default.route.conf=redis://@127.0.0.1:6379
#表示bid=2/bgroup=default的路由配置
2.default.route.conf={"type": "simple","operation": {"read": "redis://passwd123@127.0.0.1:6379","type": "rw_separate","write": "redis-sentinel://passwd2@127.0.0.1:6379,127.0.0.1:6378/master"}}
```
除了camellia提供的DynamicConfProxyRouteConfUpdater，你可以自己实现一个自定义的ProxyRouteConfUpdater实现，从而对接到你们的配置中心中，下面提供了一个自定义实现的例子：
```java
public class CustomProxyRouteConfUpdater extends ProxyRouteConfUpdater {

    private String url = "redis://@127.0.0.1:6379";

    public CustomProxyRouteConfUpdater() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::update, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public ResourceTable getResourceTable(long bid, String bgroup) {
        return ReadableResourceTableUtil.parseTable(url);
    }

    private void update() {
        String newUrl = "redis://@127.0.0.2:6379";
        if (!url.equals(newUrl)) {
            url = newUrl;
            invokeUpdateResourceTableJson(1, "default", url);
        }
    }
}
```
上述的例子中，proxy一开始的路由是redis://@127.0.0.1:6379，10s之后，被切换到了redis://@127.0.0.2:6379

### 使用自定义ClientAuthProvider来实现通过不同的登录密码来指向不同路由配置
前面我们已经知道proxy是通过不同的big/bgroup来映射不同的路由配置  
对于客户端来说，默认情况下是通过client setname命令来选择所需要的bid/bgroup，从而指向不同的路由配置  
此外，proxy还支持通过自定义的ClientAuthProvider来实现通过不同的登录密码来指向不同的bid/bgroup，如下：  
```java
public class MockClientAuthProvider implements ClientAuthProvider {

    @Override
    public ClientIdentity auth(String userName, String password) {
        ClientIdentity clientIdentity = new ClientIdentity();
        if (password.equals("pass1")) {
            clientIdentity.setPass(true);
            clientIdentity.setBid(1L);
            clientIdentity.setBgroup("default");
        } else if (password.equals("pass2")) {
            clientIdentity.setPass(true);
            clientIdentity.setBid(2L);
            clientIdentity.setBgroup("default");
        } else if (password.equals("pass3")) {
            clientIdentity.setPass(true);
        }
        return clientIdentity;
    }

    @Override
    public boolean isPasswordRequired() {
        return true;
    }
}
```  
随后在application.yml里配置如下：  
```yaml
camellia-redis-proxy:
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.samples.MockClientAuthProvider #处理认证逻辑的类
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.command.async.route.DynamicConfProxyRouteConfUpdater
      dynamic: true #表示支持多组配置，默认就是true
      bid: 1 #默认的bid，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      bgroup: default #默认的bgroup，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      reload-interval-millis: 600000 #使用ProxyRouteConfUpdater时，配置变更会通过回调自动更新，为了防止更新出现丢失，会有一个兜底轮询机制，本配置表示兜底轮询的间隔，默认10分钟  
```
上面的例子表示：
* 当使用密码pass1登录proxy时，使用bid=1,bgroup=default的路由
* 当使用密码pass2登录proxy时，使用bid=2,bgroup=default的路由
* 当使用密码pass3登录proxy时，使用默认路由，也就是bid=1,bgroup=default

特别的，proxy内置了基于ProxyDynamicConf的实现，大家可以按需使用，启用方式如下：
```yaml
camellia-redis-proxy:
  monitor-enable: true  #是否开启监控
  monitor-interval-seconds: 60 #监控回调的间隔
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.command.auth.DynamicConfClientAuthProvider #处理认证逻辑的类
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.command.async.route.DynamicConfProxyRouteConfUpdater
      dynamic: true #表示支持多组配置，默认就是true
      bid: 1 #默认的bid，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      bgroup: default #默认的bgroup，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      reload-interval-millis: 600000 #使用ProxyRouteConfUpdater时，配置变更会通过回调自动更新，为了防止更新出现丢失，会有一个兜底轮询机制，本配置表示兜底轮询的间隔，默认10分钟  
```
随后，你可以在camellia-redis-proxy.properties里如下配置：  
```
pass123.auth.conf=1|default
pass456.auth.conf=2|default
```
上述例子表示：  
* 当使用密码pass123登录proxy时，使用bid=1,bgroup=default的路由
* 当使用密码pass456登录proxy时，使用bid=2,bgroup=default的路由

感谢[@yangxb2010000](https://github.com/yangxb2010000)提供上述功能

### 不同的双（多）写模式
proxy支持设置双（多）写的模式，有三个可选项：  
#### first_resource_only
表示如果配置的第一个写地址返回了，则立即返回给客户端，这是默认的模式
#### all_resources_no_check
表示需要配置的所有写地址都返回了，才返回给给客户端，返回的是第一个地址的返回结果，你可以这样配置来生效这种模式：  
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      json-file: resource-table.json
    redis-conf:
      multi-write-mode: all_resources_no_check
```
#### all_resources_check_error
表示需要配置的所有写地址都返回了，才返回给客户端，并且会校验是否所有地址都是返回的非error结果，如果是，则返回第一个地址的返回结果；否则返回第一个错误结果，你可以这样配置来生效这种模式：  
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      json-file: resource-table.json
    redis-conf:
      multi-write-mode: all_resources_check_error
```  


### 自定义分片函数
你可以自定义分片函数，分片函数会计算出一个key的哈希值，和分片大小（bucketSize）取余后，得到该key所属的分片。  
默认的分片函数是com.netease.nim.camellia.core.client.env.DefaultShardingFunc  
你可以继承com.netease.nim.camellia.core.client.env.AbstractSimpleShardingFunc实现自己想要的分片函数，类似于这样：  
```java
package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.core.client.env.AbstractSimpleShardingFunc;

public class CustomShardingFunc extends AbstractSimpleShardingFunc {
    
    @Override
    public int shardingCode(byte[] key) {
        if (key == null) return 0;
        if (key.length == 0) return 0;
        int h = 0;
        for (byte d : key) {
            h = 31 * h + d;
        }
        return (h < 0) ? -h : h;
    }
}
```  
然后在application.yml配置即可，类似于这样：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      json-file: resource-table.json
    redis-conf:
      sharding-func: com.netease.nim.camellia.redis.proxy.samples.CustomShardingFunc
```
