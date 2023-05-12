package com.netease.nim.camellia.hot.key.server.conf;

import com.netease.nim.camellia.hot.key.server.bean.BeanInitUtils;

/**
 * Created by caojiajun on 2023/5/12
 */
public class ConfigInitUtil {

    public static HotKeyConfigService initHotKeyConfigService(HotKeyServerProperties properties) {
        String className = properties.getHotKeyConfigServiceClassName();
        if (className.equals(FileBasedHotKeyConfigService.class.getName())) {
            return new FileBasedHotKeyConfigService();
        } else if (className.equals(ApiBasedHotKeyConfigService.class.getName())) {
            return new ApiBasedHotKeyConfigService(properties.getConfig());
        }
        return (HotKeyConfigService) properties.getBeanFactory().getBean(BeanInitUtils.parseClass(properties.getHotKeyConfigServiceClassName()));
    }
}
