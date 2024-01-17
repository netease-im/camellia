package com.netease.nim.camellia.redis.proxy.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


/**
 * Created by caojiajun on 2024/1/16
 */
public class HttpCommandReplyConverter {

    public static List<Object> convert(List<Reply> replies, boolean base64) {
        if (replies == null) {
            return null;
        }
        if (replies.isEmpty()) {
            return new ArrayList<>();
        }
        List<Object> list = new ArrayList<>();
        for (Reply reply : replies) {
            list.add(convert(reply, base64));
        }
        return list;
    }

    public static JSON convert(Reply reply, boolean base64) {
        if (reply instanceof StatusReply) {
            JSONObject json = new JSONObject();
            json.put("error", false);
            json.put("value", ((StatusReply) reply).getStatus());
            return json;
        } else if (reply instanceof ErrorReply) {
            JSONObject json = new JSONObject();
            json.put("error", false);
            json.put("value", ((ErrorReply) reply).getError());
            return json;
        } else if (reply instanceof IntegerReply) {
            JSONObject json = new JSONObject();
            json.put("error", false);
            json.put("value", ((IntegerReply) reply).getInteger());
            return json;
        } else if (reply instanceof BulkReply) {
            JSONObject json = new JSONObject();
            json.put("error", false);
            byte[] raw = ((BulkReply) reply).getRaw();
            if (raw == null) {
                return json;
            }
            json.put("base64", base64);
            if (base64) {
                json.put("value", Base64.getEncoder().encodeToString(raw));
            } else {
                json.put("value", Utils.bytesToString(raw));
            }
            return json;
        } else if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            JSONObject json = new JSONObject();
            json.put("error", false);
            if (replies == null) {
                json.put("value", null);
            } else {
                JSONArray array = new JSONArray();
                for (Reply subReply : replies) {
                    array.add(convert(subReply, base64));
                }
                json.put("value", array);
            }
            return json;
        } else {
            throw new IllegalArgumentException("not support reply");
        }
    }
}
