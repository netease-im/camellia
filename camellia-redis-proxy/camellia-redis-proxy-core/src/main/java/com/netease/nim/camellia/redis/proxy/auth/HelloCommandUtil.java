package com.netease.nim.camellia.redis.proxy.auth;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ServerConf;
import com.netease.nim.camellia.redis.proxy.info.ProxyInfoUtils;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.netty.GlobalRedisProxyEnv;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 * Created by caojiajun on 2021/8/31
 */
public class HelloCommandUtil {

    public static final ErrorReply AUTH_SYNTAX_ERROR = new ErrorReply("ERR Syntax error in HELLO option 'auth'");
    public static final ErrorReply SETNAME_SYNTAX_ERROR = new ErrorReply("ERR Syntax error in HELLO option 'setname'");

    public static Reply invokeHelloCommand(ChannelInfo channelInfo, AuthCommandProcessor authCommandProcessor, Command command) {
        byte[][] objects = command.getObjects();
        if (objects.length == 1) {
            return helloCmdReply();
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
        return helloCmdReply();
    }

    public static MultiBulkReply helloCmdReply() {
        Reply[] reply = new Reply[14];
        reply[0] = new BulkReply(Utils.stringToBytes("server"));
        reply[1] = new BulkReply(Utils.stringToBytes("redis"));
        reply[2] = new BulkReply(Utils.stringToBytes("version"));
        reply[3] = new BulkReply(Utils.stringToBytes(ProxyInfoUtils.getRedisVersion()));
        reply[4] = new BulkReply(Utils.stringToBytes("proto"));
        reply[5] = new IntegerReply(2L);
        reply[6] = new BulkReply(Utils.stringToBytes("id"));
        reply[7] = new IntegerReply(194L);
        reply[8] = new BulkReply(Utils.stringToBytes("mode"));
        reply[9] = new BulkReply(Utils.stringToBytes(ServerConf.proxyMode().name()));
        reply[10] = new BulkReply(Utils.stringToBytes("role"));
        reply[11] = new BulkReply(Utils.stringToBytes("master"));
        reply[12] = new BulkReply(Utils.stringToBytes("modules"));
        reply[13] = MultiBulkReply.EMPTY;
        return new MultiBulkReply(reply);
    }

}
