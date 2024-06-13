package com.netease.nim.camellia.redis.proxy.auth;

import com.netease.nim.camellia.redis.base.proxy.ProxyUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.BulkReply;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by caojiajun on 2019/12/12.
 */
public class ClientCommandUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClientCommandUtil.class);

    public static Reply invokeClientCommand(ChannelInfo channelInfo, Command command) {
        try {
            byte[][] objects = command.getObjects();
            if (objects.length == 2) {
                boolean getname = Utils.checkStringIgnoreCase(objects[1], RedisKeyword.GETNAME.name());
                if (getname) {
                    if (channelInfo != null) {
                        String clientName = channelInfo.getClientName();
                        return new StatusReply(clientName);
                    }
                }
                boolean info = Utils.checkStringIgnoreCase(objects[1], RedisKeyword.INFO.name());
                if (info) {
                    if (channelInfo != null) {
                        return new BulkReply(Utils.stringToBytes(clientInfo(channelInfo)));
                    }
                }
                boolean list = Utils.checkStringIgnoreCase(objects[1], RedisKeyword.LIST.name());
                if (list) {
                    return new BulkReply(Utils.stringToBytes(clientList()));
                }
            } else if (objects.length == 3) {
                boolean setname = Utils.checkStringIgnoreCase(objects[1], RedisKeyword.SETNAME.name());
                if (setname) {
                    String clienName = Utils.bytesToString(objects[2]);
                    if (channelInfo == null) {
                        return ErrorReply.SYNTAX_ERROR;
                    }
                    boolean success = updateClientName(channelInfo, clienName);
                    if (!success) {
                        return ErrorReply.REPEAT_OPERATION;
                    }
                    return StatusReply.OK;
                }
            }
            if (objects.length >= 2) {
                ErrorLogCollector.collect(ClientCommandUtil.class, "client command syntax error, arg = " + Utils.bytesToString(objects[1]));
            } else {
                ErrorLogCollector.collect(ClientCommandUtil.class, "client command syntax error");
            }
            return ErrorReply.SYNTAX_ERROR;
        } catch (Exception e) {
            ErrorLogCollector.collect(ClientCommandUtil.class, "invokeClientCommand error", e);
            return ErrorReply.SYNTAX_ERROR;
        }
    }

    public static boolean updateClientName(ChannelInfo channelInfo, String clientName) {
        if (channelInfo.getClientName() != null) {
            return false;
        }
        channelInfo.setClientName(clientName);
        if (channelInfo.getBid() == null) {//只有没有设置过bid/bgroup，才能通过client setname来设置bid/bgroup
            setBidAndBGroup(channelInfo, clientName);
        }
        return true;
    }

    private static void setBidAndBGroup(ChannelInfo channelInfo, String clienName) {
        Long bid = ProxyUtil.parseBid(clienName);
        String bgroup = ProxyUtil.parseBgroup(clienName);
        if (bid != null && bgroup != null) {
            channelInfo.setBid(bid);
            channelInfo.setBgroup(bgroup);
            if (logger.isDebugEnabled()) {
                logger.debug("channel init with bid/bgroup = {}/{}, consid = {} by client name", bid, bgroup, channelInfo.getConsid());
            }
        }
    }

    private static String clientList() {
        Map<String, ChannelInfo> channelMap = ChannelMonitor.getChannelMap();
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ChannelInfo> entry : channelMap.entrySet()) {
            builder.append(clientInfo(entry.getValue()));
        }
        return builder.toString();
    }

    private static String clientInfo(ChannelInfo channelInfo) {
        StringBuilder builder = new StringBuilder();
        builder.append("id=").append(channelInfo.getId()).append(" ");
        String addr = channelInfo.getAddr();
        if (addr != null) {
            builder.append("addr=").append(addr).append(" ");
        }
        String laddr = channelInfo.getLAddr();
        if (laddr != null) {
            builder.append("laddr=").append(laddr).append(" ");
        }
        String clientName = channelInfo.getClientName();
        if (clientName == null) {
            builder.append("name=").append(" ");
        } else {
            builder.append("name=").append(clientName).append(" ");
        }
        int db = channelInfo.getDb();
        if (db <= 0) {
            builder.append("db=").append(0).append(" ");
        } else {
            builder.append("db=").append(db).append(" ");
        }
        builder.append("age=").append(channelInfo.getAge()).append(" ");
        builder.append("idle=").append(channelInfo.getIdle()).append(" ");
        builder.append("sub=").append(channelInfo.getSub()).append(" ");
        builder.append("psub=").append(channelInfo.getPSub()).append(" ");
        builder.append("multi=").append(channelInfo.getMulti()).append(" ");
        builder.append("cmd=").append(channelInfo.getCmd()).append(" ");
        builder.append("user=").append(channelInfo.getUserName()).append(" ");
        builder.append("auth=").append(channelInfo.getAuth());
        builder.append("\n");
        return builder.toString();
    }
}
