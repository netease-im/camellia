#!/bin/sh

Verison="0.0.1"
CamelliaVersion="1.2.28"
JDK="22"
if [ 'X'$JDK == 'X8' ];then
        docker build -f Dockerfile-jdk8 --build-arg CamelliaVersion=$CamelliaVersion  -t "48n6e/camellia-redis-proxy:$CamelliaVersion-jdk-$JDK-$Verison" .
elif [ 'X'$JDK == 'X22' ];then
        docker build -f Dockerfile-jdk22 --build-arg CamelliaVersion=$CamelliaVersion  -t "48n6e/camellia-redis-proxy:$CamelliaVersion-jdk-$JDK-$Verison" .
fi 
