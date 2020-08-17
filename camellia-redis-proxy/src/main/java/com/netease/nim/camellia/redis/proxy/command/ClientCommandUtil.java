package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.ProxyUtil;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by caojiajun on 2019/12/12.
 */
public class ClientCommandUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClientCommandUtil.class);

    public static Reply invokeClientCommand(ChannelInfo channelInfo, Command client) {
        byte[][] objects = client.getObjects();
        if (objects.length == 2) {
            boolean getname = Utils.checkStringIgnoreCase(objects[1], RedisKeyword.GETNAME.name());
            if (getname) {
                if (channelInfo != null) {
                    String clientName = channelInfo.getClientName();
                    return new StatusReply(clientName);
                }
            }
        } else if (objects.length == 3) {
            boolean setname = Utils.checkStringIgnoreCase(objects[1], RedisKeyword.SETNAME.name());
            if (setname) {
                String clienName = Utils.bytesToString(objects[2]);
                if (channelInfo != null) {
                    //不允许变更clientname
                    if (channelInfo.getClientName() != null) {
                        return ErrorReply.REPEAT_OPERATION;
                    }
                    channelInfo.setClientName(clienName);
                    Long bid = ProxyUtil.parseBid(clienName);
                    String bgroup = ProxyUtil.parseBgroup(clienName);
                    if (bid != null && bgroup != null) {
                        channelInfo.setBid(bid);
                        channelInfo.setBgroup(bgroup);
                        if (logger.isDebugEnabled()) {
                            logger.debug("channel init with bid/bgroup = {}/{}, consid = {}", bid, bgroup, channelInfo.getConsid());
                        }
                    }
                    return StatusReply.OK;
                }
            }
        }
        return ErrorReply.SYNTAX_ERROR;
    }
}
