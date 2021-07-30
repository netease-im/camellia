
### 动态配置（自定义）
如果你想让你的CamelliaRedisTemplate可以动态变更ResourceTable配置，但是又不想引入camellia-dashboard，也不想用独立配置文件的方式，camellia还额外提供了一种自定义的方式，如下：

```java
public class TestRedisTemplateResourceTableUpdater {

    private static class CustomRedisTemplateResourceTableUpdater extends RedisTemplateResourceTableUpdater {

        public CustomRedisTemplateResourceTableUpdater() {
            Executors.newSingleThreadScheduledExecutor()
                    .scheduleAtFixedRate(this::checkUpdate, 1, 1, TimeUnit.SECONDS);
        }

        @Override
        public ResourceTable getResourceTable() {
            //用于初始化ResourceTable
            return ResourceTableUtil.simpleTable(new Resource("redis://@127.0.0.1:6379"));
        }

        private void checkUpdate() {
            //从你的配置中心获取配置，或者监听配置的变更
            ResourceTable resourceTable = ResourceTableUtil.simpleTable(new Resource("redis://@127.0.0.1:6380"));
            //如果配置发生了变更，则回调告诉CamelliaRedisTemplate有更新
            invokeUpdateResourceTable(resourceTable);
        }
    }

    public static void main(String[] args) {
        CamelliaRedisTemplate template = new CamelliaRedisTemplate(CamelliaRedisEnv.defaultRedisEnv(), new CustomRedisTemplateResourceTableUpdater());
        System.out.println(template.get("k1"));
    }
}

```

你需要自定义一个RedisTemplateResourceTableUpdater，实现getResourceTable方法，并且可以对接到你自己配置中心    
如果发现配置有变更，则调用invokeUpdateResourceTable回调方法通知CamelliaRedisTemplate更新配置      