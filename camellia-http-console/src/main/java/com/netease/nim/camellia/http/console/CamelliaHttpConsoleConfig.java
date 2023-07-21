package com.netease.nim.camellia.http.console;

import com.netease.nim.camellia.tools.executor.CamelliaHashedExecutor;

/**
 * Created by caojiajun on 2023/6/30
 */
public class CamelliaHttpConsoleConfig {

    private static final CamelliaHashedExecutor defaultExecutor = new CamelliaHashedExecutor("console-executor", Runtime.getRuntime().availableProcessors(), 10240);

    private String host = "0.0.0.0";
    private int port;
    private int bossThread = 1;
    private int workThread = Runtime.getRuntime().availableProcessors();
    private IConsoleService consoleService;

    private CamelliaHashedExecutor executor = defaultExecutor;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBossThread() {
        return bossThread;
    }

    public void setBossThread(int bossThread) {
        this.bossThread = bossThread;
    }

    public int getWorkThread() {
        return workThread;
    }

    public void setWorkThread(int workThread) {
        this.workThread = workThread;
    }

    public IConsoleService getConsoleService() {
        return consoleService;
    }

    public void setConsoleService(IConsoleService consoleService) {
        this.consoleService = consoleService;
    }

    public CamelliaHashedExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(CamelliaHashedExecutor executor) {
        this.executor = executor;
    }
}
