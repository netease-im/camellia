package com.netease.nim.camellia.naming.nacos;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.listener.NamingChangeEvent;
import com.netease.nim.camellia.naming.core.ICamelliaNamingCallback;
import com.netease.nim.camellia.naming.core.CamelliaNamingException;
import com.netease.nim.camellia.naming.core.ICamelliaNamingService;
import com.netease.nim.camellia.naming.core.InstanceInfo;
import com.netease.nim.camellia.tools.executor.CamelliaThreadFactory;
import com.netease.nim.camellia.tools.utils.CamelliaMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Created by caojiajun on 2025/11/18
 */
public class CamelliaNacosNamingService implements ICamelliaNamingService {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaNacosNamingService.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new CamelliaThreadFactory("camellia-naming-nacos-scheduler"));

    private final NamingService namingService;
    private final String serviceName;
    private final InstanceInfo instanceInfo;
    private final Instance instance;
    private boolean registerOk;
    private final Map<String, Map<String, CallbackItem>> callbackMap = new ConcurrentHashMap<>();
    private final Map<String, Set<InstanceInfo>> instanceMap = new ConcurrentHashMap<>();
    private final Map<String, Long> timeMap = new ConcurrentHashMap<>();

    public CamelliaNacosNamingService(NamingService namingService, String serviceName, InstanceInfo instanceInfo) {
        this.namingService = namingService;
        this.serviceName = serviceName;
        this.instanceInfo = instanceInfo;
        this.instance = new Instance();
        this.instance.setServiceName(serviceName);
        this.instance.setIp(instanceInfo.getHost());
        this.instance.setPort(instanceInfo.getPort());
        int initialDelay = ThreadLocalRandom.current().nextInt(60);
        scheduler.scheduleAtFixedRate(this::reloadAll, initialDelay, 60, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (registerOk) {
                    deregister();
                }
            } catch (Exception e) {
                logger.error("deregister error, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo), e);
            }
        }));
    }

    @Override
    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    @Override
    public List<InstanceInfo> getInstanceInfoList(String serviceName) {
        try {
            Set<InstanceInfo> list = instanceMap.get(serviceName);
            if (list != null) {
                Long lastRefreshTime = timeMap.get(serviceName);
                if (lastRefreshTime != null && System.currentTimeMillis() - lastRefreshTime < 1000) {
                    return new ArrayList<>(list);
                }
            }
            return new ArrayList<>(reload(serviceName));
        } catch (Exception e) {
            logger.error("getInstanceList error, serviceName = {}", serviceName, e);
            throw new CamelliaNamingException(e);
        }
    }

    @Override
    public String subscribe(String serviceName, ICamelliaNamingCallback callback) {
        try {
            //
            reload(serviceName);
            //
            Map<String, CallbackItem> subMap = CamelliaMapUtils.computeIfAbsent(callbackMap, serviceName, k -> new ConcurrentHashMap<>());
            String id = UUID.randomUUID().toString().replace("-", "");
            EventListener eventListener = event -> {
                try {
                    boolean change = false;
                    synchronized (instanceMap) {
                        if (event instanceof NamingChangeEvent) {
                            if (((NamingChangeEvent) event).isAdded()) {
                                Set<InstanceInfo> set = CamelliaMapUtils.computeIfAbsent(instanceMap, serviceName, k -> new HashSet<>());
                                Set<InstanceInfo> added = toSet(((NamingChangeEvent) event).getAddedInstances());
                                set.addAll(added);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("instance info added, list = {}", JSONObject.toJSON(added));
                                }
                                try {
                                    List<InstanceInfo> list = new ArrayList<>(added);
                                    Collections.shuffle(list);
                                    callback.add(list);
                                } catch (Exception e) {
                                    logger.error("add callback error, serviceName = {}", serviceName, e);
                                }
                                change = true;
                            }
                            if (((NamingChangeEvent) event).isRemoved()) {
                                Set<InstanceInfo> set = CamelliaMapUtils.computeIfAbsent(instanceMap, serviceName, k -> new HashSet<>());
                                Set<InstanceInfo> removed = toSet(((NamingChangeEvent) event).getRemovedInstances());
                                set.removeAll(removed);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("instance info removed, list = {}", JSONObject.toJSON(removed));
                                }
                                try {
                                    List<InstanceInfo> list = new ArrayList<>(removed);
                                    Collections.shuffle(list);
                                    callback.remove(list);
                                } catch (Exception e) {
                                    logger.error("remove callback error, serviceName = {}", serviceName, e);
                                }
                                change = true;
                            }
                        }
                    }
                    if (!change) {
                        Long lastRefreshTime = timeMap.get(serviceName);
                        if (lastRefreshTime != null && System.currentTimeMillis() - lastRefreshTime < 1000) {
                            reloadAndNotify(serviceName);
                        }
                    }
                } catch (Exception e) {
                    logger.error("nacos eventListener error, serviceName = {}", serviceName, e);
                }
            };
            subMap.put(id, new CallbackItem(callback, eventListener));
            namingService.subscribe(serviceName, eventListener);
            return id;
        } catch (Exception e) {
            logger.error("subscribe error, serviceName = {}", serviceName, e);
            throw new CamelliaNamingException(e);
        }
    }

    @Override
    public void unsubscribe(String serviceName, String id) {
        try {
            Map<String, CallbackItem> subMap = callbackMap.get(serviceName);
            if (subMap == null) {
                return;
            }
            CallbackItem callbackItem = subMap.remove(id);
            if (callbackItem == null) {
                return;
            }
            namingService.unsubscribe(serviceName, callbackItem.eventListener);
        } catch (Exception e) {
            logger.error("unsubscribe error, serviceName = {}", serviceName, e);
            throw new CamelliaNamingException(e);
        }
    }

    @Override
    public void register() {
        try {
            namingService.registerInstance(serviceName, instance);
            registerOk = true;
            logger.info("register to nacos, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo));
        } catch (Exception e) {
            logger.error("register error, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSON(instanceInfo), e);
            throw new CamelliaNamingException(e);
        }
    }

    @Override
    public void deregister() {
        try {
            namingService.deregisterInstance(serviceName, instance);
            registerOk = false;
            logger.info("deregister to nacos, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSONString(instanceInfo));
        } catch (Exception e) {
            logger.error("deregister error, serviceName = {}, instanceInfo = {}", serviceName, JSONObject.toJSON(instanceInfo), e);
            throw new CamelliaNamingException(e);
        }
    }

    private Set<InstanceInfo> toSet(List<Instance> allInstances) {
        if (allInstances == null) {
            return new HashSet<>();
        }
        Set<InstanceInfo> set = new HashSet<>();
        for (Instance instance : allInstances) {
            if (!instance.isHealthy()) {
                continue;
            }
            String ip = instance.getIp();
            int port = instance.getPort();
            InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.setHost(ip);
            instanceInfo.setPort(port);
            set.add(instanceInfo);
        }
        return set;
    }

    private void reloadAll() {
        try {
            Set<String> serviceNameSet = new HashSet<>(instanceMap.keySet());
            for (String serviceName : serviceNameSet) {
                try {
                    reloadAndNotify(serviceName);
                } catch (Exception e) {
                    logger.error("reload error, serviceName = {}", serviceName, e);
                }
            }
        } catch (Exception e) {
            logger.error("reloadAll error", e);
        }
    }

    private void reloadAndNotify(String serviceName) throws NacosException {
        Set<InstanceInfo> oldSet = new HashSet<>(instanceMap.get(serviceName));
        Set<InstanceInfo> newSet = reload(serviceName);
        Set<InstanceInfo> added = new HashSet<>(newSet);
        added.removeAll(oldSet);
        Set<InstanceInfo> removed = new HashSet<>(oldSet);
        removed.removeAll(newSet);

        if (added.isEmpty() && removed.isEmpty()) {
            return;
        }

        Map<String, CallbackItem> callbackItemMap = callbackMap.get(serviceName);
        if (callbackItemMap != null) {
            for (Map.Entry<String, CallbackItem> entry : callbackItemMap.entrySet()) {
                ICamelliaNamingCallback callback = entry.getValue().callback;
                if (!added.isEmpty()) {
                    try {
                        List<InstanceInfo> list = new ArrayList<>(added);
                        Collections.shuffle(list);
                        callback.add(list);
                    } catch (Exception e) {
                        logger.error("add callback error, serviceName = {}", serviceName, e);
                    }
                }
                if (!removed.isEmpty()) {
                    try {
                        List<InstanceInfo> list = new ArrayList<>(removed);
                        Collections.shuffle(list);
                        callback.remove(list);
                    } catch (Exception e) {
                        logger.error("remove callback error, serviceName = {}", serviceName, e);
                    }
                }
            }
        }
    }

    private Set<InstanceInfo> reload(String serviceName) throws NacosException {
        List<Instance> allInstances = namingService.getAllInstances(serviceName);
        Set<InstanceInfo> set = toSet(allInstances);
        instanceMap.put(serviceName, new HashSet<>(set));
        timeMap.put(serviceName, System.currentTimeMillis());
        return set;
    }

    private static class CallbackItem {
        private final ICamelliaNamingCallback callback;
        private final EventListener eventListener;

        public CallbackItem(ICamelliaNamingCallback callback, EventListener eventListener) {
            this.callback = callback;
            this.eventListener = eventListener;
        }
    }
}
