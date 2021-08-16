
### 动态配置（使用独立配置文件）
如果你不想引入camellia-dashboard，但是又想CamelliaRedisTemplate可以动态变更ResourceTable，那么你可以将配置托管到某个文件，然后CamelliaRedisTemplate会定期检查文件是否有更新，如下：  
```java
public class TestJsonFile {

    public static void test() {
        String fileName = "resource-table.json";//文件可以是json，也可以是单个的redis地址
//        String fileName = "simple.conf";
        URL resource = TestJsonFile.class.getClassLoader().getResource(fileName);
        if (resource == null) {
            System.out.println(fileName + " not exists");
            return;
        }
        ReloadableLocalFileCamelliaApi localFileCamelliaApi = new ReloadableLocalFileCamelliaApi(resource.getPath());

        CamelliaRedisEnv redisEnv = CamelliaRedisEnv.defaultRedisEnv();
        long checkIntervalMillis = 5000;//检查文件是否产生变更的检查周期，单位ms
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(redisEnv, localFileCamelliaApi, checkIntervalMillis);

        String k1 = template.get("k1");
        System.out.println(k1);
    }

    public static void main(String[] args) {
        test();
    }
}


```
上面的例子中CamelliaRedisTemplate引用了classpath下一个叫resource-table.json的文件中的ResourceTable配置，并且当文件发生变更的时候，CamelliaRedisTemplate会在5000ms之内感知到并自动reloadResourceTable配置  
注：resource-table.json配置里可以只填一个地址（不是一个json），如：
```
redis://@127.0.0.1:6379
```
ReloadableLocalFileCamelliaApi会自动识别这种情况，此时ResourceTable配置就是一个没有分片也没有读写分离的简单配置