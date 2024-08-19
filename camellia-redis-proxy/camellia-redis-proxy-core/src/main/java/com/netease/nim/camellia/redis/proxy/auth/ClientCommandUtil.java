package com.netease.nim.camellia.redis.proxy.auth;

import com.netease.nim.camellia.redis.base.proxy.ProxyUtil;
import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.monitor.ChannelMonitor;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.*;
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
            if (objects.length < 2) {
                ErrorLogCollector.collect(ClientCommandUtil.class, "client command syntax error");
                return ErrorReply.SYNTAX_ERROR;
            }
            String section = Utils.bytesToString(objects[1]);

            if (section.equalsIgnoreCase(RedisKeyword.GETNAME.name())) {
                if (channelInfo != null) {
                    String clientName = channelInfo.getClientName();
                    return new StatusReply(clientName);
                }
            } else if (section.equalsIgnoreCase(RedisKeyword.INFO.name())) {
                if (channelInfo != null) {
                    return new BulkReply(Utils.stringToBytes(clientInfo(channelInfo)));
                }
            } else if (section.equalsIgnoreCase(RedisKeyword.LIST.name())) {
                return new BulkReply(Utils.stringToBytes(clientList()));
            } else if (section.equalsIgnoreCase(RedisKeyword.SETNAME.name())) {
                if (objects.length != 3) {
                    ErrorLogCollector.collect(ClientCommandUtil.class, "client command syntax error, arg = " + section);
                    return ErrorReply.SYNTAX_ERROR;
                }
                String clienName = Utils.bytesToString(objects[2]);
                if (channelInfo == null) {
                    ErrorLogCollector.collect(ClientCommandUtil.class, "client command syntax error, channel info null");
                    return ErrorReply.SYNTAX_ERROR;
                }
                boolean success = updateClientName(channelInfo, clienName);
                if (!success) {
                    return ErrorReply.REPEAT_OPERATION;
                }
                return StatusReply.OK;
            } else if (section.equalsIgnoreCase(RedisKeyword.KILL.name())) {
                if (objects.length != 4) {
                    ErrorLogCollector.collect(ClientCommandUtil.class, "client command syntax error, arg = " + section);
                    return ErrorReply.SYNTAX_ERROR;
                }
                String type = Utils.bytesToString(objects[2]);
                String target = Utils.bytesToString(objects[3]);
                if (type.equalsIgnoreCase("ADDR")) {
                    int count = 0;
                    Map<String, ChannelInfo> channelMap = ChannelMonitor.getChannelMap();
                    for (ChannelInfo info : channelMap.values()) {
                        if (target.equalsIgnoreCase(info.getAddr()) && !info.getConsid().equalsIgnoreCase(channelInfo.getConsid())) {
                            logger.warn("kill client by ADDR, id = {}, addr = {}, laddr = {}, consid = {}", info.getId(), info.getAddr(), info.getLAddr(), info.getConsid());
                            info.getCtx().close();
                            count ++;
                        }
                    }
                    return IntegerReply.parse(count);
                } else if (type.equalsIgnoreCase("LADDR")) {
                    int count = 0;
                    Map<String, ChannelInfo> channelMap = ChannelMonitor.getChannelMap();
                    for (ChannelInfo info : channelMap.values()) {
                        if (target.equalsIgnoreCase(info.getLAddr()) && !info.getConsid().equalsIgnoreCase(channelInfo.getConsid())) {
                            logger.warn("kill client by LADDR, id = {}, addr = {}, laddr = {}, consid = {}", info.getId(), info.getAddr(), info.getLAddr(), info.getConsid());
                            info.getCtx().close();
                            count ++;
                        }
                    }
                    return IntegerReply.parse(count);
                } else if (type.equalsIgnoreCase("ID")) {
                    ChannelInfo info;
                    try {
                        info = ChannelMonitor.getChannelById(Long.parseLong(target));
                    } catch (NumberFormatException e) {
                        ErrorLogCollector.collect(ClientCommandUtil.class, "client kill id command syntax error, id = " + target);
                        return ErrorReply.SYNTAX_ERROR;
                    }
                    if (info != null && !info.getConsid().equalsIgnoreCase(channelInfo.getConsid())) {
                        logger.warn("kill client by ID, id = {}, addr = {}, laddr = {}, consid = {}", info.getId(), info.getAddr(), info.getLAddr(), info.getConsid());
                        info.getCtx().close();
                        return IntegerReply.REPLY_1;
                    }
                    return IntegerReply.REPLY_0;
                }
                ErrorLogCollector.collect(ClientCommandUtil.class, "client kill command syntax error, type = " + type);
                return ErrorReply.SYNTAX_ERROR;
            }
            ErrorLogCollector.collect(ClientCommandUtil.class, "client command syntax error, arg = " + Utils.bytesToString(objects[1]));
            return ErrorReply.SYNTAX_ERROR;
        } catch (Exception e) {
            ErrorLogCollector.collect(ClientCommandUtil.class, "invokeClientCommand error", e);
            return ErrorReply.SYNTAX_ERROR;
        }
    }

    public static boolean updateClientName(ChannelInfo channelInfo, String clientName) {
        Long bid = ProxyUtil.parseBid(clientName);
        String bgroup = ProxyUtil.parseBgroup(clientName);
        if (bid == null && bgroup == null) {
            channelInfo.setClientName(clientName);
            return true;
        }
        //如果通过client name来设置bid/bgroup，则不允许修改
        String oldClientName = channelInfo.getClientName();
        Long oldBid = ProxyUtil.parseBid(oldClientName);
        String oldBgroup = ProxyUtil.parseBgroup(oldClientName);
        if (oldBid != null && oldBgroup != null) {
            return false;
        }
        channelInfo.setClientName(clientName);
        channelInfo.setBid(bid);
        channelInfo.setBgroup(bgroup);
        return true;
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
