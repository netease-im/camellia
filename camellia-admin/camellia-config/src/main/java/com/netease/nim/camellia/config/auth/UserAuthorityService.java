package com.netease.nim.camellia.config.auth;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Created by caojiajun on 2023/3/15
 */
public interface UserAuthorityService {

    AuthorityResult authority(HttpServletRequest request, HttpServletResponse response);

}
