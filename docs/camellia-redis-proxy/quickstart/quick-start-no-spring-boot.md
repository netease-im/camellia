
## 快速开始（不基于spring-boot-starter）

首先，引入依赖：
```
<dependency>
    <groupId>com.netease.nim</groupId>
    <artifactId>camellia-redis-proxy-core</artifactId>
    <version>1.2.28</version>
</dependency>
```

其次，如下：
```java
/**
 * 不使用spring-boot-starter，手工启动一个proxy的方法
 * Created by caojiajun on 2021/8/3
 */
public class SimpleTest {

    public static void main(String[] args) {
        startSimple();
//        startDashboard();
//        startCustomProxyRouteUpdater();
    }

    private static void startSimple() {
        //设置相关参数
        CamelliaRedisProxyStarter.updatePort(6380);//设置proxy的端口
        CamelliaRedisProxyStarter.updatePassword("pass123");//设置proxy的密码
        CamelliaRedisProxyStarter.updateRouteConf("redis://@127.0.0.1:6379");//可以设置单个地址，也可以设置一个json去配置双写/分片等

        CamelliaServerProperties serverProperties = CamelliaRedisProxyStarter.getServerProperties();
        serverProperties.setMonitorEnable(true);//开启监控
        List<String> plugins = serverProperties.getPlugins();
        //增加plugin
        plugins.add(BuildInProxyPluginEnum.MONITOR_PLUGIN.getAlias());
        plugins.add(BuildInProxyPluginEnum.BIG_KEY_PLUGIN.getAlias());
        plugins.add(BuildInProxyPluginEnum.HOT_KEY_PLUGIN.getAlias());
        //其他参数设置....

        //启动
        CamelliaRedisProxyStarter.start();
    }

    private static void startDashboard() {
        CamelliaRedisProxyStarter.updatePort(6380);//设置proxy的端口
        CamelliaRedisProxyStarter.updatePassword("pass123");//设置proxy的密码

        CamelliaTranspondProperties transpondProperties = CamelliaRedisProxyStarter.getTranspondProperties();
        transpondProperties.setType(CamelliaTranspondProperties.Type.REMOTE);
        CamelliaTranspondProperties.RemoteProperties remoteProperties = new CamelliaTranspondProperties.RemoteProperties();
        remoteProperties.setUrl("https://127.0.0.1:8080");
        remoteProperties.setBid(1);
        remoteProperties.setBgroup("default");
        transpondProperties.setRemote(remoteProperties);

        CamelliaServerProperties serverProperties = CamelliaRedisProxyStarter.getServerProperties();
        serverProperties.setMonitorEnable(true);//开启监控
        List<String> plugins = serverProperties.getPlugins();
        //增加plugin
        plugins.add(BuildInProxyPluginEnum.MONITOR_PLUGIN.getAlias());
        plugins.add(BuildInProxyPluginEnum.BIG_KEY_PLUGIN.getAlias());
        plugins.add(BuildInProxyPluginEnum.HOT_KEY_PLUGIN.getAlias());
        //其他参数设置....

        //启动
        CamelliaRedisProxyStarter.start();
    }

    private static void startCustomProxyRouteUpdater() {
        CamelliaRedisProxyStarter.updatePort(6380);//设置proxy的端口
        CamelliaRedisProxyStarter.updatePassword("pass123");//设置proxy的密码

        CamelliaTranspondProperties transpondProperties = CamelliaRedisProxyStarter.getTranspondProperties();
        transpondProperties.setType(CamelliaTranspondProperties.Type.CUSTOM);
        CamelliaTranspondProperties.CustomProperties customProperties = new CamelliaTranspondProperties.CustomProperties();
        customProperties.setProxyRouteConfUpdaterClassName(CustomProxyRouteConfUpdater.class.getName());
        customProperties.setBid(1);
        customProperties.setBgroup("default");
        transpondProperties.setCustom(customProperties);

        CamelliaServerProperties serverProperties = CamelliaRedisProxyStarter.getServerProperties();
        serverProperties.setMonitorEnable(true);//开启监控
        List<String> plugins = serverProperties.getPlugins();
        //增加plugin
        plugins.add(BuildInProxyPluginEnum.MONITOR_PLUGIN.getAlias());
        plugins.add(BuildInProxyPluginEnum.BIG_KEY_PLUGIN.getAlias());
        plugins.add(BuildInProxyPluginEnum.HOT_KEY_PLUGIN.getAlias());
        //其他参数设置....

        //启动
        CamelliaRedisProxyStarter.start();
    }
}

```
