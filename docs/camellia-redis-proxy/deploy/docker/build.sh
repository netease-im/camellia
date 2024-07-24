#!/bin/sh

Verison="1.2.27-0.0.1"
CamelliaVersion="1.2.27"
docker build -f Dockerfile --build-arg CamelliaVersion=$CamelliaVersion  -t "48n6e/camellia-redis-proxy:$Verison" .