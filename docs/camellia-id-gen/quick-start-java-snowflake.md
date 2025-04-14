
* 引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-core</artifactId>
    <version>1.3.5</version>
</dependency>
```

* 示例如下
```java
public class CamelliaSnowflakeIdGenTest {

    public static void main(String[] args) {
        CamelliaSnowflakeConfig config = new CamelliaSnowflakeConfig();
        config.setRegionBits(0);//单元id所占的比特位数，0表示不区分单元
        config.setRegionId(0);//regionId，如果regionBits为0，则regionId必须为0
        config.setWorkerIdBits(10);//workerId所占的比特位数
        config.setSequenceBits(12);//序列号所占比特位数
        //使用redis生成workerId
        config.setWorkerIdGen(new RedisWorkerIdGen(new CamelliaRedisTemplate("redis://@127.0.0.1:6379")));

        CamelliaSnowflakeIdGen idGen = new CamelliaSnowflakeIdGen(config);

        int i=2000;
        while (i -- > 0) {
            long id = idGen.genId();//生成id
            System.out.println(id);
            System.out.println(Long.toBinaryString(id));
            System.out.println(Long.toBinaryString(id).length());
            long ts = idGen.decodeTs(id);//从id中解析出时间戳
            System.out.println(ts);
            System.out.println(new Date(ts));
        }

        long target = 1000*10000;
        int j = 0;
        long start = System.currentTimeMillis();
        while (true) {
            idGen.genId();
            j++;
            if (j % 100000 == 0) {
                System.out.println("i=" + j);
            }
            if (j >= target) break;
        }
        long end = System.currentTimeMillis();
        System.out.println("QPS=" + (target / ((end - start)/1000.0)));
        //###idea里直接运行的简单测试结果：
        //QPS=4061738.424045491
    }
}

```