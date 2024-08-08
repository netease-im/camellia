
# camellia-redis-proxy-bootstrap

### 0、引入了如下可插拔的组件（默认都是不生效的）

* 默认，使用：`mvn clean package`
* etcd管理配置文件，使用: `mvn clean package -Petcd-config`
* nacos管理配置文件，使用: `mvn clean package -Pnacos-config`
* 使用zookeeper作为注册中心，使用: `mvn clean package -Pzk-register`
* 使用hbase模拟redis协议，使用: `mvn clean package -Pkv-hbase`
* 使用obkv的hbase-client模拟redis协议，使用: `mvn clean package -Pkv-obkv`
* 使用tikv模拟redis协议，使用: `mvn clean package -Pkv-tikv`

### 1、安装java（jdk8+，至少1.8.0_202）
如已经安装则跳过

### 2、安装maven
参考：[maven](https://github.com/apache/maven)

### 3、编译camellia-redis-proxy-bootstrap并打包
```shell
git clone https://github.com/netease-im/camellia.git
git checkout {BRANCH}
cd camellia
mvn clean package
cp camellia-redis-proxy/camellia-redis-proxy-bootstrap/target/camellia-redis-proxy-bootstrap-xxx.jar /yourdict/redis-proxy/camellia-redis-proxy-bootstrap-xxx.jar
cd /yourdict/redis-proxy
jar xvf camellia-redis-proxy-bootstrap-xxx.jar
rm -rf camellia-redis-proxy-bootstrap-xxx.jar
touch start.sh
echo "java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher" > start.sh
chmod +x start.sh
cd ..
tar zcvf redis-proxy.tar.gz ./redis-proxy
```