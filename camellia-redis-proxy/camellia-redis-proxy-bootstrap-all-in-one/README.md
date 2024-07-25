

### camellia-redis-proxy-bootstrap-all-in-one

引入了如下可插拔的组件（默认都是不生效的）：

* etcd管理配置文件
* nacos管理配置文件
* 使用zookeeper作为注册中心
* 使用hbase模拟redis协议
* 使用obkv的hbase-client模拟redis协议
* 使用tikv模拟redis协议

