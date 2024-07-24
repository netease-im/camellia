

下载最新版安装包并解压（v1.1.13）：
```
wget https://github.com/netease-im/camellia/releases/download/1.1.13/camellia-delay-queue-server-1.1.13.tar.gz
tar zxvf camellia-delay-queue-server-1.1.13.tar.gz
cd camellia-delay-queue-server-1.1.13/
```
按需修改BOOT-INF/classes/下的配置文件：
* application.yml
* logback.xml

按需调整start.sh的启动参数（主要是JVM参数），默认参数如下（确保已经安装了jdk8或以上，并添加到path）：
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```
直接启动即可：
```
./start.sh
```