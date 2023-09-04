
## 快速开始（基于安装包）

下载最新版安装包并解压（v1.2.16）：
```
wget https://github.com/netease-im/camellia/releases/download/1.2.16/camellia-redis-proxy-1.2.16.tar.gz
tar zxvf camellia-redis-proxy-1.2.16.tar.gz
cd camellia-redis-proxy-1.2.16/
```
按需修改BOOT-INF/classes/下的配置文件：
* application.yml
* logback.xml
* camellia-redis-proxy.properties
* resource-table.json

按需调整start.sh的启动参数（主要是JVM参数），默认参数如下（确保已经安装了jdk8或以上，并添加到path）：
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```
直接启动即可：
```
./start.sh
```

如果是在docker里，则调用start_in_docker.sh，默认参数如下（主要是添加了UseContainerSupport，需要jdk8u191以上）：
```
java -XX:+UseG1GC -XX:+UseContainerSupport -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```
启动如下：
```
./start_in_docker.sh
```


特别的，在某些业务场景中下，可能希望proxy的可执行文件与配置文件application.yml分开，此时可以把tar包重新打成jar包，并配合spring提供的spring.config.location参数来指定application.yml，具体如下：
```
wget https://github.com/netease-im/camellia/releases/download/1.2.16/camellia-redis-proxy-1.2.16.tar.gz
tar zxvf camellia-redis-proxy-1.2.16.tar.gz
cd camellia-redis-proxy-1.2.16/
jar -cvf0M camellia-redis-proxy.jar BOOT-INF/ META-INF/ org/
```
此时，就会生成一个camellia-redis-proxy.jar，随后使用java -jar命令启动即可，如下：
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server -jar camellia-redis-proxy.jar --spring.config.location=file:/xxx/xxx/application.yml
```
如果是在docker里，记得添加UseContainerSupport参数，如下：
```
java -XX:+UseG1GC -XX:+UseContainerSupport -Xms4096m -Xmx4096m -server -jar camellia-redis-proxy.jar --spring.config.location=file:/xxx/xxx/application.yml
```

如果需要使用jdk17运行proxy，则增加启动参数`--add-opens java.base/java.lang=ALL-UNNAMED`，如下：
```
java -XX:+UseG1GC --add-opens java.base/java.lang=ALL-UNNAMED -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```

How to build snapshot *.tar.gz package: [build-snapshot-package](build-snapshot-package.md)


How to run Camellia Redis Proxy server using service - 如何使用服务运行Camellia Redis代理服务器。

```
groupadd camelliarp
useradd camelliarp -g camelliarp
wget https://github.com/netease-im/camellia/releases/download/1.2.16/camellia-redis-proxy-1.2.16.tar.gz
tar zxvf camellia-redis-proxy-1.2.16.tar.gz
mv camellia-redis-proxy-1.2.16 /home/camelliarp/crp
chown -R camelliarp:camelliarp /home/camelliarp/crp
```

vi /etc/systemd/system/camellia_rp.service
```
[Unit]
Description=Camellia Redis Proxy service.
After=network-online.target
Wants=network-online.target

[Service]
SuccessExitStatus=143
User=camelliarp
Group=camelliarp
Type=simple
WorkingDirectory=/home/camelliarp/crp
NoNewPrivileges=yes
NonBlocking=yes
PrivateDevices=true
PrivateTmp=true
ProtectSystem=full
RuntimeDirectory=camelliarp
RuntimeDirectoryMode=0755
ExecStart=/usr/bin/java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
ExecStop=/bin/kill -15 $MAINPID
KillMode=mixed

[Install]
WantedBy=multi-user.target
```

Activate service
```
systemctl enable camellia_rp.service --now
```
