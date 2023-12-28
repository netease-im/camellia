package com.netease.nim.camellia.redis.proxy.auth;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthCommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AuthCommandProcessor.class);

    private final ClientAuthProvider clientAuthProvider;

    public AuthCommandProcessor(ClientAuthProvider clientAuthProvider) {
        this.clientAuthProvider = clientAuthProvider;
    }

    public ClientAuthProvider getClientAuthProvider() {
        return clientAuthProvider;
    }

    public Reply invokeAuthCommand(ChannelInfo channelInfo, Command auth) {
        if (!this.clientAuthProvider.isPasswordRequired()) {
            return ErrorReply.NO_PASSWORD_SET;
        }

        byte[][] objects = auth.getObjects();
        if (objects.length != 2 && objects.length != 3) {
            return ErrorReply.INVALID_PASSWORD;
        }

        String userName = null;
        String password;
        if (objects.length == 2) {
            password = Utils.bytesToString(objects[1]);
        } else {
            userName = Utils.bytesToString(objects[1]);
            password = Utils.bytesToString(objects[2]);
        }

        boolean pass = checkPassword(channelInfo, userName, password);
        if (pass) {
            return StatusReply.OK;
        } else {
            return ErrorReply.INVALID_PASSWORD;
        }
    }

    public boolean checkPassword(ChannelInfo channelInfo, String userName, String password) {
        ClientIdentity clientIdentity = this.clientAuthProvider.auth(userName, password);
        if (clientIdentity == null || !clientIdentity.isPass()) {
            channelInfo.setChannelStats(ChannelInfo.ChannelStats.NO_AUTH);
            return false;
        }
        channelInfo.setChannelStats(ChannelInfo.ChannelStats.AUTH_OK);
        channelInfo.setUserName(userName);

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
        return true;
    }

    public boolean isPasswordRequired() {
        return clientAuthProvider.isPasswordRequired();
    }
}
