
# camellia-redis-proxy-bootstrap-all-in-one

### 0、引入了如下可插拔的组件（默认都是不生效的）

* etcd管理配置文件
* nacos管理配置文件
* 使用zookeeper作为注册中心
* 使用hbase模拟redis协议
* 使用obkv的hbase-client模拟redis协议
* 使用tikv模拟redis协议

### 1、安装java（jdk8+，至少1.8.0_202）
如已经安装则跳过

### 2、安装maven
参考：[maven](https://github.com/apache/maven)

### 3、编译camellia-redis-proxy-bootstrap-all-in-one并打包
```shell
git clone https://github.com/netease-im/camellia.git
git checkout v1.2.28
cd camellia
mvn clean package
cp camellia-redis-proxy/camellia-redis-proxy-bootstrap-all-in-one/target/camellia-redis-proxy-bootstrap-all-in-one-1.2.28.jar /yourdict/redis-proxy/camellia-redis-proxy-bootstrap-all-in-one-1.2.28.jar
cd /yourdict/redis-proxy
jar xvf camellia-redis-proxy-bootstrap-all-in-one-1.2.28.jar
rm -rf camellia-redis-proxy-bootstrap-all-in-one-1.2.28.jar
touch start.sh
echo "java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher" > start.sh
chmod +x start.sh
cd ..
tar zcvf redis-proxy.tar.gz ./redis-proxy
```