package com.netease.nim.camellia.config.service;

import com.netease.nim.camellia.config.model.Config;
import com.netease.nim.camellia.config.model.ConfigNamespace;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by caojiajun on 2023/9/12
 */
public interface ConfigChangeInterceptor {

    default boolean createNamespace(HttpServletRequest request, ConfigNamespace namespace, String operator, String operatorInfo) {
        return true;
    }

    default boolean updateNamespace(HttpServletRequest request, ConfigNamespace oldNamespace, ConfigNamespace newNamespace, String operator, String operatorInfo) {
        return true;
    }

    default boolean deleteNamespace(HttpServletRequest request, ConfigNamespace namespace, String operator, String operatorInfo) {
        return true;
    }

    default boolean createConfig(HttpServletRequest request, Config config, String operator, String operatorInfo) {
        return true;
    }

    default boolean updateConfig(HttpServletRequest request, Config oldConfig, Config newConfig, String operator, String operatorInfo) {
        return true;
    }

    default boolean deleteConfig(HttpServletRequest request, Config config, String operator, String operatorInfo) {
        return true;
    }
}
