package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.MultiBulkReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2021/8/31
 */
public class HelloCommandUtil {

    private static final ErrorReply AUTH_SYNTAX_ERROR = new ErrorReply("ERR Syntax error in HELLO option 'auth'");
    private static final ErrorReply SETNAME_SYNTAX_ERROR = new ErrorReply("ERR Syntax error in HELLO option 'setname'");

    public static Reply invokeHelloCommand(ChannelInfo channelInfo, AuthCommandProcessor authCommandProcessor, Command command) {
        try {
            byte[][] objects = command.getObjects();
            if (objects.length == 1) {
                return MultiBulkReply.EMPTY;
            }
            if (objects.length > 2) {
                for (int i=1; i<objects.length; i++) {
                    String param = Utils.bytesToString(objects[i]);
                    if (param.equalsIgnoreCase("AUTH")) {
                        try {
                            String userName = Utils.bytesToString(objects[i + 1]);
                            String password = Utils.bytesToString(objects[i + 2]);
                            boolean pass = authCommandProcessor.checkPassword(channelInfo, userName, password);
                            if (!pass) {
                                return ErrorReply.WRONG_PASS;
                            }
                        } catch (Exception e) {
                            return AUTH_SYNTAX_ERROR;
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
        } catch (Exception e) {
            ErrorLogCollector.collect(HelloCommandUtil.class, e.getMessage(), e);
            return ErrorReply.SYNTAX_ERROR;
        }
    }
}
