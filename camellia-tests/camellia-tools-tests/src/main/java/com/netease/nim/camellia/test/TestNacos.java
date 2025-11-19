package com.netease.nim.camellia.test;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Properties;

/**
 * Created by caojiajun on 2025/11/18
 */
public class TestNacos {

    public static void main(String[] args) throws NacosException {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, "localhost:8848");

        NamingService namingService = NacosFactory.createNamingService(properties);

        String serviceName = "test_service";
        String ip = "10.1.1.1";
        int port = 8090;
        namingService.registerInstance(serviceName, ip, port);
        namingService.registerInstance(serviceName, ip, port);

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            List<Instance> allInstances = namingService.getAllInstances(serviceName);
            for (Instance allInstance : allInstances) {
                System.out.println(allInstance.getIp() + ":" + allInstance.getPort());
            }
        }



    }
}
