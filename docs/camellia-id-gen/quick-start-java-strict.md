
* 引入maven依赖
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-id-gen-core</artifactId>
    <version>1.3.4</version>
</dependency>
```

* 示例如下
```java
public class CamelliaStrictIdGenTest {

    private static final AtomicLong id = new AtomicLong();

    public static void main(String[] args) {
        CamelliaStrictIdGenConfig config = new CamelliaStrictIdGenConfig();

        config.setCacheKeyPrefix("strict");//redis key的前缀
        config.setDefaultStep(10);//默认每次从db获取的id个数，也是最小的个数
        config.setMaxStep(100);//根据id的消耗速率动态调整每次从db获取id的个数，这个是上限值
        config.setLockExpireMillis(3000);//redis缓存里id耗尽时需要穿透到db重新获取，为了控制并发需要一个分布式锁，这是分布式锁的超时时间
        config.setCacheExpireSeconds(3600*24);//id缓存在redis里，redis key的过期时间，默认1天
        config.setCacheHoldSeconds(10);//缓存里的id如果在短时间内被消耗完，则下次获取id时需要多获取一些，本配置是触发step调整的阈值
        config.setRegionBits(0);//单元id所占的比特位数，0表示不区分单元
        config.setRegionId(0);//regionId，如果regionBits为0，则regionId必须为0
        config.setMaxRetry(1000);//缓存中id耗尽时穿透到db，其他线程等待重试的最大次数
        config.setRetryIntervalMillis(5);//缓存中id耗尽时穿透到db，其他线程等待重试的间隔
        
        //设置redis template
        config.setTemplate(new CamelliaRedisTemplate("redis://@127.0.0.1:6379"));
        //设置IdLoader，可以使用数据库实现
        config.setIdLoader((tag, step) -> {
            IDRange idRange = new IDRange(id.get() + 1, id.addAndGet(step));
            System.out.println("load [" + idRange.getStart() + "," + idRange.getEnd() + "] in " + Thread.currentThread().getName());
            return idRange;
        });

        CamelliaStrictIdGen idGen = new CamelliaStrictIdGen(config);
        int i=2000;
        while (i -- > 0) {
            //可以获取最新的id，但是不使用
            System.out.println("peek, id = " + idGen.peekId("tag"));
            //获取最新的id
            System.out.println("get, id = " + idGen.genId("tag"));
        }
    }
}
```