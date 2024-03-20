## 部署和接入
在生产环境，需要部署至少2个proxy实例来保证高可用，并且proxy是可以水平扩展的
注意：
- 如果后端pod大于15个以上，避免K8S内部DNS的问题，可以使用statefulset，使用多个svc来分散代理一定数量的statefulset
- 如果小于15个，则可以使用deployment类型部署

## 目录
### 部署模式
* 基于lb组成集群（如lvs，或者k8s中的service等），见：[standalone_mode](standalone_mode.md)
* 基于注册中心组成集群，见：[register_mode](register_mode.md)
* 伪redis-cluster模式，见：[cluster_mode](cluster_mode.md)
* 伪redis-sentinel模式，见：[sentinel_mode](sentinel_mode.md)
* jvm-in-sidecar模式，见：[jvm-in-sidecar](jvm-in-sidecar.md)

### 其他
* 优雅上下线，见：[console](console.md)
* docker-compose 快速部署读写分离模式 [docker-compose](docker-compose/docker-compose-rw-separate.yaml)
* kubernetes 部署模板 [kubernetes](kubernetes/camellia-deployment.yaml)
* 构建镜像样例 [docker-build](docker/Dockerfile)
* proxy默认使用java8/spring-boot2运行，如果要使用java21/spring-boot3，请参考：[camellia-jdk21-bootstraps](https://github.com/caojiajun/camellia-jdk21-bootstraps)