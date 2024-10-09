package com.netease.nim.camellia.config.controller;

import com.netease.nim.camellia.config.conf.LogBean;
import com.netease.nim.camellia.config.model.*;
import com.netease.nim.camellia.config.service.ConfigNamespaceService;
import com.netease.nim.camellia.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;


/**
 * Created by caojiajun on 2023/3/15
 */
@RestController
@RequestMapping("/camellia/config/admin")
public class ConfigAdminController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private ConfigNamespaceService configNamespaceService;

    @PostMapping("/createOrUpdateConfig")
    public WebResult createOrUpdateConfig(HttpServletRequest request,
                                          @RequestParam("namespace") String namespace,
                                          @RequestParam("key") String key,
                                          @RequestParam(value = "value", required = false) String value,
                                          @RequestParam(value = "type", required = false) Integer type,
                                          @RequestParam(value = "info", required = false) String info,
                                          @RequestParam(value = "version", required = false) Long version,
                                          @RequestParam(value = "validFlag", required = false) Integer validFlag,
                                          @RequestParam(value = "operatorInfo") String operatorInfo) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        key = key.trim();
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("key", key);
        LogBean.get().addProps("value", value);
        LogBean.get().addProps("type", type);
        LogBean.get().addProps("version", version);
        LogBean.get().addProps("info", info);
        LogBean.get().addProps("validFlag", validFlag);
        LogBean.get().addProps("operatorInfo", operatorInfo);
        Config config = configService.createOrUpdateConfig(request, namespace, key, value, type, info, version, validFlag, operatorInfo);
        LogBean.get().addProps("config", config);
        return WebResult.success(config);
    }

    @PostMapping("/getConfigList")
    public WebResult getConfigList(@RequestParam("namespace") String namespace,
                                   @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                   @RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
                                   @RequestParam(value = "validFlag", required = false) Integer validFlag,
                                   @RequestParam(value = "keyword", required = false) String keyword) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("validFlag", validFlag);
        LogBean.get().addProps("pageIndex", pageIndex);
        LogBean.get().addProps("pageSize", pageSize);
        LogBean.get().addProps("keyword", keyword);
        int offset = pageIndex * pageSize;
        ConfigPage configPage = configService.getConfigList(namespace, offset, pageSize, validFlag, keyword);
        LogBean.get().addProps("configPage", configPage);
        return WebResult.success(configPage);
    }

    @PostMapping(value = "/getConfigString", produces = "text/plain;charset=UTF-8")
    public String getConfigString(@RequestParam("namespace") String namespace,
                                  @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                  @RequestParam(value = "pageSize", required = false, defaultValue = "10000") int pageSize,
                                  @RequestParam(value = "validFlag", required = false) Integer validFlag,
                                  @RequestParam(value = "keyword", required = false) String keyword) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("validFlag", validFlag);
        LogBean.get().addProps("keyword", keyword);
        LogBean.get().addProps("pageSize", pageSize);
        LogBean.get().addProps("keyword", keyword);
        int offset = pageIndex * pageSize;
        String configString = configService.getConfigString(namespace, offset, pageSize, validFlag, keyword);
        LogBean.get().addProps("configString", configString);
        return configString;
    }

    @PostMapping("/getConfigByKey")
    public WebResult getConfigByKey(@RequestParam("namespace") String namespace,
                                    @RequestParam("key") String key) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        key = key.trim();
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("key", key);
        Config config = configService.getConfigByKey(namespace, key);
        LogBean.get().addProps("config", config);
        return WebResult.success(config);
    }

    @PostMapping("/deleteConfig")
    public WebResult deleteConfig(HttpServletRequest request,
                                  @RequestParam("namespace") String namespace,
                                  @RequestParam("id") long id,
                                  @RequestParam("key") String key,
                                  @RequestParam(value = "version", required = false) Long version,
                                  @RequestParam(value = "operatorInfo") String operatorInfo) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        key = key.trim();
        LogBean.get().addProps("id", id);
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("key", key);
        LogBean.get().addProps("operatorInfo", operatorInfo);
        int ret = configService.deleteConfig(request, namespace, id, key, version, operatorInfo);
        return WebResult.success(ret);
    }

    @PostMapping("/getConfigNamespaceHistoryList")
    public WebResult getConfigNamespaceHistoryList(@RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                                   @RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
                                                   @RequestParam(value = "keyword", required = false) String keyword) {
        LogBean.get().addProps("pageSize", pageSize);
        LogBean.get().addProps("keyword", keyword);
        int offset = pageIndex * pageSize;
        LogBean.get().addProps("keyword", keyword);
        ConfigHistoryPage configHistoryPage = configService.getConfigNamespaceHistoryList(offset, pageSize, keyword);
        return WebResult.success(configHistoryPage);
    }

    @PostMapping("/getConfigHistoryListByNamespace")
    public WebResult getConfigHistoryListByNamespace(@RequestParam("namespace") String namespace,
                                                     @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                                     @RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
                                                     @RequestParam(value = "keyword", required = false) String keyword) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("pageSize", pageSize);
        LogBean.get().addProps("keyword", keyword);
        int offset = pageIndex * pageSize;
        LogBean.get().addProps("keyword", keyword);
        ConfigHistoryPage configHistoryPage = configService.getConfigHistoryListByNamespace(namespace, offset, pageSize, keyword);
        return WebResult.success(configHistoryPage);
    }

    @PostMapping("/getConfigHistoryListByConfigKey")
    public WebResult getConfigHistoryListByConfigKey(@RequestParam("namespace") String namespace,
                                                     @RequestParam(value = "key", required = false) String key,
                                                     @RequestParam(value = "id", required = false) Long id,
                                                     @RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                                     @RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
                                                     @RequestParam(value = "keyword", required = false) String keyword) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        if (key != null) {
            key = key.trim();
        }
        LogBean.get().addProps("key", key);
        LogBean.get().addProps("id", id);
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("pageSize", pageSize);
        LogBean.get().addProps("keyword", keyword);
        int offset = pageIndex * pageSize;
        LogBean.get().addProps("keyword", keyword);
        ConfigHistoryPage configHistoryPage;
        if (id != null && id > 0) {
            configHistoryPage = configService.getConfigHistoryListByConfigId(namespace, id, offset, pageSize, keyword);
        } else {
            configHistoryPage = configService.getConfigHistoryListByConfigKey(namespace, key, offset, pageSize, keyword);
        }
        return WebResult.success(configHistoryPage);
    }

    @PostMapping("/createOrUpdateConfigNamespace")
    public WebResult createOrUpdateConfigNamespace(HttpServletRequest request,
                                                   @RequestParam("namespace") String namespace,
                                                   @RequestParam(value = "info", required = false) String info,
                                                   @RequestParam(value = "alias", required = false) String alias,
                                                   @RequestParam(value = "version", required = false) Integer version,
                                                   @RequestParam(value = "validFlag", required = false) Integer validFlag,
                                                   @RequestParam(value = "operatorInfo") String operatorInfo) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("info", info);
        LogBean.get().addProps("validFlag", validFlag);
        LogBean.get().addProps("version", version);
        LogBean.get().addProps("alias", alias);
        LogBean.get().addProps("operatorInfo", operatorInfo);
        ConfigNamespace configNamespace = configNamespaceService.createOrUpdateConfigNamespace(request, namespace, info, alias, version, validFlag, operatorInfo);
        LogBean.get().addProps("configNamespace", configNamespace);
        return WebResult.success(configNamespace);
    }

    @PostMapping("/deleteConfigNamespace")
    public WebResult deleteConfigNamespace(HttpServletRequest request,
                                           @RequestParam(value = "id", required = false) Long id,
                                           @RequestParam(value = "version") Integer version,
                                           @RequestParam("namespace") String namespace,
                                           @RequestParam(value = "operatorInfo") String operatorInfo) {
        namespace  = namespace.toLowerCase(Locale.ROOT);
        LogBean.get().addProps("namespace", namespace);
        LogBean.get().addProps("id", id);
        LogBean.get().addProps("version", version);
        LogBean.get().addProps("operatorInfo", operatorInfo);
        int ret = configNamespaceService.deleteConfigNamespace(request, id, namespace, version, operatorInfo);
        LogBean.get().addProps("ret", ret);
        return WebResult.success(ret);
    }

    @PostMapping("/getConfigNamespaceList")
    public WebResult getConfigNamespaceList(@RequestParam(value = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                            @RequestParam(value = "pageSize", required = false, defaultValue = "100") int pageSize,
                                            @RequestParam(value = "validFlag", required = false) Integer validFlag,
                                            @RequestParam(value = "keyword", required = false) String keyword) {
        LogBean.get().addProps("pageSize", pageSize);
        LogBean.get().addProps("keyword", keyword);
        int offset = pageIndex * pageSize;
        LogBean.get().addProps("keyword", keyword);
        LogBean.get().addProps("validFlag", validFlag);
        ConfigNamespacePage configNamespacePage = configNamespaceService.getList(offset, pageSize, validFlag, keyword);
        LogBean.get().addProps("configNamespacePage", configNamespacePage);
        return WebResult.success(configNamespacePage);
    }
}
