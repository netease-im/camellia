package com.netease.nim.camellia.redis.proxy.command;


import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.netty.ChannelInfo;
import com.netease.nim.camellia.redis.proxy.util.KeyParser;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.List;

public class Command {

    private final byte[][] objects;
    private String name;
    private RedisCommand redisCommand;
    private ChannelInfo channelInfo;
    private boolean hasCheckBlocking = false;
    private boolean blocking = false;
    private List<byte[]> keys = null;

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

    public List<byte[]> getKeys() {
        if (keys != null) return keys;
        keys = KeyParser.findKeys(this);
        return keys;
    }

    public boolean isBlocking() {
        RedisCommand redisCommand = getRedisCommand();
        if (redisCommand == null) return false;
        if (redisCommand.isBlocking()) {
            return true;
        }
        if (hasCheckBlocking) {
            return blocking;
        }
        if (redisCommand == RedisCommand.XREAD || redisCommand == RedisCommand.XREADGROUP) {
            for (byte[] object : getObjects()) {
                String string = new String(object, Utils.utf8Charset);
                if (string.equalsIgnoreCase(RedisKeyword.BLOCK.name())) {
                    blocking = true;
                    break;
                }
            }
        }
        hasCheckBlocking = true;
        return blocking;
    }

    public ChannelInfo getChannelInfo() {
        return channelInfo;
    }

    public void setChannelInfo(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
    }

    public String toParamsStr() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (byte[] object : objects) {
            builder.append(Utils.bytesToString(object)).append(" ");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");
        return builder.toString();
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
