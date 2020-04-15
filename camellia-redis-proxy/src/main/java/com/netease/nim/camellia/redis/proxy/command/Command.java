package com.netease.nim.camellia.redis.proxy.command;


import com.netease.nim.camellia.redis.proxy.util.Utils;

public class Command {

    private byte[][] objects;
    private String name;

    public Command(byte[][] objects) {
        this.objects = objects;
        if (objects != null && objects.length > 0) {
            this.name = new String(objects[0], Utils.utf8Charset).toLowerCase();
        } else {
            this.name = null;
        }
    }

    public String getName() {
        return name;
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
