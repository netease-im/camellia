
## 快速开始（基于安装包）

下载最新版安装包并解压（v1.0.42）：
```
wget https://github.com/netease-im/camellia/releases/download/v1.0.42/camellia-redis-proxy-1.0.42.tar.gz
tar zxvf camellia-redis-proxy-1.0.42.tar.gz
cd camellia-redis-proxy-1.0.42/
```
按需修改BOOT-INF/classes/下的配置文件：
* application.yml
* logback.xml
* camellia-redis-proxy.properties
* resource-table.json

按需调整start.sh的启动参数（主要是JVM参数），默认参数如下（确保已经安装了jdk8或以上，并添加到path）：
```
java -XX:+UseG1GC -Xms2048m -Xmx2048m -server org.springframework.boot.loader.JarLauncher
```
直接启动即可：
```
./start.sh
```

如果是在docker里，则调用start_in_docker.sh，默认参数如下（主要是添加了UseContainerSupport，需要jdk8u191以上）：
```
java -XX:+UseG1GC -XX:+UseContainerSupport -Xms2048m -Xmx2048m -server org.springframework.boot.loader.JarLauncher
```
启动如下：
```
./start_in_docker.sh
```