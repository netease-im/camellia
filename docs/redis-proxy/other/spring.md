
## 如何使用spring管理bean生成

* proxy的很多自定义入口（不管是plugin还是callback）都需要配置全类名  
* proxy会优先从spring获取该类，如果获取不到，会调用全类名的无参构造方法来创建相关对象