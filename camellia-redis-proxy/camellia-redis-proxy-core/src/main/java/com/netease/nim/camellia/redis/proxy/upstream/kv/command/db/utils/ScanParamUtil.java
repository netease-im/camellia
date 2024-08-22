package com.netease.nim.camellia.redis.proxy.upstream.kv.command.db.utils;

import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.regex.Pattern;

/**
 * Created by caojiajun on 2024/8/22
 */
public class ScanParamUtil {

    public static ErrorReply parseScanParam(ScanParam scanParam, byte[][] objects) {
        if (objects.length > 2) {
            ErrorReply reply = parseScanParam(scanParam, objects[2], objects[3]);
            if (reply != null) {
                return reply;
            }
        }
        if (objects.length > 4) {
            ErrorReply reply = parseScanParam(scanParam, objects[4], objects[5]);
            if (reply != null) {
                return reply;
            }
        }
        if (objects.length > 6) {
            ErrorReply reply = parseScanParam(scanParam, objects[6], objects[7]);
            if (reply != null) {
                return reply;
            }
        }
        return null;
    }

    private static ErrorReply parseScanParam(ScanParam scanParam, byte[] option, byte[] value) {
        String optionStr = Utils.bytesToString(option);
        if (optionStr.equalsIgnoreCase("COUNT")) {
            scanParam.count = Integer.parseInt(Utils.bytesToString(value));
        } else if (optionStr.equalsIgnoreCase("MATCH")) {
            String match = Utils.bytesToString(value);
            match = match.replace("{", "\\{");
            match = match.replace("*", ".*");
            scanParam.pattern = Pattern.compile(match);
        } else if (optionStr.equalsIgnoreCase("TYPE")) {
            scanParam.type = Utils.bytesToString(value);
        } else {
            ErrorLogCollector.collect(ScanParamUtil.class, "scan param error, option = " + optionStr);
            return ErrorReply.SYNTAX_ERROR;
        }
        return null;
    }
}
