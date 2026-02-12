package com.netease.nim.camellia.redis.proxy.cluster.provider;

import com.netease.nim.camellia.redis.base.exception.CamelliaRedisException;
import com.netease.nim.camellia.redis.proxy.cluster.ProxyNode;
import com.netease.nim.camellia.redis.proxy.command.ProxyCurrentNodeInfo;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import com.netease.nim.camellia.tools.utils.SysUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2024/6/19
 */
public abstract class AbstractClusterModeProvider implements ProxyClusterModeProvider {

    private static final Logger logger = LoggerFactory.getLogger(AbstractClusterModeProvider.class);

    /**
     * exeuctor size
     */
    protected static final int executorSize;
    static {
        executorSize = Math.max(4, Math.min(SysUtils.getCpuNum(), 8));
    }

    /**
     * scheduled executor
     */
    protected static final ScheduledExecutorService schedule = Executors.newScheduledThreadPool(executorSize,
            new CamelliaThreadFactory("proxy-cluster-mode-schedule"));

    /**
     * async task executor
     */
    protected static final ThreadPoolExecutor executor = new ThreadPoolExecutor(executorSize, executorSize,
            0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(10000), new CamelliaThreadFactory("proxy-cluster-mode-executor"), new ThreadPoolExecutor.AbortPolicy());


    private final CopyOnWriteArrayList<SlotMapChangeListener> listenerList = new CopyOnWriteArrayList<>();

    private ProxyNode current;
    private Set<ProxyNode> initNodes;
    private final ConcurrentHashMap<ProxyNode, RedisConnectionAddr> addrCache = new ConcurrentHashMap<>();

    /**
     * default constructor
     */
    public AbstractClusterModeProvider() {
    }

    /**
     * addSlotMapChangeListener
     * @param listener SlotMapChangeListener
     */
    @Override
    public final void addSlotMapChangeListener(SlotMapChangeListener listener) {
        listenerList.add(listener);
    }

    /**
     * slotMapChangeNotify
     */
    protected final void slotMapChangeNotify() {
        for (SlotMapChangeListener listener : listenerList) {
            try {
                listener.change();
            } catch (Exception e) {
                logger.error("slotMapChangeNotify callback error", e);
            }
        }
    }

    /**
     * sync for future
     * @param future future
     * @return reply
     */
    protected final Reply sync(CompletableFuture<Reply> future) {
        try {
            return future.get(heartbeatTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.info("sync reply error", e);
            throw new CamelliaRedisException(e);
        }
    }

    /**
     * heartbeatTimeoutSeconds
     * @return heartbeatTimeoutSeconds
     */
    protected final int heartbeatTimeoutSeconds() {
        return ProxyDynamicConf.getInt("proxy.cluster.mode.heartbeat.request.timeout.seconds", 10);
    }

    /**
     * init nodes
     * @param checkEmpty if check empty
     * @return nodes
     */
    protected final Set<ProxyNode> initNodes(boolean checkEmpty) {
        if (initNodes != null) {
            return new HashSet<>(initNodes);
        }
        Set<ProxyNode> initNodes = null;
        try {
            String string = ProxyDynamicConf.getString("cluster.mode.nodes", null);
            if (string == null) {
                throw new IllegalArgumentException("missing 'cluster.mode.nodes' in ProxyDynamicConf");
            }
            String[] split = string.split(",");
            initNodes = new HashSet<>();
            for (String str : split) {
                ProxyNode node = ProxyNode.parseString(str);
                if (node == null) continue;
                initNodes.add(node);
            }
            if (initNodes.isEmpty()) {
                throw new IllegalArgumentException("parse 'cluster.mode.nodes' error");
            }
        } catch (Exception e) {
            if (checkEmpty) {
                throw e;
            }
            if (initNodes == null) {
                initNodes = new HashSet<>();
            }
        }
        logger.info("proxy cluster mode, init nodes = {}", initNodes);
        this.initNodes = initNodes;
        return new HashSet<>(this.initNodes);
    }

    /**
     * current node
     * @return node
     */
    protected final ProxyNode current() {
        if (current != null) return current;
        ProxyNode proxyNode = ProxyCurrentNodeInfo.current();
        if (proxyNode.getPort() == 0 || proxyNode.getCport() == 0) {
            throw new IllegalStateException("redis proxy not start");
        }
        this.current = proxyNode;
        logger.info("current proxy node = {}", current);
        return current;
    }

    /**
     * util method for node to addr
     * @param proxyNode node
     * @return addr
     */
    protected final RedisConnectionAddr toAddr(ProxyNode proxyNode) {
        return CamelliaMapUtils.computeIfAbsent(addrCache, proxyNode,
                node -> new RedisConnectionAddr(node.getHost(), node.getCport(), null, ServerConf.cportPassword()));
    }

}
