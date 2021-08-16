package com.netease.nim.camellia.redis.proxy.command.async.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.async.CommandContext;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;


/**
 * Created by caojiajun on 2021/8/6
 */
public class Converters {

    private final StringConverter stringConverter;
    private final SetConverter setConverter;
    private final ListConverter listConverter;
    private final HashConverter hashConverter;
    private final ZSetConverter zSetConverter;

    public Converters(ConverterConfig converterConfig) {
        this.stringConverter = converterConfig.getStringConverter();
        this.setConverter = converterConfig.getSetConverter();
        this.listConverter = converterConfig.getListConverter();
        this.hashConverter = converterConfig.getHashConverter();
        this.zSetConverter = converterConfig.getzSetConverter();
    }

    public void convertRequest(Command command) {
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return;
        RedisCommand.CommandType commandType = redisCommand.getCommandType();
        if (commandType == null) return;
        CommandContext commandContext = command.getCommandContext();
        if (commandType == RedisCommand.CommandType.STRING) {
            stringConvertRequest(redisCommand, commandContext, command);
        } else if (commandType == RedisCommand.CommandType.SET) {
            setConvertRequest(redisCommand, commandContext, command);
        } else if (commandType == RedisCommand.CommandType.LIST) {
            listConvertRequest(redisCommand, commandContext, command);
        } else if (commandType == RedisCommand.CommandType.HASH) {
            hashConvertRequest(redisCommand, commandContext, command);
        } else if (commandType == RedisCommand.CommandType.ZSET) {
            zsetConvertRequest(redisCommand, commandContext, command);
        }
    }

    public void convertReply(Command command, Reply reply) {
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return;
        RedisCommand.CommandType commandType = redisCommand.getCommandType();
        if (commandType == null) return;
        if (reply == null) return;
        if (reply instanceof ErrorReply) return;
        CommandContext commandContext = command.getCommandContext();
        if (commandType == RedisCommand.CommandType.STRING) {
            stringConvertReply(redisCommand, commandContext, command, reply);
        } else if (commandType == RedisCommand.CommandType.SET) {
            setConvertReply(redisCommand, commandContext, command, reply);
        } else if (commandType == RedisCommand.CommandType.LIST) {
            listConvertReply(redisCommand, commandContext, command, reply);
        } else if (commandType == RedisCommand.CommandType.HASH) {
            hashConvertReply(redisCommand, commandContext, command, reply);
        } else if (commandType == RedisCommand.CommandType.ZSET) {
            zsetConvertReply(redisCommand, commandContext, command, reply);
        }
    }

    private void stringConvertReply(RedisCommand redisCommand, CommandContext commandContext, Command command, Reply reply) {
        if (stringConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case GET:
            case GETSET:
            case GETDEL:
            case GETEX:
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

    private void setConvertRequest(RedisCommand redisCommand, CommandContext commandContext, Command command) {
        if (setConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case SADD:
            case SREM:
            case SISMEMBER:
            case SMISMEMBER:
                if (objects.length >= 2) {
                    for (int i=2; i<objects.length; i++) {
                        byte[] convertedValue = setConverter.valueConvert(commandContext, objects[1], objects[i]);
                        if (convertedValue != null) {
                            objects[i] = convertedValue;
                        }
                    }
                }
                break;
        }
    }

    private void setConvertReply(RedisCommand redisCommand, CommandContext commandContext, Command command, Reply reply) {
        if (setConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case SMEMBERS:
                if (reply instanceof MultiBulkReply) {
                    updateSetMultiBulkReply(commandContext, objects[1], (MultiBulkReply) reply);
                }
                break;
            case SPOP:
            case SRANDMEMBER:
                if (reply instanceof MultiBulkReply) {
                    updateSetMultiBulkReply(commandContext, objects[1], (MultiBulkReply) reply);
                } else if (reply instanceof BulkReply) {
                    byte[] raw = ((BulkReply) reply).getRaw();
                    byte[] bytes = setConverter.valueReverseConvert(commandContext, objects[1], raw);
                    ((BulkReply) reply).updateRaw(bytes);
                }
                break;
            case SSCAN:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies.length == 2) {
                        Reply reply1 = replies[1];
                        if (reply1 instanceof MultiBulkReply) {
                            updateSetMultiBulkReply(commandContext, objects[1], (MultiBulkReply) reply1);
                        }
                    }
                }
                break;
        }
    }

