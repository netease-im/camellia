
## 严格递增的id生成算法（基于redis+ntp时间戳）
### 特性
* id全局严格递增
* 支持根据tag维护多条序列，彼此独立

### 原理
* 使用当前时间戳（秒）作为前缀，后面补seq，假设是11位，则单个tag最多每秒生成2048个id，也就是最大支持2048/秒
* 为了处理ntp不同步的问题，需要在redis中存储当前的时间戳，每次生成时判断redis中的时间戳和本机时间戳是否匹配
* 如果本机时间戳>redis时间戳，则使用本机时间戳覆盖
* 如果本机时间戳<=redis时间戳，则使用redis时间戳
* 为了提高利用率，可以规定一个起始时间戳
* seq取11位的情况下（最大2048/秒），用到52位（js的最大精度），可以使用69730年
* seq取12位的情况下（最大4096/秒），用到52位（js的最大精度），可以使用34865年
* seq取13位的情况下（最大8192/秒），用到52位（js的最大精度），可以使用17432年
* 相比CamelliaStrictIdGen的风险点在于，如果redis中的key丢失了（主从切换或者驱逐等），可能导致id在短时间内（1s内）产生重复或者回溯

### 用法(直接使用)
引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-core</artifactId>
    <version>1.2.28</version>
</dependency>
```
示例如下：
```java
public class CamelliaStrictIdGen2Test {

    public static void main(String[] args) {
        CamelliaStrictIdGen2Config config = new CamelliaStrictIdGen2Config();
        config.setSeqBits(11);//seq占11位，单个序列最多2048/秒
        config.setRedisTemplate(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        CamelliaStrictIdGen2 idGen = new CamelliaStrictIdGen2(config);
        int count = 30000;
        long start = System.currentTimeMillis();
        long lastId = 0;
        for (int i=0; i<count; i++) {
            long id = idGen.genId("test");
            if (i % 3000 == 0) {
                System.out.println("id=" + id);
            }
            if (id <= lastId) {
                System.out.println("error, id=" + id + ", lastId=" + lastId);
                System.out.println(Long.toBinaryString(id));
                System.out.println(Long.toBinaryString(lastId));
                System.exit(-1);
                return;
            }
            lastId = id;
//            System.out.println(id);
//            System.out.println(Long.toBinaryString(id));
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (count / ((end - start)/1000.0)));
    }
}


```

### 示例源码
[源码](/camellia-samples/camellia-id-gen-strict-samples)

