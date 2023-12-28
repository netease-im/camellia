## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的   

## 目录
* 基于lb组成集群（如lvs，或者k8s中的service等），见：[lb](lb.md)
* 基于注册中心组成集群，见：[register](register.md)
* 伪redis-cluster模式，见：[cluster_mode](cluster_mode.md)
* 伪redis-sentinel模式，见：[sentinel_mode](sentinel_mode.md)
* jvm-in-sidecar模式，见：[jvm-in-sidecar](jvm-in-sidecar.md)
* 优雅上下线，见：[console](console.md)

