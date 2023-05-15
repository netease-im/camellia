package com.netease.nim.camellia.hot.key.server.conf;

import com.netease.nim.camellia.hot.key.server.bean.BeanInitUtils;

/**
 * Created by caojiajun on 2023/5/12
 */
public class ConfigInitUtil {

    public static HotKeyConfigService initHotKeyConfigService(HotKeyServerProperties properties) {
        HotKeyConfigService service;
        String className = properties.getHotKeyConfigServiceClassName();
        if (className.equals(FileBasedHotKeyConfigService.class.getName())) {
            service = new FileBasedHotKeyConfigService();
        } else if (className.equals(ApiBasedHotKeyConfigService.class.getName())) {
            service = new ApiBasedHotKeyConfigService();
        } else {
            service = (HotKeyConfigService) properties.getBeanFactory().getBean(BeanInitUtils.parseClass(properties.getHotKeyConfigServiceClassName()));
        }
        service.init(properties);
        return service;
    }
}
