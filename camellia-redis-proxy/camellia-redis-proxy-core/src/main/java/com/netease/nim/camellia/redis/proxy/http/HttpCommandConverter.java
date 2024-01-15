package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by caojiajun on 2024/1/15
 */
public class HttpCommandConverter {

    public static final char singleQuotation = '\'';
    public static final char doubleQuotation = '\"';
    public static final char blank = ' ';

    public static List<Command> toCommands(HttpCommandRequest request) {
        List<String> commands = request.getCommands();
        List<Command> list = new ArrayList<>();
        for (String command : commands) {
            list.add(toCommand(command));
        }
        return list;
    }

    public static Command toCommand(String command) {
        if (!command.contains("'") && !command.contains("\"")) {
            String[] split = command.split(" ");
            byte[][] args = new byte[split.length][];
            for (int i=0; i<split.length; i++) {
                args[i] = Utils.stringToBytes(split[i]);
            }
            return new Command(args);
        } else {
            List<String> args = new ArrayList<>();
            boolean inArg = false;
            StringBuilder arg = new StringBuilder();
            Character quotation = null;
            for (char c : command.toCharArray()) {
                if (c == singleQuotation || c == doubleQuotation) {
                    if (inArg) {
                        if (quotation != null && quotation == c) {
                            quotation = null;
                            inArg = false;
                            args.add(arg.toString());
                            arg = new StringBuilder();
                        } else {
                            arg.append(c);
                        }
                    } else {
                        quotation = c;
                        inArg = true;
                    }
                } else if (blank == c) {
                    if (inArg) {
                        if (quotation != null) {
                            arg.append(c);
                        } else {
                            inArg = false;
                            args.add(arg.toString());
                            arg = new StringBuilder();
                        }
                    }
                } else {
                    arg.append(c);
                    inArg = true;
                }
            }
            if (arg.length() > 0) {
                args.add(arg.toString());
            }
            byte[][] bytes = new byte[args.size()][];
            for (int i=0; i<args.size(); i++) {
                bytes[i] = Utils.stringToBytes(args.get(i));
            }
            return new Command(bytes);
        }
    }
}
