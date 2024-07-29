

How to run Camellia Redis Proxy server using service - 如何使用服务运行Camellia Redis代理服务器。

```
groupadd camelliarp
useradd camelliarp -g camelliarp
wget https://github.com/netease-im/camellia/releases/download/1.2.28/camellia-redis-proxy-1.2.28.tar.gz
tar zxvf camellia-redis-proxy-1.2.28.tar.gz
mv camellia-redis-proxy-1.2.28 /home/camelliarp/crp
chown -R camelliarp:camelliarp /home/camelliarp/crp
```

vi /etc/systemd/system/camellia_rp.service
```
[Unit]
Description=Camellia Redis Proxy service.
After=network-online.target
Wants=network-online.target

[Service]
SuccessExitStatus=143
User=camelliarp
Group=camelliarp
Type=simple
WorkingDirectory=/home/camelliarp/crp
NoNewPrivileges=yes
NonBlocking=yes
PrivateDevices=true
PrivateTmp=true
ProtectSystem=full
RuntimeDirectory=camelliarp
RuntimeDirectoryMode=0755
ExecStart=/usr/bin/java -XX:+UseG1GC -Xms4096m -Xmx4096m -server org.springframework.boot.loader.JarLauncher
ExecStop=/bin/kill -15 $MAINPID
KillMode=mixed

[Install]
WantedBy=multi-user.target
```

Activate service
```
systemctl enable camellia_rp.service --now
```
