## Control the number of client connections

* camellia-redis-proxy supports configuring the upper limit of the number of connections on the client side (supports
  the global number of connections, supports every bid/bgroup level by only 1 configuration, and also supports each
  bid/bgroup level)
* camellia-redis-proxy supports configuration to close idle client connections (this function may cause abnormal client
  requests with a small number of requests, configure it carefully)

## Configure the maximum number of client connections

### Principle

* The proxy provides ConnectLimiter to configure the maximum number of client connections. You can customize the
  implementation of the ConnectLimiter interface (for example, dynamically obtain the configuration of the maximum
  number of connections from the business configuration center), and configure the full class name in application.yaml
  or use spring Automatic injection is enabled
* The configuration of the maximum number of connections is obtained by reading the related configuration of
  camellia-redis-proxy.properties, which supports dynamic changes
* When the global maximum number of connections is triggered, the new client connection will be closed directly; when
  the maximum number of connections at the bid/bgroup level is triggered, commands such as AUTH/CLIENT/HELLO will be
  executed on the new connection to bind bid/bgroup returns an error message and closes the connection

camellia-redis-proxy.properties is configured as follows (unlimited by default):

````
#Configure the global maximum connection limit, if it is less than 0, it means no limit
max.client.connect=100000

#Configure every bid/bgroup level maximum connection limit (limit each bid/bgroup), if it is less than 0, it means no limit
# default bid = -1, default bgroup = default
-1.default.max.client.connect=100000 

#Configure the maximum number of connections for a bid/bgroup:
#Indicates the maximum number of connections belonging to bid=1, bgroup=default, if it is less than 0, it means no limit
1.default.max.client.connect=10000
````

## Configure to detect/close idle client connections

### Principle

* The IdleStateHandler provided by netty is used to detect idle client connections. If it is configured to close when
  idle, the idle client connections will be forcibly closed by the server
* When the client connection uses the subscribe/psubscribe command to subscribe, the idle connection will not be closed,
  but the idle connection log will be printed
* This function may cause abnormal client requests with a small number of requests, please use it with caution

### Configuration example

````yaml
server:
  port: 6380
spring:
  application:
    name: camellia-redis-proxy-server

camellia-redis-proxy:
  password: pass123
  netty:
    reader-idle-time-seconds: 600 #Only if all three parameters are greater than or equal to 0, the idle connection detection will be enabled
    writer-idle-time-seconds: 0 #Only if all three parameters are greater than or equal to 0, the idle connection detection will be enabled
    all-idle-time-seconds: 0 #Only if all three parameters are greater than or equal to 0, the idle connection detection will be enabled
  transpond:
    type: local
    local:
      resource: redis://@127.0.0.1:6379
````

The above example means to enable idle connection detection. When a connection does not have any readable data within
600s, it is determined as an idle connection, and a log will be printed at this time. If you need to close the idle
connection, you need to be in camellia-redis-proxy.properties Configuration, configuration supports dynamic changes:

````
#Whether the idle connection is closed when the reader-idle event is triggered, the default is false
##Global configuration
reader.idle.client.connection.force.close.enable=true
##bid/bgroup level configuration, the priority is higher than the global configuration
1.default.reader.idle.client.connection.force.close.enable=true

#Whether the idle connection is closed when the writer-idle event is triggered, the default is false
##Global configuration
writer.idle.client.connection.force.close.enable=true
##bid/bgroup level configuration, the priority is higher than the global configuration
1.default.writer.idle.client.connection.force.close.enable=true

#Whether the idle connection is closed when the all-idle event is triggered, the default is false
all.idle.client.connection.force.close.enable=true
##bid/bgroup level configuration, the priority is higher than the global configuration
1.default.all.idle.client.connection.force.close.enable=true
````

other config
```
## default true, such as SUBSCRIBE
### global level
idle.client.connection.force.close.check.in.subscribe=true
### bid/bgroup level
1.default.idle.client.connection.force.close.check.in.subscribe=true

## default false, such as BLPOP
### global level
idle.client.connection.force.close.check.command.running=false
### bid/bgroup level
1.default.idle.client.connection.force.close.check.command.running=false
```