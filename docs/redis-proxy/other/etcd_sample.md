

## 使用etcd管理并启动proxy的一个完整示例

示例说明：
* 需要使用etcd管理proxy配置
* 使用使用proxy的双写功能
* 使用proxy的伪集群模式

### 1. 安装etcd

### 2、下载camellia-redis-proxy
参考：[quick_start](../quickstart/quick-start-package.md)

### 3、下载camellia-redis-proxy-config-etcd

```
wget https://repo1.maven.org/maven2/com/netease/nim/camellia-redis-proxy-config-etcd/1.2.18/camellia-redis-proxy-config-etcd-1.2.18.jar
```

### 4、配置etcd

* 把3中的camellia-redis-proxy-config-etcd-1.2.18.jar复制到camellia-redis-proxy的`./BOOT-INF/lib`目录下
* 修改`./BOOT-INF/classes/logback.xml`，可以参考logback-sample.xml，修改LOG_HOME即可，也可以不修改（则日志会输出到控制台）
* 修改`./BOOT-INF/classes/application.yml`，添加etcd配置
* 其他配置文件均可以删除（因为我们使用etcd管理配置，所以不需要其他配置文件）

必填项：  
* etcd.target和etcd.endpoints二选一
* etcd.config.key，表示从etcd中获取配置的路径
* etcd.config.type，配置类型，建议json（也支持properties）


```yml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  console-port: 16379
#  monitor-enable: false
#  monitor-interval-seconds: 60
  client-auth-provider-class-name: com.netease.nim.camellia.redis.proxy.auth.MultiTenantClientAuthProvider
  cluster-mode-enable: true #cluster-mode，把proxy伪装成cluster
  proxy-dynamic-conf-loader-class-name: com.netease.nim.camellia.redis.proxy.config.etcd.EtcdProxyDynamicConfLoader
  config:
    "etcd.target": "ip:///etcd0:2379,etcd1:2379,etcd2:2379"
#    "etcd.endpoints": "http://etcd0:2379,http://etcd1:2379,http://etcd2:2379" #etcd.target和etcd.endpoints二选一，优先使用etcd.target
#    "etcd.user": "xx"
#    "etcd.password": "xx"
#    "etcd.namespace": "xx"
#    "etcd.authority": "xx"
    "etcd.config.key": "/xx/xxx"
    "etcd.config.type": "json" #也可以配置为json/properties
#  plugins: #引入哪些插件，内建的插件可以用别名，自定义插件用全类名
#    - monitorPlugin
#    - bigKeyPlugin
#    - hotKeyPlugin
  transpond:
    type: custom
    custom:
      proxy-route-conf-updater-class-name: com.netease.nim.camellia.redis.proxy.route.MultiTenantProxyRouteConfUpdater
```

### 5、在etcd中配置路由

json示例：
```json
{
    "proxy.cluster.mode.nodes": "127.0.0.1:6380@16380,127.0.0.2:6380@16380",
    "multi.tenant.route.config":
    [
        {
            "name": "route1",
            "password": "pass123",
            "route":
            {
                "type": "simple",
                "operation":
                {
                    "read": "redis-cluster://@127.0.0.1:6379,127.0.0.1:6378",
                    "type": "rw_separate",
                    "write":
                    {
                        "resources":
                        [
                          "redis-cluster://@127.0.0.1:6379,127.0.0.1:6378",
                          "redis-cluster://@127.0.0.1:7379,127.0.0.1:7378"
                        ],
                        "type": "multi"
                    }
                }
            }
        }
    ]
}
```

字段说明：
* proxy.cluster.mode.nodes表示proxy的节点列表
* multi.tenant.route.config表示路由配置，是一个数组，可以配置多个路由，通过不同的密码区分
* name表示路由名称，不得重复
* password表示密码，用于区分不同的路由
* route表示路由规则，示例中表达了一个简单的双写规则

其他路由配置方式，可以参考：[complex](../auth/complex.md) 和 [redis-resource](../auth/redis-resources.md)

一个etcd的配置截图（使用了etcd-manager这个ui工具）：     
<img src="etcd.jpg" width="100%" height="100%">

### 6、启动proxy

调用`./start.sh`依次启动所有proxy节点即可