    private void updateSetMultiBulkReply(CommandContext commandContext, byte[] key, MultiBulkReply multiBulkReply) {
        if (multiBulkReply == null) return;
        Reply[] replies = multiBulkReply.getReplies();
        if (replies == null) return;
        for (Reply reply : replies) {
            if (reply instanceof BulkReply) {
                byte[] raw = ((BulkReply) reply).getRaw();
                byte[] bytes = setConverter.valueReverseConvert(commandContext, key, raw);
                ((BulkReply) reply).updateRaw(bytes);
            }
        }
    }

    private void listConvertRequest(RedisCommand redisCommand, CommandContext commandContext, Command command) {
        if (listConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case LINSERT:
                if (objects.length == 5) {
                    byte[] bytes1 = listConverter.valueConvert(commandContext, objects[1], objects[3]);
                    objects[3] = bytes1;
                    byte[] bytes2 = listConverter.valueConvert(commandContext, objects[1], objects[4]);
                    objects[4] = bytes2;
                }
                break;
            case LPOS:
                if (objects.length >= 3) {
                    byte[] bytes1 = listConverter.valueConvert(commandContext, objects[1], objects[2]);
                    objects[2] = bytes1;
                }
                break;
            case LPUSH:
            case LPUSHX:
            case RPUSH:
            case RPUSHX:
                if (objects.length >= 3) {
                    for (int i=2; i<objects.length; i++) {
                        byte[] bytes1 = listConverter.valueConvert(commandContext, objects[1], objects[i]);
                        objects[i] = bytes1;
                    }
                }
                break;
            case LSET:
            case LREM:
                if (objects.length == 4) {
                    byte[] bytes1 = listConverter.valueConvert(commandContext, objects[1], objects[3]);
                    objects[3] = bytes1;
                }
                break;
        }
    }

