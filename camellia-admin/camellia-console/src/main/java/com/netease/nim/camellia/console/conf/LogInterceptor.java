package com.netease.nim.camellia.console.conf;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.console.controller.HealthStatus;
import com.netease.nim.camellia.console.util.LogBean;
import com.netease.nim.camellia.core.api.CamelliaApiEnv;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class LogInterceptor implements HandlerInterceptor {

    private static final Logger staticsLogger = LoggerFactory.getLogger("status");

    public static boolean isDebugEnabled() {
        return staticsLogger.isDebugEnabled();
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        LogBean logBean = LogBean.init();
        logBean.setUri(httpServletRequest.getRequestURI());
        String source = httpServletRequest.getHeader(CamelliaApiEnv.REQUEST_SOURCE);
        if (source != null) {
            logBean.setSource(source);
        }
        logBean.setStartTime(System.currentTimeMillis());
        String ip = getRequestIp(httpServletRequest);
        logBean.setIp(ip);
        return true;
    }

    private String getRequestIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (checkIp(ip)) {
            return ip;
        }
        ip = request.getHeader("X-Forwarded-For");
        if (checkIp(ip)) {
            return ip;
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (checkIp(ip)) {
            return ip;
        }
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (checkIp(ip)) {
            return ip;
        }
        ip = request.getRemoteAddr();
        return ip;
    }

    private boolean checkIp(String ip) {
        return ip != null && !ip.trim().isEmpty() && !ip.equalsIgnoreCase("unknown");
    }


    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        LogBean logBean = LogBean.get();
        logBean.setSpendTime();
        logBean.setCode(httpServletResponse.getStatus());
        logBean.setMethod(httpServletRequest.getMethod());
        String uri = logBean.getUri();
        JSONObject logBeanJson = logBean.toJson();
        int code = logBean.getCode();
        if (code == 200) {
            if (uri.startsWith("/health")) {
                if (staticsLogger.isDebugEnabled()) {
                    staticsLogger.debug(logBeanJson.toString());
                }
            } else {
                if (logBean.getSpendTime() > 1000) {
                    if (staticsLogger.isWarnEnabled()) {
                        staticsLogger.warn(logBeanJson.toString());
                    }
                } else {
                    if (staticsLogger.isInfoEnabled()) {
                        staticsLogger.info(logBeanJson.toString());
                    }
                }
            }
        } else if (code == 500) {
            if (staticsLogger.isErrorEnabled()) {
                staticsLogger.error(logBeanJson.toString());
            }
        } else {
            if (staticsLogger.isWarnEnabled()) {
                staticsLogger.warn(logBeanJson.toString());
            }
        }
        if (!uri.startsWith("/health")) {
            HealthStatus.updateRequestTimestamp();
        }
    }

}
