
## 快速开始（基于fatJar和sample-code)

下载源码，切到最新稳定分支（v1.2.1）
```
git clone https://github.com/netease-im/camellia.git
cd camellia
git checkout v1.2.1
```
按需修改[sample-code](/) 中的配置文件：
* application.yml
* logback.xml
* camellia-redis-proxy.properties
* resource-table.json

使用maven编译
```
mvn clean install
```
找到可执行jar包，使用java -jar命令运行即可(注意设置内存和GC，并确保已经安装了jdk8或以上，并添加到path）：
```
cd camellia-samples/camellia-redis-proxy-samples/target
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server -jar camellia-redis-proxy-samples-1.2.1.jar 
```

如果是在容器环境，则需要添加UseContainerSupport参数（需要jdk8u191以上），如下：
```
cd camellia-samples/camellia-redis-proxy-samples/target
java -XX:+UseG1GC -XX:+UseContainerSupport -Xms4096m -Xmx4096m -server -jar camellia-redis-proxy-samples-1.2.1.jar
```