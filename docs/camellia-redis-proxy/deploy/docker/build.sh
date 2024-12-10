#!/bin/sh

Verison="0.0.1"
CamelliaVersion="1.3.0"
JDK="21"
if [ 'X'$JDK == 'X8' ];then
        docker build -f Dockerfile-jdk8 --build-arg CamelliaVersion=$CamelliaVersion  -t "48n6e/camellia-redis-proxy:$CamelliaVersion-jdk-$JDK-$Verison" .
elif [ 'X'$JDK == 'X21' ];then
        docker build -f Dockerfile-jdk21 --build-arg CamelliaVersion=$CamelliaVersion  -t "48n6e/camellia-redis-proxy:$CamelliaVersion-jdk-$JDK-$Verison" .
fi 
