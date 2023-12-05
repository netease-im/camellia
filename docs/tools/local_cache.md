
# CamelliaLocalCache

## 简介
* 底层基于ConcurrentLinkedHashMap，是一个LRU的缓存   
* 支持设置ttl（底层不会删除，但是不能get到）
* 支持缓存null值

## maven
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-tools</artifactId>
    <version>1.2.20</version>
</dependency>
```

## 示例
```java
public class LocalCacheSamples {
    public static void main(String[] args) {
        CamelliaLocalCache cache = new CamelliaLocalCache();
        //put
        cache.put("tag", "key1", new User("jim"), 10);//expireSeconds < 0 means no ttl
        //get
        User user = cache.get("tag", "key1", User.class);
        System.out.println(JSONObject.toJSONString(user));
        //put null
        cache.put("tag", "key2", null, 10);
        //get null
        CamelliaLocalCache.ValueWrapper valueWrapper = cache.get("tag", "key2");
        User u = (User)valueWrapper.get();
        System.out.println(u);
        //get missing
        CamelliaLocalCache.ValueWrapper valueWrapper2 = cache.get("tag", "key3");
        System.out.println(valueWrapper2 == null);
        //del
        cache.evict("tag", "key3");
        //clear all
        cache.clear();
    }

    private static class User {
        private String name;

        public User(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

```