    public void listConvertReply(RedisCommand redisCommand, CommandContext commandContext, Command command, Reply reply) {
        if (listConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case RPOP:
            case LPOP: {
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies != null) {
                        for (Reply reply1 : replies) {
                            if (reply1 instanceof BulkReply) {
                                byte[] bytes = listConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) reply1).getRaw());
                                ((BulkReply) reply1).updateRaw(bytes);
                            }
                        }
                    }
                } else if (reply instanceof BulkReply) {
                    byte[] bytes = listConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) reply).getRaw());
                    ((BulkReply) reply).updateRaw(bytes);
                }
                break;
            }
            case BRPOP:
            case BLPOP:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies != null && replies.length == 2) {
                        Reply reply1 = replies[0];
                        Reply reply2 = replies[1];
                        if (reply1 instanceof BulkReply && reply2 instanceof BulkReply) {
                            byte[] bytes = listConverter.valueReverseConvert(commandContext, ((BulkReply) reply1).getRaw(), ((BulkReply) reply2).getRaw());
                            ((BulkReply) reply2).updateRaw(bytes);
                        }
                    }
                }
            case LINDEX:
                if (reply instanceof BulkReply) {
                    byte[] bytes = listConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) reply).getRaw());
                    ((BulkReply) reply).updateRaw(bytes);
                }
                break;
            case LRANGE:
                if (reply instanceof MultiBulkReply) {
                    for (Reply reply1 : ((MultiBulkReply) reply).getReplies()) {
                        if (reply1 instanceof BulkReply) {
                            byte[] bytes = listConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) reply1).getRaw());
                            ((BulkReply) reply1).updateRaw(bytes);
                        }
                    }
                }
                break;
        }
    }

    private void hashConvertRequest(RedisCommand redisCommand, CommandContext commandContext, Command command) {
        if (hashConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case HDEL:
            case HEXISTS:
            case HINCRBY:
            case HINCRBYFLOAT:
            case HGET:
            case HMGET:
                if (objects.length >= 3) {
                    for (int i=2; i<objects.length; i++) {
                        byte[] bytes = hashConverter.fieldConvert(commandContext, objects[1], objects[i]);
                        objects[i] = bytes;
                    }
                }
                break;
            case HSET:
            case HSETNX:
            case HMSET:
                if (objects.length >= 4 && objects.length % 2 == 0) {
                    for (int i=2; i<objects.length; i+=2) {
                        byte[] field = objects[i];
                        byte[] value = objects[i+1];
                        byte[] bytes1 = hashConverter.fieldConvert(commandContext, objects[1], field);
                        byte[] bytes2 = hashConverter.valueConvert(commandContext, objects[1], value);
                        objects[i] = bytes1;
                        objects[i+1] = bytes2;
                    }
                }
                break;
        }
    }

    public void hashConvertReply(RedisCommand redisCommand, CommandContext commandContext, Command command, Reply reply) {
        if (hashConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case HGET:
                if (reply instanceof BulkReply) {
                    byte[] raw = ((BulkReply) reply).getRaw();
                    if (raw != null) {
                        byte[] bytes = hashConverter.valueReverseConvert(commandContext, objects[1], raw);
                        ((BulkReply) reply).updateRaw(bytes);
                    }
                }
                break;
            case HVALS:
            case HMGET:
                if (reply instanceof MultiBulkReply) {
                    updateHashValues(commandContext, objects[1], (MultiBulkReply) reply);
                }
                break;
            case HGETALL:
                if (reply instanceof MultiBulkReply) {
                    updateHashKVs(commandContext, objects[1], (MultiBulkReply) reply);
                }
                break;
            case HKEYS:
                if (reply instanceof MultiBulkReply) {
                    updateHashFields(commandContext, objects[1], (MultiBulkReply) reply);
                }
                break;
            case HRANDFIELD:
                if (reply instanceof BulkReply) {
                    byte[] raw = ((BulkReply) reply).getRaw();
                    byte[] bytes = hashConverter.fieldReverseConvert(commandContext, objects[1], raw);
                    ((BulkReply) reply).updateRaw(bytes);
                } else if (reply instanceof MultiBulkReply) {
                    updateHashFields(commandContext, objects[1], (MultiBulkReply) reply);
                }
                break;
            case HSCAN:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies.length == 2) {
                        Reply reply1 = replies[1];
                        if (reply1 instanceof MultiBulkReply) {
                            updateHashKVs(commandContext, objects[1], (MultiBulkReply) reply1);
                        }
                    }
                }
                break;
        }
    }

    private void updateHashKVs(CommandContext commandContext, byte[] key, MultiBulkReply reply) {
        Reply[] replies = reply.getReplies();
        if (replies == null) return;
        if (replies.length % 2 != 0) return;
        for (int i=0; i<replies.length; i+=2) {
            Reply field = reply.getReplies()[i];
            Reply value = reply.getReplies()[i + 1];
            if (field instanceof BulkReply) {
                byte[] raw = ((BulkReply) field).getRaw();
                byte[] bytes = hashConverter.fieldReverseConvert(commandContext, key, raw);
                ((BulkReply) field).updateRaw(bytes);
            }
            if (value instanceof BulkReply) {
                byte[] raw = ((BulkReply) value).getRaw();
                byte[] bytes = hashConverter.valueReverseConvert(commandContext, key, raw);
                ((BulkReply) value).updateRaw(bytes);
            }
        }
    }

    private void updateHashValues(CommandContext commandContext, byte[] key, MultiBulkReply reply) {
        for (Reply reply1 : reply.getReplies()) {
            if (reply1 instanceof BulkReply) {
                byte[] raw = ((BulkReply) reply1).getRaw();
                byte[] bytes = hashConverter.valueReverseConvert(commandContext, key, raw);
                ((BulkReply) reply1).updateRaw(bytes);

            }
        }
    }

    private void updateHashFields(CommandContext commandContext, byte[] key, MultiBulkReply reply) {
        for (Reply reply1 : reply.getReplies()) {
            if (reply1 instanceof BulkReply) {
                byte[] raw = ((BulkReply) reply1).getRaw();
                byte[] bytes = hashConverter.fieldReverseConvert(commandContext, key, raw);
                ((BulkReply) reply1).updateRaw(bytes);
            }
        }
    }

    private void zsetConvertRequest(RedisCommand redisCommand, CommandContext commandContext, Command command) {
        if (zSetConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case ZADD:
                if (objects.length == 4) {
                    byte[] bytes = zSetConverter.valueConvert(commandContext, objects[1], objects[3]);
                    objects[3] = bytes;
                } if (objects.length >= 4) {
                    for (int i=2; i<objects.length-1; i++) {
                        byte[] object = objects[i];
                        String s = Utils.bytesToString(object);
                        if (s.equalsIgnoreCase("NX") || s.equalsIgnoreCase("XX")
                                || s.equalsIgnoreCase("GT") || s.equalsIgnoreCase("LT")
                                || s.equalsIgnoreCase("CH") || s.equalsIgnoreCase("INCR")) {
                            continue;
                        }
                        byte[] member = objects[i+1];
                        byte[] bytes = zSetConverter.valueConvert(commandContext, objects[1], member);
                        objects[i+1] = bytes;
                        i++;
                    }
                }
                break;
            case ZINCRBY:
                if (objects.length == 4) {
                    byte[] bytes = zSetConverter.valueConvert(commandContext, objects[1], objects[3]);
                    objects[3] = bytes;
                }
                break;
            case ZMSCORE:
            case ZSCORE:
            case ZRANK:
            case ZREM:
            case ZREVRANK:
                if (objects.length >= 3) {
                    for (int i=2; i<objects.length; i++) {
                        byte[] bytes = zSetConverter.valueConvert(commandContext, objects[1], objects[i]);
                        objects[i] = bytes;
                    }
                }
                break;
        }
    }

    public void zsetConvertReply(RedisCommand redisCommand, CommandContext commandContext, Command command, Reply reply) {
        if (zSetConverter == null) return;
        byte[][] objects = command.getObjects();
        switch (redisCommand) {
            case BZPOPMAX:
            case BZPOPMIN:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies.length == 3) {
                        Reply reply1 = replies[0];
                        Reply reply2 = replies[1];
                        if (reply1 instanceof BulkReply && reply2 instanceof BulkReply) {
                            byte[] bytes = zSetConverter.valueReverseConvert(commandContext, ((BulkReply) reply1).getRaw(), ((BulkReply) reply2).getRaw());
                            ((BulkReply) reply2).updateRaw(bytes);
                        }
                    }
                }
                break;
            case ZPOPMIN:
            case ZPOPMAX:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies != null && replies.length >= 2) {
                        for (int i=0; i<replies.length; i+=2) {
                            Reply reply1 = replies[i];
                            if (reply1 instanceof BulkReply) {
                                byte[] bytes = zSetConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) reply1).getRaw());
                                ((BulkReply) reply1).updateRaw(bytes);
                            }
                        }
                    }
                }
                break;
            case ZRANDMEMBER:
            case ZRANGE:
            case ZRANGEBYLEX:
            case ZRANGEBYSCORE:
            case ZREVRANGE:
            case ZREVRANGEBYLEX:
            case ZREVRANGEBYSCORE:
                if (reply instanceof BulkReply) {
                    byte[] bytes = zSetConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) reply).getRaw());
                    ((BulkReply) reply).updateRaw(bytes);
                } else if (reply instanceof MultiBulkReply) {
                    boolean withScores = withScores(objects);
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (withScores) {
                        updateZSetWithScores(commandContext, objects[1], (MultiBulkReply) reply);
                    } else {
                        if (replies != null) {
                            for (Reply member : replies) {
                                byte[] bytes = zSetConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) member).getRaw());
                                ((BulkReply) member).updateRaw(bytes);
                            }
                        }
                    }
                }
                break;
            case ZSCAN:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies.length == 2) {
                        Reply reply1 = replies[1];
                        if (reply1 instanceof MultiBulkReply) {
                            updateZSetWithScores(commandContext, objects[1], (MultiBulkReply) reply1);
                        }
                    }
                }
                break;
        }
    }

    private void updateZSetWithScores(CommandContext commandContext, byte[] key, MultiBulkReply multiBulkReply) {
        Reply[] replies = multiBulkReply.getReplies();
        if (replies != null && replies.length % 2 == 0) {
            for (int i=0; i<replies.length; i+=2) {
                Reply member = replies[i];
                byte[] bytes = zSetConverter.valueReverseConvert(commandContext, key, ((BulkReply) member).getRaw());
                ((BulkReply) member).updateRaw(bytes);
            }
        }
    }

    private boolean withScores(byte[][] objects) {
        for (int i=2; i<objects.length; i++) {
            String s = Utils.bytesToString(objects[i]);
            if (s.equalsIgnoreCase(RedisKeyword.WITHSCORES.name())) {
                return true;
            }
        }
        return false;
    }


}
