package com.netease.nim.camellia.redis.proxy.auth;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.reply.StatusReply;
import com.netease.nim.camellia.redis.proxy.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * auth command processor
 * Created by caojiajun on 2021/08/18
 */
public class AuthCommandProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AuthCommandProcessor.class);

    private final ClientAuthProvider clientAuthProvider;

    /**
     * constructor
     * @param clientAuthProvider provider
     */
    public AuthCommandProcessor(ClientAuthProvider clientAuthProvider) {
        this.clientAuthProvider = clientAuthProvider;
    }

    /**
     * get auth provider
     * @return provider
     */
    public ClientAuthProvider getClientAuthProvider() {
        return clientAuthProvider;
    }

    /**
     * invoke command
     * @param channelInfo channel info
     * @param auth auth command
     * @return reply
     */
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

    /**
     * check password
     * @param channelInfo channel info
     * @param userName user name
     * @param password password
     * @return result
     */
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

    /**
     * is password required
     * @return result
     */
    public boolean isPasswordRequired() {
        return clientAuthProvider.isPasswordRequired();
    }
}
