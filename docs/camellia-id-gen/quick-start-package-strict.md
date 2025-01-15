

下载最新版安装包并解压（v1.3.2）：
```
wget https://github.com/netease-im/camellia/releases/download/1.3.2/camellia-id-gen-strict-server-1.3.2.tar.gz
tar zxvf camellia-id-gen-strict-server-1.3.2.tar.gz
cd camellia-id-gen-strict-server-1.3.2/
```
按需修改BOOT-INF/classes/下的配置文件：
* application.yml
* logback.xml

按需调整start.sh的启动参数（主要是JVM参数），默认参数如下（确保已经安装了java21或以上，并添加到path）：
```
java -XX:+UseG1GC -Dio.netty.tryReflectionSetAccessible=true --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.access=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.net.util=ALL-UNNAMED -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
```
直接启动即可：
```
./start.sh
```