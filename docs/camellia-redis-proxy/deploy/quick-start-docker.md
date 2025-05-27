
## QuickStart

* docker-compose 快速部署读写分离模式，见： [docker-compose](docker-compose/docker-compose-rw-separate.yaml)
* kubernetes 部署模板，见： [kubernetes](kubernetes/camellia-deployment.yaml)
* 构建镜像样例，见： [docker-build](docker/Dockerfile-jdk21)
* k8s环境部署注意事项：
    - 如果使用k8s的svc模式为clusterIP: None,当pod大于15个以上,避免K8S内部DNS的问题,可以使用多个svc分散代理一定数量的statefulset(原因:udp的dns请求最大响应大小为512字节，超过后udp返回的dns响应会设置Truncate)
    - 如果后端redis和camellia-redis-proxy实例在短时间内先后发生重启/宕机,默认情况下可能会导致camellia-redis-proxy初始化后不监听端口,建议配置preheat: false或者 "upstream.lazy.init.enable": true,请参考:[初始化](https://github.com/netease-im/camellia/blob/master/docs/camellia-redis-proxy/other/init.md)

* 你可以使用kube-blocks去一键部署camellia-redis-proxy，请参考：[kube-blocks](https://www.kubeblocks.io/docs/preview/api_docs/overview/supported-addons)