package com.netease.nim.camellia.console.util;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.console.constant.AppCode;
import com.netease.nim.camellia.console.exception.AppException;

/**
 * Create with IntelliJ IDEA
 *
 * @author ChenHongliang
 */
public class ResponseCheckUtil {
    public static JSONObject parseResponse(RespBody respBody){
        JSONObject jsonObject = JSONObject.parseObject(respBody.getData());
        if(respBody.getHttpCode()==200 && jsonObject.getInteger("code")==200){
            return jsonObject;
        }
        LogBean.get().addProps("dashboard response wrong",respBody);
        throw new AppException(AppCode.DASHBOARD_RESULT_WRONG,"response wrong:"+jsonObject.getString("msg"));
    }
}
