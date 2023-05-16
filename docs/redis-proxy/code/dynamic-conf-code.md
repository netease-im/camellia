## 动态配置实现原理ProxyDynamicConf

### 初始化 init

![image-20230516155245412](./dynamic-conf-imgs/dynamic-conf-1.png)

当进行初始化的时候，首先会从`CamelliaServerProperties`中的conf读取配置，也就是yml文件配置中的`camellia-redis-proxy:config`键值对。之后`reload`进行第一次初始化，并且根据配置项`dynamic.conf.reload.interval.seconds`开启一个定时刷新配置的后台线程。

### 加载配置reload

![image-20230516155749474](./dynamic-conf-imgs/dynamic-conf-2.png)

CAS是因为调用reload的地方不止一个，所以同一时间只执行一次就行。当新旧文件不一致的时候，才会调用相应的`ProxyDynamicConfLoader`的load方法，比如本地配置文件加载器`FileBasedProxyDynamicConfLoader`。加载完配置文件之后，设置一下`ProxyDynamicConf.conf `，清除配置缓存，触发缓存清除回调函数。



回调函数大概是这些：

![image-20230516160804686](./dynamic-conf-imgs/dynamic-conf-3.png)

### 本地文件加载FileBasedProxyDynamicConfLoader

![image-20230516161327790](./dynamic-conf-imgs/dynamic-conf-4.png)

主要注意文件配置的优先级。