package com.netease.nim.camellia.redis.proxy.util;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2021/8/18
 */
public class SocketUtils {

    public static final int PORT_RANGE_MIN = 1024;
    public static final int PORT_RANGE_MAX = 65535;

    public static int findRandomAvailablePort() {
        return findAvailablePort(PORT_RANGE_MIN, PORT_RANGE_MAX);
    }

    public static int findAvailablePort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        int candidatePort;
        int searchCounter = 0;
        do {
            if (searchCounter > portRange) {
                throw new IllegalStateException(String.format(
                        "Could not find an available TCP port in the range [%d, %d] after %d attempts", minPort, maxPort, searchCounter));
            }
            candidatePort = randomPort(minPort, maxPort);
            searchCounter++;
        }
        while (!isPortAvailable(candidatePort));
        return candidatePort;
    }

    private static int randomPort(int minPort, int maxPort) {
        int portRange = maxPort - minPort;
        return minPort + ThreadLocalRandom.current().nextInt(portRange + 1);
    }

    private static boolean isPortAvailable(int port) {
        try {
            ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                    port, 1, InetAddress.getByName("localhost"));
            serverSocket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
