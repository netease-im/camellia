
## quick start as new java project


### 1) 首先创建一个spring-boot的工程，然后添加以下依赖（最新1.4.0-SNAPSHOT）

```
<dependency>
  <groupId>com.netease.nim</groupId>
  <artifactId>camellia-redis-proxy-spring-boot-starter</artifactId>
  <version>1.4.0-SNAPSHOT</version>
</dependency>
```

### 2) 编写主类Application.java, 如下:
```java
import com.netease.nim.camellia.redis.proxy.springboot.EnableCamelliaRedisProxyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableCamelliaRedisProxyServer
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }
}
```

### 3) 配置camellia-redis-proxy.properties, 如下:
```properties
port=6380
password=pass123
route.conf=redis://@127.0.0.1:6379
```

### 4) 启动Application.java即可.
   你可以使用redis-cli去连接proxy，端口是6380，密码是pass123（如果不需要密码，则在application.yml里去掉这一行即可）
```
➜ ~ ./redis-cli -h 127.0.0.1 -p 6380 -a pass123
127.0.0.1:6380> set k1 v1
OK
127.0.0.1:6380> get k1
"v1"
127.0.0.1:6380> mget k1 k2 k3
1) "v1"
2) (nil)
3) (nil)
```