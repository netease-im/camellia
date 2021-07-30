
### 动态配置（自定义）
如果你想让你的CamelliaRedisTemplate可以动态变更ResourceTable配置，但是又不想引入camellia-dashboard，也不想用独立配置文件的方式，camellia还额外提供了一种自定义的方式，如下：

首先你要实现一个自定义的RedisTemplateResourceTableUpdater，如下：
```java
public class CustomRedisTemplateResourceTableUpdater extends RedisTemplateResourceTableUpdater {

    public CustomRedisTemplateResourceTableUpdater() {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::checkUpdate, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public ResourceTable getResourceTable() {
        //用于初始化ResourceTable
        return ResourceTableUtil.simpleTable(new Resource("redis://@127.0.0.1:6379"));
    }

    private void checkUpdate() {
        //从你的配置中心获取配置，或者监听配置的变更
        ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis://pass123@127.0.0.1:6380"));
        //如果配置发生了变更，则回调告诉CamelliaRedisTemplate有更新
        invokeUpdateResourceTable(resourceTable);
    }
}
```

其次，使用自定义的RedisTemplateResourceTableUpdater去初始化CamelliaRedisTemplate即可
```java
public class TestRedisTemplateResourceTableUpdater {

    public static void main(String[] args) throws InterruptedException {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(CamelliaRedisEnv.defaultRedisEnv(), new CustomRedisTemplateResourceTableUpdater());
        while (true) {
            System.out.println(template.get("k1"));
            Thread.sleep(1000);
        }
    }
}

```

上面的例子中，CamelliaRedisTemplate初始化时访问的是redis://@127.0.0.1:6379，5s之后访问的是redis://pass123@127.0.0.1:6380