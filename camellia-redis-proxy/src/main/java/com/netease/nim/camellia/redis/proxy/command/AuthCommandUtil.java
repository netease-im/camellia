package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.command.auth.ClientAuthProvider;
import com.netease.nim.camellia.redis.proxy.command.auth.ClientIdentity;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthCommandUtil {
    private static final Logger logger = LoggerFactory.getLogger(AuthCommandUtil.class);

    private final ClientAuthProvider clientAuthProvider;

    public AuthCommandUtil(ClientAuthProvider clientAuthProvider) {
        this.clientAuthProvider = clientAuthProvider;
    }

    public Reply invokeAuthCommand(ChannelInfo channelInfo, Command auth) {
        if (!this.clientAuthProvider.isPasswordRequired()) {
            return new ErrorReply("ERR Client sent AUTH, but no password is set");
        }

        byte[][] objects = auth.getObjects();
        if (objects.length != 2) {
            return ErrorReply.INVALID_PASSWORD;
        }

        String password = Utils.bytesToString(objects[1]);

        ClientIdentity clientIdentity = this.clientAuthProvider.auth(password);
        if (clientIdentity == null || !clientIdentity.isPass()) {
            channelInfo.setChannelStats(ChannelInfo.ChannelStats.NO_AUTH);
            return ErrorReply.INVALID_PASSWORD;
        }

        channelInfo.setChannelStats(ChannelInfo.ChannelStats.AUTH_OK);

        if (clientIdentity.getBid() != null && channelInfo.getBid() == null) {//不允许auth多次来改变bid/bgroup
            channelInfo.setBid(clientIdentity.getBid());
            channelInfo.setBgroup(clientIdentity.getBgroup());
            if (logger.isDebugEnabled()) {
                logger.debug("channel init with bid/bgroup = {}/{}, consid = {} by password",
                        clientIdentity.getBid(),
                        clientIdentity.getBgroup(),
                        channelInfo.getConsid());
            }
        }
        return StatusReply.OK;
    }

    public boolean isPasswordRequired() {
        return clientAuthProvider.isPasswordRequired();
    }
}
