package com.netease.nim.camellia.http.accelerate.proxy.core;

import com.netease.nim.camellia.http.accelerate.proxy.core.console.ProxyConsoleServer;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.DefaultTransportRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.transport.ITransportRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.DefaultUpstreamRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.route.upstream.IUpstreamRouter;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.ITransportServer;
import com.netease.nim.camellia.http.accelerate.proxy.core.transport.tcp.TransportTcpServer;
import com.netease.nim.camellia.http.accelerate.proxy.core.conf.DynamicConf;
import com.netease.nim.camellia.http.accelerate.proxy.core.proxy.IHttpAccelerateProxy;
import com.netease.nim.camellia.http.accelerate.proxy.core.proxy.HttpAccelerateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 *
 *
 * 1、user
 * ==https==>
 * 2、nginx
 * ==http==>
 * 3、IHttpAccelerateProxy --> ITransportRouter --> ITransportClient
 * ==tcp/quic==>
 * 4、ITransportServer --> IUpstreamRouter --> IUpstreamClient
 * ==http==>
 * 5、nginx
 * ==http==>
 * 6、后端
 *
 * <p>
 *
 * 通过http-accelerate-proxy，来连接距离遥远的两个nginx，解决nginx_upstream不支持http1.0/1.1外其他协议导致的问题:
 * 1）连接复用问题
 * 2）丢包问题
 * 3）业务复杂逻辑问题
 *
 * <p>
 *
 * Created by caojiajun on 2023/7/6
 */
public class Bootstrap {

    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    /**
     * 启动
     */
    public void start() {
        try {
            //1、配置
            DynamicConf.init();

            //2、upstream路由
            IUpstreamRouter upstreamRouter = new DefaultUpstreamRouter();
            upstreamRouter.start();

            //3、转发server
            ITransportServer transportServer = new TransportTcpServer(upstreamRouter);
            transportServer.start();

            //4、转发路由
            ITransportRouter transportRouter = new DefaultTransportRouter();
            transportRouter.start();

            //5、proxy
            IHttpAccelerateProxy proxy = new HttpAccelerateProxy(transportRouter);
            proxy.start();

            //6、console
            ProxyConsoleServer consoleServer = new ProxyConsoleServer();
            consoleServer.start(transportRouter, upstreamRouter, transportServer, proxy);

            logger.info("camellia http accelerate proxy start success!");
        } catch (Exception e) {
            logger.error("camellia http accelerate proxy start error!", e);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            System.exit(-1);
        }
    }
}
