## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的


## 目录
### 部署模式
* 基于lb组成集群（如lvs，或者k8s中的service等），见：[standalone_mode](standalone_mode.md)
* 基于注册中心组成集群，见：[register_mode](register_mode.md)
* 伪redis-cluster模式，见：[cluster_mode](cluster_mode.md)
* 伪redis-sentinel模式，见：[sentinel_mode](sentinel_mode.md)
* jvm-in-sidecar模式，见：[jvm-in-sidecar](jvm-in-sidecar.md)

### 其他
* 优雅上下线，见：[console](console.md)
* docker-compose 快速部署读写分离模式，见： [docker-compose](docker-compose/docker-compose-rw-separate.yaml)
* kubernetes 部署模板，见： [kubernetes](kubernetes/camellia-deployment.yaml)
* 构建镜像样例，见： [docker-build](docker/Dockerfile)
* proxy默认使用java8/spring-boot2运行，如果要使用java21/spring-boot3，请参考：[camellia-jdk21-bootstraps](https://github.com/caojiajun/camellia-jdk21-bootstraps)
* k8s环境部署注意事项：  
  - 如果使用k8s的svc模式为clusterIP: None,当pod大于15个以上,避免K8S内部DNS的问题,可以使用多个svc分散代理一定数量的statefulset(原因:udp的dns请求最大响应大小为512字节，超过后udp返回的dns响应会设置Truncate)
  - 如果后端redis和camellia-redis-proxy实例在短时间内先后发生重启/宕机,默认情况下可能会导致camellia-redis-proxy初始化后不监听端口,建议配置preheat: false或者 "upstream.lazy.init.enable": true,请参考:[初始化](https://github.com/netease-im/camellia/blob/master/docs/redis-proxy/other/init.md)

* 你可以使用kube-blocks去一键部署camellia-redis-proxy，请参考：[kube-blocks](https://www.kubeblocks.io/docs/preview/api_docs/overview/supported-addons)