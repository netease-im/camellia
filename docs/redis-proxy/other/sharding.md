
## 自定义分片函数
camellia-redis-proxy支持分片，你可以自定义分片函数，分片函数会计算出一个key的哈希值，和分片大小（bucketSize）取余后，得到该key所属的分片。  
默认的分片函数是：
```
com.netease.nim.camellia.core.client.env.DefaultShardingFunc
```  
默认的分片函数是不支持HashTag的，如果你想使用HashTag，可以使用如下两个分片函数：
```
com.netease.nim.camellia.core.client.env.CRC16HashTagShardingFunc
com.netease.nim.camellia.core.client.env.DefaultHashTagShardingFunc
```
此外，你也可以继承com.netease.nim.camellia.core.client.env.AbstractSimpleShardingFunc实现自己想要的分片函数，类似于这样：

```java
package com.netease.nim.camellia.redis.proxy.samples;

import com.netease.nim.camellia.core.client.env.AbstractSimpleShardingFunc;

public class CustomShardingFunc extends AbstractSimpleShardingFunc {
    
    @Override
    public int shardingCode(byte[] key) {
        if (key == null) return 0;
        if (key.length == 0) return 0;
        int h = 0;
        for (byte d : key) {
            h = 31 * h + d;
        }
        return (h < 0) ? -h : h;
    }
}
```  
然后在application.yml配置即可，类似于这样：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: local
    local:
      type: complex
      json-file: resource-table.json
    redis-conf:
      sharding-func: com.xxx.CustomShardingFunc
```
