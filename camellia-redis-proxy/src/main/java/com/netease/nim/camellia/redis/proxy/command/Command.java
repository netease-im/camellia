package com.netease.nim.camellia.redis.proxy.command;


import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.util.Utils;

public class Command {

    private final byte[][] objects;
    private String name;
    private RedisCommand redisCommand;

    public Command(byte[][] objects) {
        this.objects = objects;
    }

    public String getName() {
        if (name != null) return name;
        if (objects != null && objects.length > 0) {
            name = new String(objects[0], Utils.utf8Charset).toLowerCase();
        }
        return name;
    }

    public RedisCommand getRedisCommand() {
        if (redisCommand != null) return redisCommand;
        this.redisCommand = RedisCommand.getSupportRedisCommandByName(getName());
        return redisCommand;
    }

    public byte[][] getObjects() {
        return objects;
    }

    public void fillParameters(Class<?>[] parameterTypes, Object[] parameters) {
        int position = 0;
        for (Class<?> type : parameterTypes) {
            if (type == byte[].class) {
                if (position >= parameters.length) {
                    throw new IllegalArgumentException("wrong number of arguments for '" + getName() + "' command");
                }
                if (objects.length - 1 > position) {
                    parameters[position] = objects[1 + position];
                }
            } else {
                int left = objects.length - position - 1;
                byte[][] lastArgument = new byte[left][];
                for (int i = 0; i < left; i++) {
                    lastArgument[i] = objects[i + position + 1];
                }
                parameters[position] = lastArgument;
            }
            position++;
        }
    }
}
