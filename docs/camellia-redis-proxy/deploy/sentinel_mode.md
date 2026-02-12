## 伪redis-sentinel模式

* 伪redis-sentinel模式，如下：  
  <img src="redis-proxy-sentinel.png" width="60%" height="60%">

* 此时，可以把proxy节点同时当作sentinel节点和redis节点，通过不同的端口区分，通过cport去模拟sentinel获取master节点的请求，返回的是proxy自己（多个proxy节点哈希选一个，从而不同的proxy节点返回的master是相同的）     
* 客户端需要同时监听多个proxy节点（当作sentinel节点），当一个proxy节点宕机，则另外一个proxy节点会通过sentinel协议去告知客户端（只会告知把master设置为宕机节点的客户端），master已经变成我了，快切过来  

* 这种模式下，可以把proxy集群当作一个redis-sentinel集群去访问，从而不需要外部服务即可组成高可用集群

* 对于不同的客户端，会下发不同的proxy节点作为伪master，从而达到负载均衡的效果
* 当一台proxy节点挂了，会通知正在使用该节点的客户端进行伪master切换，从而达到高可用的效果

```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  #port: -6379 #优先级高于server.port，如果缺失，则使用server.port，如果设置为-6379则会随机一个可用端口
  #application-name: camellia-redis-proxy-server  #优先级高于spring.application.name，如果缺失，则使用spring.application.name
  console-port: 16379 #console端口，默认是16379，如果设置为-16379则会随机一个可用端口
  cport: 16380 #cluster-mode下的心跳端口，默认是proxy端口+10000
  password: pass123
  sentinel-mode-enable: true #cluster-mode，把proxy伪装成cluster，需要在camellia-redis-proxy.properties配置proxy.cluster.mode.nodes
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
```     

配置节点发现规则（基于配置文件）
```properties
### 此时集群内的节点会通过配置文件彼此发现
proxy.sentinel.mode.nodes.provider.class.name=com.netease.nim.camellia.redis.proxy.sentinel.
#把所有proxy节点配置上，格式为ip:port@cport
proxy.sentinel.mode.nodes=192.168.3.218:6380@16380,192.168.3.218:6390@16390
```
配置节点发现规则（基于redis）
```properties
### 此时集群内的节点会通过redis自动彼此发现，优点是可以不需要提前知道节点ip，适用于k8s环境
proxy.sentinel.mode.nodes.provider.class.name=com.netease.nim.camellia.redis.proxy.sentinel.
proxy.sentinel.mode.nodes.provider.redis.url=redis://passwd@127.0.0.1
proxy.sentinel.mode.nodes.provider.redis.key=xxxx
```


配置sentinel的名字
```
#sentinel里模拟的master的名字，默认是camellia_sentinel
proxy.sentinel.mode.master.name=camellia_sentinel
```

其他可以配置的参数：
```
#proxy节点间的心跳间隔，表示了心跳请求的频率
proxy.sentinel.mode.heartbeat.interval.seconds=5
#proxy节点间的心跳超时，20s没有收到心跳，则会剔除该节点
proxy.sentinel.mode.heartbeat.timeout.seconds=20
#proxy节点的ip，默认会自动获取本机ip，一般不需要配置
proxy.sentinel.mode.current.node.host=10.1.1.1
#sentinel的账号，默认null
proxy.sentinel.mode.sentinel.username=xxx
#sentinel的密码，默认null
proxy.sentinel.mode.sentinel.password=xxx
```

```高可用原理

1、客户端把proxy当作sentinel节点，通过cport去连接
2、客户端会通过sentinel协议，尝试获取master节点，proxy会根据客户端ip，做哈希，返回其中一个proxy节点
3、客户端会通过sentinel协议，去监听master节点的变化（需要对所有proxy节点cport端口建立监听，就和真正的sentinel集群一样）
4、proxy节点间会互相发送心跳，从而感知到节点的存活和宕机，维护在线proxy节点列表
5、当有一个proxy节点宕机后，另外一个proxy节点会发现，于是会到连到自己的所有客户端（过滤出哈希结果为宕机节点的客户端），发送一个master变化的通知，让客户端切到新的proxy节点

例子：
1）假设有一个客户端c，有两个proxy，a和b
2）c连接a和b的cport端口，询问master是谁，proxy根据c的客户端ip哈希，均返回a是master
3）c连接a的port端口，开启正常业务请求（get/set等）
4）b宕机，因为业务请求在a上，业务无影响
5）a宕机，b发现到a的心跳不通，同时发现刚才告诉了c，master是a，于是通过cport那个端口建立的监听连接，发送一个通知给到c，告诉他，master变了
6）c收到通知，重新建立到b的连接，继续开启正常业务请求（get/set等）
7）a重启后，b检测到a可用，计算哈希后，会继续发一个变更通知给c，流量回切到a，从而避免所有流量均在b节点导致负载不均衡

```

通过console的/online接口和/offline接口可以完成节点的上下线  