## lb模式

通常来说，有四种方式来部署多实例的架构：
* 前置四层代理(如lvs/阿里slb), 如下:   
  <img src="redis-proxy-lb.png" width="60%" height="60%">

此时，你可以像调用单点redis一样调用redis proxy