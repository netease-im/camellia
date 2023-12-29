
## 快速开始（基于安装包）

下载最新版安装包并解压（v1.2.21）：
```
wget https://github.com/netease-im/camellia/releases/download/1.2.21/camellia-redis-proxy-1.2.21.tar.gz
tar zxvf camellia-redis-proxy-1.2.21.tar.gz
cd camellia-redis-proxy-1.2.21/
```
按需修改BOOT-INF/classes/下的配置文件：
* application.yml
* logback.xml
* camellia-redis-proxy.properties

按需调整start.sh的启动参数（主要是JVM参数），默认参数如下（确保已经安装了`jdk1.8.0_202`或以上，并添加到path）：
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```
直接启动即可：
```
./start.sh
```

特别的，在某些业务场景中下，可能希望proxy的可执行文件与配置文件application.yml分开，此时可以把tar包重新打成jar包，并配合spring提供的spring.config.location参数来指定application.yml，具体如下：
```
wget https://github.com/netease-im/camellia/releases/download/1.2.21/camellia-redis-proxy-1.2.21.tar.gz
tar zxvf camellia-redis-proxy-1.2.21.tar.gz
cd camellia-redis-proxy-1.2.21/
jar -cvf0M camellia-redis-proxy.jar BOOT-INF/ META-INF/ org/
```
此时，就会生成一个camellia-redis-proxy.jar，随后使用java -jar命令启动即可，如下：
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server -jar camellia-redis-proxy.jar  
```

proxy默认会去`./BOOT-INF/classes/`目录下加载配置文件，你可以通过以下启动参数去指定配置文件（application.yml、logback.xml、camellia-redis-proxy.properties/camellia-redis-proxy.json）
```
--spring.config.location=file:/xxx/xxx/application.yml
```
```
-Dlogging.config=/xxx/xxx/logback.xml
```
```
-Ddynamic.conf.file.path=/xxx/xxx/camellia-redis-proxy.properties
-Ddynamic.conf.file.path=/xxx/xxx/camellia-redis-proxy.json
```



others：  

* How to run in jdk11/jdk17/jdk21, [jdk17](jdk17.md)  

* How to build/run by spring-boot3/jdk21/docker, [camellia-jdk21-bootstraps](https://github.com/caojiajun/camellia-jdk21-bootstraps)  

* How to build latest snapshot *.tar.gz package: [build-snapshot-package](build-snapshot-package.md)  

* How to run Camellia Redis Proxy server using service: [run-as-service](run-as-services.md)  

