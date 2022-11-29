package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class KeyParser {

    public static List<byte[]> findKeys(Command command) {
        if (command == null || command.getRedisCommand() == null) return Collections.emptyList();
        RedisCommand redisCommand = command.getRedisCommand();
        RedisCommand.CommandKeyType commandKeyType = redisCommand.getCommandKeyType();
        if (commandKeyType == null) {
            return Collections.emptyList();
        }
        if (commandKeyType == RedisCommand.CommandKeyType.None) return Collections.emptyList();
        if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_SINGLE && command.getObjects().length >= 2) {
            return Collections.singletonList(command.getObjects()[1]);
        } else if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_MULTI && command.getObjects().length >= 2) {
            List<byte[]> keys = new ArrayList<>(command.getObjects().length - 1);
            byte[][] objects = command.getObjects();
            for (int i=1; i<objects.length; i++) {
                keys.add(objects[i]);
            }
            return keys;
        } else if (commandKeyType == RedisCommand.CommandKeyType.COMPLEX) {
            List<byte[]> keys = new ArrayList<>();
            byte[][] objects = command.getObjects();
            switch (redisCommand) {
                case MSET:
                case MSETNX:
                    msetOrMsetNx(command, keys);
                    break;
                case EVAL:
                case EVALSHA:
                    evalOrEvalSha(command, keys);
                    break;
                case RENAME:
                case RENAMENX:
                case GEOSEARCHSTORE:
                case SMOVE:
                case LMOVE:
                case ZRANGESTORE:
                case BLMOVE:
                    if (command.getObjects().length >= 3) {
                        dynamicKey(command, keys, 1, 2);
                    }
                    break;
                case ZINTERSTORE:
                case ZUNIONSTORE:
                case ZDIFFSTORE:
                case EXZINTERSTORE:
                case EXZUNIONSTORE:
                case EXZDIFFSTORE:
                    if (objects.length >= 4) {
                        int keyCount = (int) Utils.bytesToNum(objects[2]);
                        if (keyCount > 0) {
                            keys.add(objects[1]);
                            dynamicKey(command, keys, 3, 3 + keyCount - 1);
                        }
                    }
                    break;
                case ZDIFF:
                case ZUNION:
                case ZINTER:
                case ZINTERCARD:
                case EXZDIFF:
                case EXZUNION:
                case EXZINTER:
                case EXZINTERCARD:
                    if (objects.length >= 3) {
                        int keyCount = (int) Utils.bytesToNum(objects[1]);
                        if (keyCount > 0) {
                            dynamicKey(command, keys, 2, 2 + keyCount - 1);
                        }
                    }
                    break;
                case BITOP:
                    if (objects.length >= 4) {
                        dynamicKey(command, keys, 2, objects.length - 1);
                    }
                    break;
                case BLPOP:
                case BRPOP:
                case BRPOPLPUSH:
                case BZPOPMAX:
                case BZPOPMIN:
                case EXBZPOPMAX:
                case EXBZPOPMIN:
                    dynamicKey(command, keys, 1, objects.length - 2);
                    break;
                case XREAD:
                case XREADGROUP:
                    int index = -1;
                    for (int i=1; i<objects.length; i++) {
                        String string = new String(objects[i], Utils.utf8Charset);
                        if (string.equalsIgnoreCase(RedisKeyword.STREAMS.name())) {
                            index = i;
                            break;
                        }
                    }
                    if (index == -1) {
                        break;
                    }
                    int last = objects.length - index - 1;
                    if (last <= 0) {
                        break;
                    }
                    if (last % 2 != 0) {
                        break;
                    }
                    int keyCount = last / 2;
                    dynamicKey(command, keys, index + 1, index + keyCount);
                    break;
                case XINFO:
                case XGROUP:
                    if (objects.length >= 3) {
                        keys.add(objects[2]);
                    }
                    break;
                case JSON_MGET:
                    if (objects.length >= 3) {
                        dynamicKey(command, keys, 1, objects.length - 2);
                    }
                    break;
                default:
                    break;
            }
            return keys;
        } else {
            return Collections.emptyList();
        }
    }

    private static void dynamicKey(Command command, List<byte[]> keys, int start, int end) {
        byte[][] objects = command.getObjects();
        for (int i=start; i<=end; i++) {
            byte[] key = objects[i];
            keys.add(key);
        }
    }

    private static void msetOrMsetNx(Command command, List<byte[]> keys) {
        byte[][] objects = command.getObjects();
        for (int i=1; i<objects.length; i+=2) {
            keys.add(objects[i]);
        }
    }

    private static void evalOrEvalSha(Command command, List<byte[]> keys) {
        byte[][] objects = command.getObjects();
        long keyCount = Utils.bytesToNum(objects[2]);
        if (keyCount == 1) {
            if (objects.length < 4) {
                return;
            }
            byte[] key = objects[3];
            keys.add(key);
        } else if (keyCount > 1) {
            if (objects.length < 3 + keyCount) {
                return;
            }
            for (int i=3; i<3+keyCount; i++) {
                byte[] key = objects[i];
                keys.add(key);
            }
        }
    }
}
