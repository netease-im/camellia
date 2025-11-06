package com.netease.nim.camellia.console.service;

import com.netease.nim.camellia.console.service.ao.TableAO;
import com.netease.nim.camellia.console.service.ao.TableRefAO;
import com.netease.nim.camellia.console.service.ao.URLAO;
import com.netease.nim.camellia.console.service.bo.TableBO;
import com.netease.nim.camellia.console.service.bo.TableRefBO;
import jakarta.servlet.http.HttpServletRequest;


/**
 * Created by caojiajun on 2023/3/29
 */
public interface OperationInterceptor {

    default boolean createTable(HttpServletRequest request, String username, TableAO tableAO) {
        return true;
    }

    default boolean deleteTable(HttpServletRequest request, String username, TableBO oldTable, TableAO tableAO) {
        return true;
    }

    default boolean changeTable(HttpServletRequest request, String username, TableBO oldTable, TableAO tableAO) {
        return true;
    }

    default boolean createOrUpdateTableRef(HttpServletRequest request, String username, TableRefBO oldTableRef, TableRefAO tableRefAO) {
        return true;
    }

    default boolean deleteTableRef(HttpServletRequest request, String username, TableRefBO oldTableRef, TableRefAO tableRefAO) {
        return true;
    }

    default boolean createOrUpdateResource(HttpServletRequest request, String username, URLAO urlao) {
        return true;
    }

    default boolean deleteResource(HttpServletRequest request, String username, URLAO urlao) {
        return true;
    }
}
