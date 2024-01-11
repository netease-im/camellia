
## QuickStart

download *.tar.gz（v1.2.25）：
```
wget https://github.com/netease-im/camellia/releases/download/1.2.25/camellia-redis-proxy-1.2.25.tar.gz
tar zxvf camellia-redis-proxy-1.2.25.tar.gz
cd camellia-redis-proxy-1.2.25/
```
modify config in ./BOOT-INF/classes/
* application.yml
* logback.xml
* camellia-redis-proxy.properties

prerequisite `jdk1.8.0_202`, run by: 
```
./start.sh
```

if want run by jar, you can build a jar file like this:  
```
wget https://github.com/netease-im/camellia/releases/download/1.2.25/camellia-redis-proxy-1.2.25.tar.gz
tar zxvf camellia-redis-proxy-1.2.25.tar.gz
cd camellia-redis-proxy-1.2.25/
jar -cvf0M camellia-redis-proxy.jar BOOT-INF/ META-INF/ org/
```
then you can run like this:  
```
java -XX:+UseG1GC -Xms4096m -Xmx4096m -server -jar camellia-redis-proxy.jar  
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

* How to run in jdk11/jdk17/jdk21, [jdk17](jdk17.md)  

* How to build/run by spring-boot3/jdk21/docker, [camellia-jdk21-bootstraps](https://github.com/caojiajun/camellia-jdk21-bootstraps)  

* How to build latest snapshot *.tar.gz package: [build-snapshot-package](build-snapshot-package.md)  

* How to run Camellia Redis Proxy server using service: [run-as-service](run-as-services.md)  

