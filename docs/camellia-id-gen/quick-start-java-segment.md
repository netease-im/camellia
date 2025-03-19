
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
public class CamelliaSegmentIdGenTest {

    private static final AtomicLong id = new AtomicLong();

    public static void main(String[] args) throws Exception {
        CamelliaSegmentIdGenConfig config = new CamelliaSegmentIdGenConfig();
        config.setStep(1000);//每次从数据库获取一批id时的批次大小
        config.setTagCount(1000);//服务包括的tag数量，会缓存在本地内存，如果实际tag数超过本配置，会导致本地内存被驱逐，进而丢失部分id段，丢失后会穿透到数据库）
        config.setMaxRetry(10);//当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，本配置表示重试的次数
        config.setRetryIntervalMillis(10);//当并发请求过来时，只会让一次请求穿透到db，其他请求会等待并重试，表示重试间隔
        config.setRegionBits(0);//region比特位，0表示不区分单元
        config.setRegionId(0);//regionId，如果regionBits为0，则regionId必须为0
                
        //设置IdLoader，可以使用数据库实现        
        config.setIdLoader((tag, step) -> {
            IDRange idRange = new IDRange(id.get() + 1, id.addAndGet(step));
            System.out.println("load [" + idRange.getStart() + "-" + idRange.getEnd() + "] in " + Thread.currentThread().getName());
            return idRange;
        });
        CamelliaSegmentIdGen idGen = new CamelliaSegmentIdGen(config);
        int i=2000;
        while (i -- > 0) {
            //可以获取一批
            System.out.println(idGen.genIds("tag", 3));
//            Thread.sleep(1000);
            //也可以获取一个
            System.out.println(idGen.genId("tag"));
//            Thread.sleep(1000);
        }
    }
}

```