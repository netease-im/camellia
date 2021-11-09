package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2021/8/31
 */
public class HelloCommandUtil {

    private static final ErrorReply AUTH_SYNTAX_ERROR = new ErrorReply("ERR Syntax error in HELLO option 'auth'");
    private static final ErrorReply SETNAME_SYNTAX_ERROR = new ErrorReply("ERR Syntax error in HELLO option 'setname'");

    public static Reply invokeHelloCommand(ChannelInfo channelInfo, AuthCommandProcessor authCommandProcessor, Command command) {
        byte[][] objects = command.getObjects();
        if (objects.length == 1) {
            return MultiBulkReply.EMPTY;
        }
        if (objects.length > 2) {
            for (int i=1; i<objects.length; i++) {
                String param = Utils.bytesToString(objects[i]);
                if (param.equalsIgnoreCase("AUTH")) {
                    String userName;
                    String password;
                    try {
                        userName = Utils.bytesToString(objects[i + 1]);
                        password = Utils.bytesToString(objects[i + 2]);
                    } catch (Exception e) {
                        return AUTH_SYNTAX_ERROR;
                    }
                    boolean pass = authCommandProcessor.checkPassword(channelInfo, userName, password);
                    if (!pass) {
                        return ErrorReply.WRONG_PASS;
                    }
                }
                if (param.equalsIgnoreCase("SETNAME")) {
                    try {
                        String clientName = Utils.bytesToString(objects[i + 1]);
                        ClientCommandUtil.updateClientName(channelInfo, clientName);
                    } catch (Exception e) {
                        return SETNAME_SYNTAX_ERROR;
                    }
                }
            }
        }
        return MultiBulkReply.EMPTY;
    }
}
