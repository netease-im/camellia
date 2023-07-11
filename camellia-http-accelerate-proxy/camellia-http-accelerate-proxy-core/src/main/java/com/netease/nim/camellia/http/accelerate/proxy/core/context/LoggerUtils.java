package com.netease.nim.camellia.http.accelerate.proxy.core.context;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.http.accelerate.proxy.core.monitor.ProxyMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by caojiajun on 2023/7/10
 */
public class LoggerUtils {

    private static final Logger logger = LoggerFactory.getLogger("stats");

    public static void logging(LogBean logBean) {
        try {
            JSONObject logJson = new JSONObject(true);
            logJson.put("host", logBean.getHost());
            logJson.put("path", logBean.getPath());
            logJson.put("code", logBean.getCode());
            logJson.put("errorReason", logBean.getErrorReason());
            if (logBean.getErrorReason() != null) {
                ProxyMonitor.updateError(logBean.getHost(), logBean.getErrorReason());
            }
            if (logBean.getEndTime() != null && logBean.getStartTime() != null) {
                long spendTime = logBean.getEndTime() - logBean.getStartTime();
                logJson.put("spendTime", spendTime);
                ProxyMonitor.updateSpendTime(logBean.getHost(), spendTime);
            }
            if (logBean.getTransportServerSendTime() != null && logBean.getTransportServerReceiveTime() != null) {
                long transportSpendTime1 = logBean.getTransportServerReceiveTime() - logBean.getTransportServerSendTime();
                logJson.put("transportSpendTime1", transportSpendTime1);
                ProxyMonitor.updateTransportSpendTime1(logBean.getHost(), transportSpendTime1);
            }
            if (logBean.getUpstreamSendTime() != null && logBean.getUpstreamReplyTime() != null) {
                long upstreamSpendTime = logBean.getUpstreamReplyTime() - logBean.getUpstreamSendTime();
                logJson.put("upstreamSpendTime", upstreamSpendTime);
                ProxyMonitor.updateUpstreamSpendTime("upstreamSpendTime", upstreamSpendTime);
            }
            if (logBean.getEndTime() != null && logBean.getUpstreamReplyTime() != null) {
                long transportSpendTime2 = logBean.getEndTime() - logBean.getUpstreamReplyTime();
                logJson.put("transportSpendTime2", transportSpendTime2);
                ProxyMonitor.updateTransportSpendTime2(logBean.getHost(), transportSpendTime2);
            }
            logJson.put("transportAddr", logBean.getTransportAddr());
            logJson.put("upstreamAddr", logBean.getUpstreamAddr());
            logJson.put("startTime", logBean.getStartTime());
            logJson.put("transportServerSendTime", logBean.getTransportServerSendTime());
            logJson.put("transportServerReceiveTime", logBean.getTransportServerReceiveTime());
            logJson.put("upstreamSendTime", logBean.getUpstreamSendTime());
            logJson.put("upstreamReplyTime", logBean.getUpstreamReplyTime());
            logJson.put("endTime", logBean.getEndTime());
            logJson.put("traceId", logBean.getTraceId());
            logger.info(logJson.toJSONString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
