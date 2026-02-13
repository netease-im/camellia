
## redis-cluster模式

redis-cluster模式，如下：  
  <img src="redis-proxy-cluster.jpg" width="60%" height="60%">

此时，可以把proxy集群当作一个redis-cluster集群去访问，从而不需要外部服务即可组成高可用集群

### 方案一

具体见: [cluster-mode-1](cluster_mode-1.md)

### 方案二

具体见: [cluster-mode-2](cluster_mode-2.md)

### 上下线
通过console的/online接口和/offline接口可以完成节点的上下线


### 其他配置

```properties
#是否对每一个命令都检查slot以及是否要重定向，默认false
cluster.mode.command.move.always=false
```
