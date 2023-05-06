
# camellia-hot-key

## 简介

* 一个热key探测和缓存的工具
* 包括SDK、Server两个模块

## 基本架构

<img src="hot-key.png" width="90%" height="90%">  

## SDK部分

包括两个SDK：  
* CamelliaHotKeySdk
* CamelliaHotKeyCacheSdk（基于CamelliaHotKeySdk封装）

### CamelliaHotKeySdk

* CamelliaHotKeySdk用于推送key相关的事件给到hot-key-server做统计
* 此外sdk侧可以添加listener用于感知事件（可选），hot-key-server也可以配置是否进行事件通知，如果仅仅用于热key探测，建议关闭通知

```java
public interface ICamelliaHotKeySdk {

    /**
     * 推送一个key的动作给server
     * @param key key
     * @param keyAction 动作
     */
    void push(String key, KeyAction keyAction);

    /**
     * 增加一个key的事件监听
     * @param listener 监听器
     */
    void addListener(CamelliaHotKeyListener listener);

}

public enum KeyAction {

    QUERY,
    UPDATE,
    DELETE,
    ;
}

public interface CamelliaHotKeyListener {

    /**
     * 热key事件监听回调方法
     * @param event 事件
     */
    void onHotKeyEvent(HotKeyEvent event);
}

public class HotKeyEvent {

    private final HotKeyEventType eventType;//事件类型
    private final String key;

    public HotKeyEvent(HotKeyEventType eventType, String key) {
        this.eventType = eventType;
        this.key = key;
    }

    public HotKeyEventType getEventType() {
        return eventType;
    }

    public String getKey() {
        return key;
    }
}

public enum HotKeyEventType {

    HOT_KEY_DISCOVERY,
    KEY_UPDATE,
    KEY_DELETE,
    ;
}
```


### CamelliaHotKeyCacheSdk

* 基于CamelliaHotKeySdk，可以进一步封装一个CamelliaHotKeyCacheSdk，该sdk支持热key缓存，且热key会在被更新或者删除后广播给其他sdk
* 使用CamelliaHotKeyCacheSdk时，hot-key-server需要开启事件通知，从而能及时感知到热key的变化

```java
public interface ICamelliaHotKeyCacheSdk {

    /**
     * 获取一个key的value
     * 如果是热key，则会优先获取本地缓存中的内容，如果获取不到则会走loader穿透（穿透时会加并发控制）
     * 如果不是热key，则通过loader获取到value后返回
     *
     * 如果key有更新了，hot-key-server会广播给所有sdk去更新本地缓存，从而保证缓存值的时效性
     *
     * @param key key
     * @param loader value loader
     * @return value
     */
    <T> T getValue(String key, ValueLoader<T> loader);

    /**
     * key的value被更新了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param key key
     */
    void keyUpdate(String key);

    /**
     * key的value被删除了，需要调用本方法给hot-key-server，进而广播给所有人
     * @param key key
     */
    void keyDelete(String key);
}

public interface ValueLoader<T> {

    /**
     * load 一个value
     * @param key key
     * @return value
     */
    T load(String key);
}
```

## Server部分

xxx