
## 使用camellia-dashboard管理动态路由
你可以将路由信息托管到远程的camellia-dashboard（见[camellia-dashboard](/docs/dashboard/dashboard.md)）  
camellia-dashboard是一个web服务，proxy会定期去检查camellia-dashboard里的配置是否有变更，如果有，则会更新proxy的路由  
以下是一个配置示例：
```yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  transpond:
    type: remote
    remote:
      url: http://127.0.0.1:8080 #camellia-dashbaord的地址
      check-interval-millis: 5000 #到camellia-dashbaord的轮询周期
      dynamic: true #表示支持多组配置，默认就是true
      bid: 1 #默认的bid，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
      bgroup: default #默认的bgroup，当客户端请求时没有声明自己的bid和bgroup时使用的bgroup，可以缺省，若缺省则不带bid/bgroup的请求会被拒绝
```
上面的配置表示proxy的路由配置会从camellia-dashboard获取，获取的是bid=1以及bgroup=default的那份配置   
此外，proxy会定时检查camellia-dashboard上的配置是否更新了，若更新了，则会更新本地配置，默认检查的间隔是5s  
特别的，当需要支持多租户时，设置dynamic=true即可  