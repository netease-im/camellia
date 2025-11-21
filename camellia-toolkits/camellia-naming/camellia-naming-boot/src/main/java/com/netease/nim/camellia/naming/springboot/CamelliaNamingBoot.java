package com.netease.nim.camellia.naming.springboot;

import com.netease.nim.camellia.naming.core.CamelliaNamingException;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2025/11/20
 */
public class CamelliaNamingBoot {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNamingBoot.class);

    private final CamelliaNamingBootConf bootConf;
    private final ICamelliaNamingService service;

    protected CamelliaNamingBoot(CamelliaNamingBootConf bootConf, ICamelliaNamingService service) {
        this.bootConf = bootConf;
        this.service = service;
    }

    public CamelliaNamingBootConf getConf() {
        return bootConf;
    }

    public ICamelliaNamingService getService() {
        return service;
    }

    public void register() {
        if (bootConf.isRegisterEnable()) {
            String enable = bootConf.getConfig().get("register.pre.check.enable");
            if (Boolean.parseBoolean(enable)) {
                String type = bootConf.getConfig().get("register.pre.check.type");
                int timeout = getInt(bootConf.getConfig(), "register.pre.check.timeout.ms", 1000);
                int maxRetry = getInt(bootConf.getConfig(), "register.pre.check.max.retry", 3);
                String host = bootConf.getConfig().get("register.pre.check.host");
                if (host == null) {
                    host = bootConf.getHost();
                }
                int port = getInt(bootConf.getConfig(), "register.pre.check.port", bootConf.getPort());
                boolean pass = false;
                if ("tcp".equalsIgnoreCase(type)) {
                    for (int i=0; i<maxRetry; i++) {
                        pass = checkTcp(host, port, timeout);
                        if (pass) {
                            break;
                        }
                        sleep((i + 1) * 1000L);
                    }
                } else if ("http".equalsIgnoreCase(type)) {
                    String uri = bootConf.getConfig().get("register.pre.check.http.uri");
                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .readTimeout(timeout, TimeUnit.MILLISECONDS)
                            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                    for (int i=0; i<maxRetry; i++) {
                        pass = checkHttp(okHttpClient, host, port, timeout, uri);
                        if (pass) {
                            break;
                        }
                        sleep((i + 1) * 1000L);
                    }
                } else {
                    pass = true;
                }
                if (!pass) {
                    throw new CamelliaNamingException("register pre check failed");
                }
            }
            service.register();
        }
    }

    public void deregister() {
        if (bootConf.isRegisterEnable()) {
            service.deregister();
        }
    }

    private static boolean checkTcp(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            try {
                socket.connect(new InetSocketAddress(host, port), timeout);
                logger.info("register pre check, check tcp success, host = {}, port = {}, timeout = {}", host, port, timeout);
                return true;
            } catch (Exception e) {
                logger.warn("register pre check, check tcp fail on connect, host = {}, port = {}, timeout = {}, e = {}", host, port, timeout, e.toString());
                return false;
            }
        } catch (Exception e) {
            logger.warn("register pre check, check tcp fail, host = {}, port = {}, timeout = {}, e = {}", host, port, timeout, e.toString());
            return false;
        }
    }

    private static boolean checkHttp(OkHttpClient okHttpClient, String host, int port, int timeout, String uri) {
        String url = "http://" + host + ":" + port;
        if (uri != null) {
            url = url + uri;
        }
        Request.Builder builder = new Request.Builder().get().url(url);
        Request request = builder.build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            boolean pass = response.code() == 200;
            if (pass) {
                logger.info("register pre check, check http success, host = {}, port = {}, timeout = {}, uri = {}", host, port, timeout, uri);
            } else {
                logger.warn("register pre check, check http fail, host = {}, port = {}, timeout = {}, uri = {}, code = {}", host, port, timeout, uri, response.code());
            }
            return pass;
        } catch (Exception e) {
            logger.warn("register pre check, check http fail, host = {}, port = {}, timeout = {}, uri = {}, e = {}", host, port, timeout, uri, e.toString());
            return false;
        }
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static int getInt(Map<String, String> config, String key, int defaultValue) {
        String value = config.get(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

}
