
## QuickStart

download *.tar.gz（v1.3.7）：
```
wget https://github.com/netease-im/camellia/releases/download/1.4.0/camellia-redis-proxy-1.4.0.tar.gz
tar zxvf camellia-redis-proxy-1.4.0.tar.gz
cd camellia-redis-proxy-1.4.0/
```
modify config in ./BOOT-INF/classes/
* application.yml
* logback.xml
* camellia-redis-proxy.properties

prerequisite `jdk21`, run by: 
```
./start.sh
```

if want run by jar, you can build a jar file like this:  
```
wget https://github.com/netease-im/camellia/releases/download/1.3.7/camellia-redis-proxy-1.3.7.tar.gz
tar zxvf camellia-redis-proxy-1.3.7.tar.gz
cd camellia-redis-proxy-1.3.7/
jar -cvf0M camellia-redis-proxy.jar BOOT-INF/ META-INF/ org/
```
then you can run like this:  
```
java -XX:+UseG1GC -Dio.netty.tryReflectionSetAccessible=true --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.security=ALL-UNNAMED --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/jdk.internal.access=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/sun.net.util=ALL-UNNAMED -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher  
```

proxy will load config from `./BOOT-INF/classes/` by default, you can use startup parameter to specify config file path  
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

* How to build latest snapshot *.tar.gz package: [build-snapshot-package](build-snapshot-package.md)  

* How to run Camellia Redis Proxy server using service: [run-as-service](run-as-services.md)  

