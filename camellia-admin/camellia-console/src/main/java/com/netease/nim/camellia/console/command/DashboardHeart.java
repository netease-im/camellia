package com.netease.nim.camellia.console.command;

import com.alibaba.fastjson.JSON;
import com.netease.nim.camellia.console.conf.ConsoleProperties;
import com.netease.nim.camellia.console.constant.ModelConstant;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.model.WebResult;
import com.netease.nim.camellia.console.service.DashboardService;
import com.netease.nim.camellia.console.util.DashboardApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.netease.nim.camellia.console.constant.ModelConstant.*;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
@Component
public class DashboardHeart {
    private static final Logger logger=LoggerFactory.getLogger(DashboardHeart.class);

    private final Map<Long, ScheduledExecutorService> dashboardHeartMap = new ConcurrentHashMap<>();
    @Autowired
    DashboardService dashboardService;
    @Autowired
    ConsoleProperties consoleProperties;

    public Map<Long, ScheduledExecutorService> getDashboardHeartMap() {
        return dashboardHeartMap;
    }

    public void put(Long key, String url) {
        if (dashboardHeartMap.get(key) == null) {
            synchronized (dashboardHeartMap) {
                if (dashboardHeartMap.get(key) == null) {
                    logger.info("put {} to dashboardHeartMap",key);
                    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                    Runnable task = new HeartRunnable(url, key, dashboardService);
                    scheduledExecutorService.scheduleAtFixedRate(task, 1, consoleProperties.getHeartCallSeconds(), TimeUnit.SECONDS);
                    dashboardHeartMap.put(key, scheduledExecutorService);
                }
            }
        }
    }

    public void cancel(Long key) {
        ScheduledExecutorService scheduledExecutorService = dashboardHeartMap.get(key);
        if (scheduledExecutorService != null) {
            logger.info("delete {} from dashboardHeartMap",key);
//            synchronized (dashboardHeartMap){
            scheduledExecutorService.shutdown();
            dashboardHeartMap.remove(key);
//            }
        }
    }

    public void reload(){
        synchronized (dashboardHeartMap){
            List<CamelliaDashboard> allDashboard = dashboardService.getAllDashboardByUseAndOnline(ModelConstant.use,null);
            if(allDashboard==null){
                allDashboard=new ArrayList<>();
            }
            Set<Long> collect = allDashboard.stream().map(CamelliaDashboard::getDid).collect(Collectors.toSet());
            Set<Long> longs = dashboardHeartMap.keySet();
            if(isSameSet(collect,longs)){
                logger.debug("same dashboard not reload");
                return;
            }
            logger.info("reload dashboard{}", JSON.toJSONString(allDashboard));
            for(ScheduledExecutorService service:dashboardHeartMap.values()){
                if(service!=null&&!service.isShutdown()){
                    service.shutdown();
                }
            }
            dashboardHeartMap.clear();
            for(CamelliaDashboard camelliaDashboard:allDashboard){
                Long key=camelliaDashboard.getDid();
                String url = camelliaDashboard.getAddress();
                ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                Runnable task = new HeartRunnable(url, key, dashboardService);
                scheduledExecutorService.scheduleAtFixedRate(task, 1, consoleProperties.getHeartCallSeconds(), TimeUnit.SECONDS);
                dashboardHeartMap.put(key, scheduledExecutorService);
            }
        }
    }

    private boolean isSameSet(Set<Long> var1, Set<Long> var2) {
        if(var1.size()==var2.size()){
            for(Long key:var1){
                if(!var2.contains(key)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    public static class HeartRunnable implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(HeartRunnable.class);

        private final String url;

        private final Long id;

        private final DashboardHeartApi dashboardHeartApi;

        private final DashboardService dashboardService;


        public HeartRunnable(String url, Long id, DashboardService dashboardService) {
            this.url = url;
            this.id = id;
            dashboardHeartApi = DashboardApiUtil.init(url, 2000, 2000);
            this.dashboardService = dashboardService;
        }

        @Override
        public void run() {
            CamelliaDashboard byId = dashboardService.getByDId(id);
            try {
                if(byId.getIsUse()==notUse){
                    logger.warn("id:{},url:{} is not use", byId.getDid(), byId.getAddress());
                    return;
                }
                WebResult check = dashboardHeartApi.check();
                if (check.getCode() == 200) {
                    if (byId.getIsOnline() == offLine) {
                        logger.info("url:{} online", url);
                        dashboardService.updateOnlineStatus(id, onLine);
                    }
                    return;
                }

            } catch (Exception e) {
                logger.warn("url:{} connect wrong reason:{}", url, e.getMessage());
            }
            if (byId.getIsOnline() == use) {
                logger.info("url:{} offline", url);
                dashboardService.updateOnlineStatus(id, offLine);
            }
        }
    }


}
