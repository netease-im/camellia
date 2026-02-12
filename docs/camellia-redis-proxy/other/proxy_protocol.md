
## proxy_protocol
* if you use haproxy or nginx as L4-layer-proxy of camellia-redis-proxy
* you can use proxy_protocol for camellia-redis-proxy to get the real ip address of the client
* if proxy-protocol-enable=true, you can not access redis-proxy direct by redis resp2 protocol anymore


```properties
proxy.protocol.enable=true
```

* if you enable proxy-protocol on some port, not all ports, you can use proxy-protocol-ports to specify the ports

```properties
proxy.protocol.ports=6380,6381
```

if missing `proxy-protocol-ports` config, means tls-port/not-tls-port both enable proxy-protocol