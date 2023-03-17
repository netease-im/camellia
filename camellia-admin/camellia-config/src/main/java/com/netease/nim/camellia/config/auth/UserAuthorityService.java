package com.netease.nim.camellia.config.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by caojiajun on 2023/3/15
 */
public interface UserAuthorityService {

    AuthorityResult authority(HttpServletRequest request, HttpServletResponse response);

}
