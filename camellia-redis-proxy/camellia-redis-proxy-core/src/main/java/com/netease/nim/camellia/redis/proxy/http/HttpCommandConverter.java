package com.netease.nim.camellia.redis.proxy.http;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Created by caojiajun on 2024/1/15
 */
public class HttpCommandConverter {

    public static final char singleQuotation = '\'';
    public static final char doubleQuotation = '\"';
    public static final char blank = ' ';

    public static List<Command> convert(HttpCommandRequest request) {
        List<String> commands = request.getCommands();
        List<Command> list = new ArrayList<>(commands.size());
        for (String command : commands) {
            list.add(convert(command, request.isRequestBase64()));
        }
        return list;
    }

    public static Command convert(String command, boolean base64) {
        if (base64) {
            String[] split = command.split(" ");
            List<byte[]> list = new ArrayList<>(split.length);
            boolean first = true;
            for (String str : split) {
                str = str.trim();
                if (str.isEmpty()) {
                    continue;
                }
                if (first) {
                    list.add(Utils.stringToBytes(str));
                    first = false;
                } else {
                    byte[] bytes = Base64.getDecoder().decode(str);
                    list.add(bytes);
                }
            }
            return new Command(list.toArray(new byte[0][0]));
        }
        if (!command.contains("'") && !command.contains("\"")) {
            String[] split = command.split(" ");
            List<byte[]> list = new ArrayList<>(split.length);
            for (String str : split) {
                str = str.trim();
                if (str.isEmpty()) {
                    continue;
                }
                list.add(Utils.stringToBytes(str));
            }
            return new Command(list.toArray(new byte[0][0]));
        } else {
            List<String> args = new ArrayList<>();
            boolean inArg = false;
            StringBuilder arg = new StringBuilder();
            Character quotation = null;
            int index = 0;
            char[] array = command.toCharArray();
            for (char c : array) {
                if (c == singleQuotation || c == doubleQuotation) {
                    if (inArg) {
                        if (quotation != null && quotation == c) {
                            boolean lastChar = index == (array.length - 1);
                            if (!lastChar) {
                                boolean nextCharBlack = array[index + 1] == blank;
                                if (!nextCharBlack) {
                                    throw new IllegalArgumentException("Invalid argument(s)");
                                }
                            }
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
                index ++;
            }
            if (quotation != null) {
                throw new IllegalArgumentException("Invalid argument(s)");
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
