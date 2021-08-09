package com.netease.nim.camellia.redis.proxy.command.async.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.reply.*;


/**
 * Created by caojiajun on 2021/8/6
 */
public class Converters {

    public StringConverter stringConverter;

    public Converters(ConverterConfig converterConfig) {
        this.stringConverter = converterConfig.getStringConverter();
    }

    public void convertRequest(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return;
        if (redisCommand.getType() != RedisCommand.Type.WRITE) return;
        RedisCommand.CommandType commandType = redisCommand.getCommandType();
        if (commandType == null) return;
        CommandContext commandContext = command.getCommandContext();
        if (commandType == RedisCommand.CommandType.STRING) {
            stringConvertRequest(redisCommand, commandContext, command);
        }
    }

    public void convertReply(Command command, Reply reply) {
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return;
        if (redisCommand.getType() != RedisCommand.Type.READ) return;
        RedisCommand.CommandType commandType = redisCommand.getCommandType();
        if (commandType == null) return;
        if (reply == null) return;
        if (reply instanceof ErrorReply) return;
        CommandContext commandContext = command.getCommandContext();
        if (commandType == RedisCommand.CommandType.STRING) {
            stringConvertReply(redisCommand, commandContext, command, reply);
        }
    }

    private void stringConvertReply(RedisCommand redisCommand, CommandContext commandContext, Command command, Reply reply) {
        if (stringConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case GET:
                if (reply instanceof BulkReply) {
                    byte[] raw = ((BulkReply) reply).getRaw();
                    if (raw != null) {
                        byte[] bytes = stringConverter.valueReverseConvert(commandContext, objects[1], raw);
                        ((BulkReply) reply).updateRaw(bytes);
                    }
                }
                break;
            case MGET:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies != null) {
                        int i=1;
                        for (Reply reply1 : replies) {
                            if (reply1 instanceof BulkReply) {
                                byte[] raw = ((BulkReply) reply1).getRaw();
                                if (raw != null) {
                                    byte[] bytes = stringConverter.valueReverseConvert(commandContext, objects[i], raw);
                                    ((BulkReply) reply1).updateRaw(bytes);
                                }
                            }
                            i ++;
                        }
                    }
                }
                break;
        }
    }

    private void stringConvertRequest(RedisCommand redisCommand, CommandContext commandContext, Command command) {
        if (stringConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case SET:
            case SETNX:
            case GETSET:
                if (objects.length >= 3) {
                    byte[] convertedValue = stringConverter.valueConvert(commandContext, objects[1], objects[2]);
                    if (convertedValue != null) {
                        objects[2] = convertedValue;
                    }
                }
                break;
            case SETEX:
            case PSETEX:
                if (objects.length >= 4) {
                    byte[] convertedValue = stringConverter.valueConvert(commandContext, objects[1], objects[3]);
                    if (convertedValue != null) {
                        objects[3] = convertedValue;
                    }
                }
                break;
            case MSET:
            case MSETNX:
                for (int i=2; i<objects.length; i+=2) {
                    byte[] convertedValue = stringConverter.valueConvert(commandContext, objects[i-1], objects[i]);
                    if (convertedValue != null) {
                        objects[i] = convertedValue;
                    }
                }
                break;
        }
    }
}
