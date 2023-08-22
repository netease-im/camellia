
how to build snapshot *.tar.gz package

```shell
git clone https://github.com/netease-im/camellia.git
cd camellia
mvn clean package
cp camellia-redis-proxy/camellia-redis-proxy/bootstrap/target/xxx.jar /yourdict/redis-proxy/xxx.jar
cd /yourdict/redis-proxy
tar xvf xxx.jar
rm -rf xxx.jar
touch start.sh
echo "java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher" > start.sh
chmod +x start.sh
cd ..
tar zcvf redis-proxy.tar.gz ./redis-proxy
```