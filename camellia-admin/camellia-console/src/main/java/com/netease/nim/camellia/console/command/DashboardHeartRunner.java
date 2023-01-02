package com.netease.nim.camellia.console.command;

import com.netease.nim.camellia.console.conf.ConsoleProperties;
import com.netease.nim.camellia.console.constant.ModelConstant;
import com.netease.nim.camellia.console.model.CamelliaDashboard;
import com.netease.nim.camellia.console.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */

@Component
public class DashboardHeartRunner implements CommandLineRunner, Ordered {

    @Autowired
    DashboardService dashboardService;

    @Autowired
    ConsoleProperties consoleProperties;

    @Autowired
    DashboardHeart dashboardHeart;

    @Override
    public void run(String... args) throws Exception {
        List<CamelliaDashboard> allDashboard = dashboardService.getAllDashboardByUseAndOnline(ModelConstant.use,null);
        for(CamelliaDashboard camelliaDashboard:allDashboard){
            dashboardHeart.put(camelliaDashboard.getDid(), camelliaDashboard.getAddress());
        }
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                dashboardHeart.reload();
            }
        }, 1, consoleProperties.getReloadSeconds(), TimeUnit.SECONDS);


    }

    @Override
    public int getOrder() {
        
        return 1;
    }
}
