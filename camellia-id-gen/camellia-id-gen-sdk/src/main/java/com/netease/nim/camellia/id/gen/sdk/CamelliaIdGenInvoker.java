package com.netease.nim.camellia.id.gen.sdk;

import com.netease.nim.camellia.core.discovery.CamelliaDiscovery;
import com.netease.nim.camellia.core.discovery.ServerNode;
import com.netease.nim.camellia.id.gen.common.CamelliaIdGenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created by caojiajun on 2021/9/29
 */
public class CamelliaIdGenInvoker {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaIdGenInvoker.class);

    private final CamelliaDiscovery discovery;
    private List<IdGenServer> all;
    private List<IdGenServer> dynamic;

    private final Object lock = new Object();

    public CamelliaIdGenInvoker(CamelliaIdGenSdkConfig config) {
        discovery = config.getDiscovery();
        if (discovery == null) {

            String url = config.getUrl();
            if (url == null || url.trim().isEmpty()) {
                throw new CamelliaIdGenException("url/discovery is empty");
            }
            this.all = new ArrayList<>(Collections.singletonList(new IdGenServer(url)));
        } else {
            this.all = new ArrayList<>(toIdGenServerList(this.discovery.findAll()));
            if (all.isEmpty()) {
                throw new CamelliaIdGenException("id gen server is empty");
            }
            this.dynamic = new ArrayList<>(all);
            config.getScheduleThreadPool().scheduleAtFixedRate(this::reload, config.getDiscoveryReloadIntervalSeconds(),
                    config.getDiscoveryReloadIntervalSeconds(), TimeUnit.SECONDS);
            this.discovery.setCallback(new CamelliaDiscovery.Callback() {
                @Override
                public void add(ServerNode server) {
                    try {
                        synchronized (lock) {
                            all.add(toIdGenServer(server));
                            dynamic = new ArrayList<>(all);
                        }
                    } catch (Exception e) {
                        logger.error("add error", e);
                    }
                }

                @Override
                public void remove(ServerNode server) {
                    try {
                        synchronized (lock) {
                            ArrayList<IdGenServer> list = new ArrayList<>(all);
                            list.remove(toIdGenServer(server));
                            if (list.isEmpty()) {
                                logger.warn("last id gen server, skip remove");
                                return;
                            }
                            all = list;
                            dynamic = new ArrayList<>(all);
                        }
                    } catch (Exception e) {
                        logger.error("remove error", e);
                    }
                }
            });
        }
    }

    private List<IdGenServer> toIdGenServerList(List<ServerNode> list) {
        if (list == null) {
            return null;
        }
        List<IdGenServer> result = new ArrayList<>(list.size());
        for (ServerNode serverNode : list) {
            result.add(toIdGenServer(serverNode));
        }
        return result;
    }

    private IdGenServer toIdGenServer(ServerNode serverNode) {
        return new IdGenServer("http://" + serverNode.getHost() + ":" + serverNode.getPort());
    }

    private void reload() {
        try {
            List<IdGenServer> all = toIdGenServerList(discovery.findAll());
            if (!all.isEmpty()) {
                synchronized (lock) {
                    this.all = new ArrayList<>(all);
                    this.dynamic = new ArrayList<>(all);
                }
            }
        } catch (Exception e) {
            logger.error("reload error", e);
        }
    }

    public final IdGenServer nextIdGenServer() {
        try {
            if (all.size() == 1) {
                return all.get(0);
            }
            if (dynamic.isEmpty()) {
                dynamic = new ArrayList<>(all);
            }
            int index = ThreadLocalRandom.current().nextInt(dynamic.size());
            return dynamic.get(index);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return all.get(0);
        }
    }

    public final void onError(IdGenServer server) {
        try {
            dynamic.remove(server);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public interface IdGenCall<T> {
        T call(IdGenServer server) throws Exception;
    }

    public <T> T invoke(IdGenCall<T> idGenCall, int maxRetry) {
        CamelliaIdGenException cause = null;
        while (maxRetry-- > 0) {
            IdGenServer idGenServer = nextIdGenServer();
            try {
                return idGenCall.call(idGenServer);
            } catch (CamelliaIdGenException e) {
                cause = e;
                onError(idGenServer);
            } catch (Exception e) {
                cause = new CamelliaIdGenException(e);
                onError(idGenServer);
            }
        }
        if (cause != null) {
            throw cause;
        }
        throw new CamelliaIdGenException("interval error");
    }

}
