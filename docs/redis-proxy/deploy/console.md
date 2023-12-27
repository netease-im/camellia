### 优雅上下线
当redis proxy启动的时候，会同时启动一个http服务器console server，默认端口是16379  
我们可以用console server做一些优雅上下线等操作，使用方法是自己实现一个ConsoleService（继承自ConsoleServiceAdaptor）即可，如下所示：

console server包含如下http api:
* /online
  会将一个全局的内存变量status设置成ONLINE状态，会自动处理zk的注册，伪redis-cluster/redis-sentinel模式的节点上线
* /offline
  会将一个全局的内存变量status设置成OFFLINE状态，会自动处理zk的取消注册，伪redis-cluster/redis-sentinel模式的节点下线      
  并且如果此时proxy是idle的，则返回http.code=200，否则会返回http.code=500  
  ps: 当且仅当最后一个命令执行完成已经超过10s了，才会处于idle
* /status
  如果status=ONLINE, 则返回http.code=200,    
  否则返回http.code=500
* /check
  如果服务器端口可达（指的是proxy的服务端口），则返回200，否则返回500
* /reload
  reload动态配置ProxyDynamicConf

