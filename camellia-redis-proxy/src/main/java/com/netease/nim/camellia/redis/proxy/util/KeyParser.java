package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * Created by caojiajun on 2020/10/22
 */
public class KeyParser {

    private static final Logger logger = LoggerFactory.getLogger(KeyParser.class);

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
            switch (redisCommand) {
                case MSET:
                case MSETNX:
                    msetOrMsetNx(command, keys);
                    break;
                case EVAL:
                case EVALSHA:
                    evalOrEvalSha(command, keys);
                    break;
                case PFCOUNT:
                case SDIFF:
                case SINTER:
                case SUNION:
                case PFMERGE:
                case SINTERSTORE:
                case SUNIONSTORE:
                case SDIFFSTORE:
                case RPOPLPUSH:
                    if (command.getObjects().length >= 2) {
                        dynamicKey(command, keys, 1, command.getObjects().length - 1);
                    }
                    break;
                case RENAME:
                case RENAMENX:
                case SMOVE:
                    if (command.getObjects().length >= 3) {
                        dynamicKey(command, keys, 1, 2);
                    }
                    break;
                default:
                    logger.warn("unknown keys of {}", redisCommand);
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
            for (int i=4; i<3+keyCount; i++) {
                byte[] key = objects[i];
                keys.add(key);
            }
        }
    }
}
