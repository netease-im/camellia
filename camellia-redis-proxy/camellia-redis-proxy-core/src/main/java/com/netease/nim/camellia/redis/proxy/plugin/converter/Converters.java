package com.netease.nim.camellia.redis.proxy.plugin.converter;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.command.CommandContext;
import com.netease.nim.camellia.redis.proxy.upstream.utils.PubSubUtils;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.Utils;


/**
 * Created by caojiajun on 2021/8/6
 */
public class Converters {

    private final KeyConverter keyConverter;
    private final StringConverter stringConverter;
    private final SetConverter setConverter;
    private final ListConverter listConverter;
    private final HashConverter hashConverter;
    private final ZSetConverter zSetConverter;

    public Converters(ConverterConfig converterConfig) {
        this.keyConverter = converterConfig.getKeyConverter();
        this.stringConverter = converterConfig.getStringConverter();
        this.setConverter = converterConfig.getSetConverter();
        this.listConverter = converterConfig.getListConverter();
        this.hashConverter = converterConfig.getHashConverter();
        this.zSetConverter = converterConfig.getzSetConverter();
    }

    public KeyConverter getKeyConverter() {
        return keyConverter;
    }

    public StringConverter getStringConverter() {
        return stringConverter;
    }

    public SetConverter getSetConverter() {
        return setConverter;
    }

    public ListConverter getListConverter() {
        return listConverter;
    }

    public HashConverter getHashConverter() {
        return hashConverter;
    }

    public ZSetConverter getzSetConverter() {
        return zSetConverter;
    }

    public void convertRequest(Command command) {
        if (command == null) return;
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return;
        RedisCommand.CommandType commandType = redisCommand.getCommandType();
        if (commandType == null) return;
        CommandContext commandContext = command.getCommandContext();
        keyConvert(redisCommand, commandContext, command, true);
        if (keyConverter != null) {
            command.clearKeysCache();
        }
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
        if (command == null) return;
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
        keyConvertReply(redisCommand, commandContext, command, reply);
        keyConvert(redisCommand, commandContext, command, false);
        if (keyConverter != null) {
            command.clearKeysCache();
        }
    }

    private byte[] convert(CommandContext commandContext, RedisCommand redisCommand, byte[] key, boolean convert) {
        if (convert) {
            return keyConverter.convert(commandContext, redisCommand, key);
        } else {
            return keyConverter.reverseConvert(commandContext, redisCommand, key);
        }
    }

    private void keyConvert(RedisCommand redisCommand, CommandContext commandContext, Command command, boolean convert) {
        if (keyConverter == null) return;
        byte[][] objects = command.getObjects();
        RedisCommand.CommandKeyType commandKeyType = redisCommand.getCommandKeyType();
        if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_SINGLE) {
            if (objects.length >= 2) {
                byte[] convertedKey = convert(commandContext, redisCommand, objects[1], convert);
                objects[1] = convertedKey;
            }
        } else if (commandKeyType == RedisCommand.CommandKeyType.SIMPLE_MULTI) {
            if (objects.length >= 2) {
                for (int i = 1; i < objects.length; i++) {
                    byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                    objects[i] = convertedKey;
                }
            }
        } else if (commandKeyType == RedisCommand.CommandKeyType.COMPLEX) {
            switch (redisCommand) {
                case MSETEX:
                    convertCountedKeys(redisCommand, commandContext, objects, 1, 2, 2, false, convert);
                    break;
                case CMS_MERGE:
                case TDIGEST_MERGE:
                    convertCountedKeys(redisCommand, commandContext, objects, 2, 3, 1, true, convert);
                    break;
                case SDIFFCARD:
                case SUNIONCARD:
                case TS_NRANGE:
                case TS_NREVRANGE:
                    convertCountedKeys(redisCommand, commandContext, objects, 1, 2, 1, false, convert);
                    break;
                case JSON_MSET:
                case TS_MADD:
                    if (objects.length >= 4 && (objects.length - 1) % 3 == 0) {
                        for (int i = 1; i < objects.length; i += 3) {
                            objects[i] = convert(commandContext, redisCommand, objects[i], convert);
                        }
                    }
                    break;
                case MSET:
                case MSETNX:
                    if (objects.length >= 3 && objects.length % 2 == 1) {
                        for (int i = 1; i < objects.length; i += 2) {
                            byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                            objects[i] = convertedKey;
                        }
                    }
                    break;
                case EVAL:
                case EVALSHA:
                case EVAL_RO:
                case EVALSHA_RO:
                case FCALL:
                case FCALL_RO:
                case TFCALL:
                case TFCALLASYNC:
                    long keyCount = Utils.bytesToNum(objects[2]);
                    if (keyCount == 1) {
                        if (objects.length < 4) {
                            return;
                        }
                        byte[] convertedKey = convert(commandContext, redisCommand, objects[3], convert);
                        objects[3] = convertedKey;
                    } else if (keyCount > 1) {
                        if (objects.length < 3 + keyCount) {
                            return;
                        }
                        for (int i = 3; i < 3 + keyCount; i++) {
                            byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                            objects[i] = convertedKey;
                        }
                    }
                    break;
                case RENAME:
                case RENAMENX:
                case COPY:
                case GEOSEARCHSTORE:
                case SMOVE:
                case LMOVE:
                case ZRANGESTORE:
                case BLMOVE:
                case LMOVEM:
                case BLMOVEM:
                case ZMOVE:
                case BZMOVE:
                case ZMOVEMEMBER:
                case TS_CREATERULE:
                case TS_DELETERULE:
                case LCS:
                    if (command.getObjects().length >= 3) {
                        byte[] convertedKey1 = convert(commandContext, redisCommand, objects[1], convert);
                        objects[1] = convertedKey1;
                        byte[] convertedKey2 = convert(commandContext, redisCommand, objects[2], convert);
                        objects[2] = convertedKey2;
                    }
                    break;
                case ZINTERSTORE:
                case ZUNIONSTORE:
                case ZDIFFSTORE:
                case EXZINTERSTORE:
                case EXZUNIONSTORE:
                case EXZDIFFSTORE:
                    if (objects.length >= 4) {
                        int keyCount1 = (int) Utils.bytesToNum(objects[2]);
                        if (keyCount1 > 0) {
                            byte[] convertedKey1 = convert(commandContext, redisCommand, objects[1], convert);
                            objects[1] = convertedKey1;
                            for (int i = 3; i < 3 + keyCount1; i++) {
                                byte[] convertedKey2 = convert(commandContext, redisCommand, objects[i], convert);
                                objects[i] = convertedKey2;
                            }
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
                case ZMPOP:
                case LMPOP:
                case SINTERCARD:
                    if (objects.length >= 3) {
                        int keyCount2 = (int) Utils.bytesToNum(objects[1]);
                        if (keyCount2 > 0) {
                            for (int i = 2; i < 2 + keyCount2; i++) {
                                byte[] convertedKey2 = convert(commandContext, redisCommand, objects[i], convert);
                                objects[i] = convertedKey2;
                            }
                        }
                    }
                    break;
                case BLMPOP:
                case BZMPOP:
                    if (objects.length >= 4) {
                        int keyCount2 = (int) Utils.bytesToNum(objects[2]);
                        if (keyCount2 > 0) {
                            for (int i = 3; i < 3 + keyCount2; i++) {
                                byte[] convertedKey2 = convert(commandContext, redisCommand, objects[i], convert);
                                objects[i] = convertedKey2;
                            }
                        }
                    }
                    break;
                case BITOP:
                    if (objects.length >= 4) {
                        for (int i = 2; i < objects.length; i++) {
                            byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                            objects[i] = convertedKey;
                        }
                    }
                    break;
                case BLPOP:
                case BRPOP:
                case BRPOPLPUSH:
                case BZPOPMAX:
                case BZPOPMIN:
                case EXBZPOPMAX:
                case EXBZPOPMIN:
                    for (int i = 1; i < objects.length - 1; i++) {
                        byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                        objects[i] = convertedKey;
                    }
                    break;
                case XREAD:
                case XREADGROUP:
                    int index = -1;
                    for (int i = 1; i < objects.length; i++) {
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
                    int keyCount3 = last / 2;
                    for (int i = index + 1; i < index + keyCount3 + 1; i++) {
                        byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                        objects[i] = convertedKey;
                    }
                    break;
                case XINFO:
                case XGROUP:
                    if (objects.length >= 3) {
                        byte[] convertedKey = convert(commandContext, redisCommand, objects[2], convert);
                        objects[2] = convertedKey;
                    }
                    break;
                case JSON_MGET:
                    if (objects.length >= 3) {
                        for (int i=1; i<objects.length - 1; i++) {
                            byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                            objects[i] = convertedKey;
                        }
                    }
                    break;
                default:
                    break;
            }
        } else if (redisCommand == RedisCommand.KEYS) {
            if (objects.length >= 2) {
                byte[] convertedKey = convert(commandContext, redisCommand, objects[1], convert);
                objects[1] = convertedKey;
            }
        } else if (redisCommand == RedisCommand.SCAN) {
            int matchIndex = -1;
            for (int i = 2; i < objects.length; i ++) {
                if (i == matchIndex + 1) {
                    byte[] convertedKey = convert(commandContext, redisCommand, objects[i], convert);
                    objects[i] = convertedKey;
                }
                if ("MATCH".equalsIgnoreCase(new String(objects[i], Utils.utf8Charset))) {
                    matchIndex = i;
                }
            }
            if (matchIndex == -1) {
                byte[] convertedKey = convert(commandContext, redisCommand, Utils.stringToBytes("*"), convert);
                byte[][] args = new byte[objects.length + 2][];
                System.arraycopy(objects, 0, args, 0, objects.length);
                args[objects.length] = RedisKeyword.MATCH.getRaw();
                args[objects.length + 1] = convertedKey;
                command.updateObjects(args);
            }
        } else if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE
                || redisCommand == RedisCommand.UNSUBSCRIBE || redisCommand == RedisCommand.PUNSUBSCRIBE
                || redisCommand == RedisCommand.SSUBSCRIBE || redisCommand == RedisCommand.SUNSUBSCRIBE) {
            if (objects.length > 1) {
                for (int i = 1; i < objects.length; i ++) {
                    byte[] convertedChannels = convert(commandContext, redisCommand, objects[i], convert);
                    objects[i] = convertedChannels;
                }
            }
        } else if (redisCommand == RedisCommand.PUBLISH || redisCommand == RedisCommand.SPUBLISH) {
            if (objects.length > 1) {
                byte[] convertedChannels = convert(commandContext, redisCommand, objects[1], convert);
                objects[1] = convertedChannels;
            }
        } else if (redisCommand == RedisCommand.PUBSUB) {
            if (objects.length >= 2) {
                String subCommand = Utils.bytesToString(objects[1]);
                if (subCommand.equalsIgnoreCase("NUMSUB")) {
                    if (objects.length >= 3) {
                        for (int i=2; i<objects.length; i++) {
                            byte[] convertedChannels = convert(commandContext, redisCommand, objects[i], convert);
                            objects[i] = convertedChannels;
                        }
                    }
                } else if (subCommand.equalsIgnoreCase("CHANNELS")) {
                    if (objects.length == 3) {
                        byte[] convertedChannels = convert(commandContext, redisCommand, objects[2], convert);
                        objects[2] = convertedChannels;
                    }
                }
            }
        }
    }

    private void keyConvertReply(RedisCommand redisCommand, CommandContext commandContext, Command command, Reply reply) {
        if (keyConverter == null) return;
        if (redisCommand.getCommandType() == RedisCommand.CommandType.TIME_SERIES) {
            timeSeriesKeyConvertReply(redisCommand, commandContext, command, reply);
        } else if (redisCommand == RedisCommand.RANDOMKEY) {
            reverseConvertKey(redisCommand, commandContext, reply);
        } else if (redisCommand == RedisCommand.KEYS) {
            if (!(reply instanceof MultiBulkReply)) {
                return;
            }
            reverseConvertKeyList(redisCommand, commandContext, (MultiBulkReply) reply);
        } else if (redisCommand == RedisCommand.SCAN) {
            if (!(reply instanceof MultiBulkReply)) {
                return;
            }
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies.length < 2) {
                return;
            }
            if (replies[1] instanceof MultiBulkReply) {
                reverseConvertKeyList(redisCommand, commandContext, (MultiBulkReply) replies[1]);
            }
        } else if (redisCommand == RedisCommand.BLPOP || redisCommand == RedisCommand.BRPOP
                || redisCommand == RedisCommand.BZPOPMAX || redisCommand == RedisCommand.BZPOPMIN
                || redisCommand == RedisCommand.EXBZPOPMAX || redisCommand == RedisCommand.EXBZPOPMIN
                || redisCommand == RedisCommand.ZMPOP || redisCommand == RedisCommand.BZMPOP
                || redisCommand == RedisCommand.LMPOP || redisCommand == RedisCommand.BLMPOP) {
            if (reply instanceof MultiBulkReply) {
                Reply[] replies = ((MultiBulkReply) reply).getReplies();
                if (replies != null && replies.length >= 2) {
                    reverseConvertKey(redisCommand, commandContext, replies[0]);
                }
            }
        } else if (redisCommand == RedisCommand.SUBSCRIBE || redisCommand == RedisCommand.PSUBSCRIBE
                || redisCommand == RedisCommand.UNSUBSCRIBE || redisCommand == RedisCommand.PUNSUBSCRIBE
                || redisCommand == RedisCommand.SSUBSCRIBE || redisCommand == RedisCommand.SUNSUBSCRIBE) {
            PubSubUtils.checkKeyConverter(redisCommand, command.getCommandContext(), keyConverter, reply);
        } else if (redisCommand == RedisCommand.PUBSUB) {
            byte[][] objects = command.getObjects();
            if (objects.length >= 2) {
                String subCommand = Utils.bytesToString(objects[1]);
                if (subCommand.equalsIgnoreCase("NUMSUB")) {
                    if (reply instanceof MultiBulkReply) {
                        Reply[] replies = ((MultiBulkReply) reply).getReplies();
                        if (replies != null) {
                            if (replies.length % 2 == 0) {
                                for (int i=0; i<replies.length; i+=2) {
                                    reverseConvertKey(redisCommand, commandContext, replies[i]);
                                }
                            }
                        }
                    }
                } else if (subCommand.equalsIgnoreCase("CHANNELS")) {
                    if (reply instanceof MultiBulkReply) {
                        Reply[] replies = ((MultiBulkReply) reply).getReplies();
                        if (replies != null) {
                            for (Reply reply1 : replies) {
                                reverseConvertKey(redisCommand, commandContext, reply1);
                            }
                        }
                    }
                }
            }
        }
    }

    private void timeSeriesKeyConvertReply(RedisCommand redisCommand, CommandContext context, Command command, Reply reply) {
        if (!(reply instanceof MultiBulkReply)) {
            return;
        }
        Reply[] replies = ((MultiBulkReply) reply).getReplies();
        if (replies == null) {
            return;
        }
        switch (redisCommand) {
            case TS_QUERYINDEX:
                reverseConvertKeyList(redisCommand, context, (MultiBulkReply) reply);
                break;
            case TS_MRANGE:
            case TS_MREVRANGE:
                byte[][] args = command.getObjects();
                // GROUPBY results are named after label=value, not Redis keys.
                if (args.length >= 4 && "GROUPBY".equalsIgnoreCase(Utils.bytesToString(args[args.length - 4]))
                        && "REDUCE".equalsIgnoreCase(Utils.bytesToString(args[args.length - 2]))) {
                    break;
                }
                reverseTimeSeriesEntryKeys(redisCommand, context, replies);
                break;
            case TS_MGET:
                reverseTimeSeriesEntryKeys(redisCommand, context, replies);
                break;
            case TS_INFO:
                for (int i = 0; i + 1 < replies.length; i += 2) {
                    String name = replies[i].toString();
                    if ("sourceKey".equalsIgnoreCase(name) || "keySelfName".equalsIgnoreCase(name)) {
                        reverseConvertKey(redisCommand, context, replies[i + 1]);
                    } else if ("rules".equalsIgnoreCase(name) && replies[i + 1] instanceof MultiBulkReply) {
                        Reply[] rules = ((MultiBulkReply) replies[i + 1]).getReplies();
                        if (rules != null) {
                            reverseTimeSeriesEntryKeys(redisCommand, context, rules);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private void reverseTimeSeriesEntryKeys(RedisCommand redisCommand, CommandContext context, Reply[] entries) {
        for (Reply entry : entries) {
            if (entry instanceof MultiBulkReply) {
                Reply[] values = ((MultiBulkReply) entry).getReplies();
                if (values != null && values.length > 0) {
                    reverseConvertKey(redisCommand, context, values[0]);
                }
            }
        }
    }

    private void reverseConvertKeyList(RedisCommand redisCommand, CommandContext commandContext, MultiBulkReply multiBulkReply) {
        Reply[] replies = multiBulkReply.getReplies();
        if (replies != null) {
            for (Reply reply : replies) {
                reverseConvertKey(redisCommand, commandContext, reply);
            }
        }
    }

    private void reverseConvertKey(RedisCommand redisCommand, CommandContext commandContext, Reply reply) {
        if (!(reply instanceof BulkReply)) {
            return;
        }
        byte[] raw = ((BulkReply) reply).getRaw();
        if (raw != null) {
            byte[] bytes = keyConverter.reverseConvert(commandContext, redisCommand, raw);
            ((BulkReply) reply).updateRaw(bytes);
        }
    }

    private static int countedArgumentCount(byte[][] objects, int countIndex, int firstIndex, int step) {
        if (countIndex < 0 || objects.length <= countIndex) {
            return 0;
        }
        try {
            long count = Long.parseLong(Utils.bytesToString(objects[countIndex]));
            if (count > 0 && count <= (objects.length - firstIndex) / step) {
                return (int) count;
            }
        } catch (NumberFormatException ignored) {
            // Leave invalid requests to the upstream command's argument validation.
        }
        return 0;
    }

    private void convertCountedKeys(RedisCommand redisCommand, CommandContext commandContext, byte[][] objects,
                                    int countIndex, int firstKey, int step, boolean destination, boolean convert) {
        int count = countedArgumentCount(objects, countIndex, firstKey, step);
        if (count == 0) {
            return;
        }
        if (destination) {
            objects[1] = convert(commandContext, redisCommand, objects[1], convert);
        }
        for (int i = 0; i < count; i++) {
            int index = firstKey + i * step;
            objects[index] = convert(commandContext, redisCommand, objects[index], convert);
        }
    }

    // Stop at the first FIELDS/MEMBERS marker so data bearing the same name
    // is never interpreted as another option.
    private static int argumentSection(byte[][] objects, String marker) {
        return argumentSection(objects, marker, 2);
    }

    private static int argumentSection(byte[][] objects, String marker, int start) {
        for (int i = start; i < objects.length; i++) {
            if (marker.equalsIgnoreCase(Utils.bytesToString(objects[i]))) {
                return i;
            }
        }
        return -1;
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
                        int i = 1;
                        for (Reply reply1 : replies) {
                            if (reply1 instanceof BulkReply) {
                                byte[] raw = ((BulkReply) reply1).getRaw();
                                if (raw != null) {
                                    byte[] bytes = stringConverter.valueReverseConvert(commandContext, objects[i], raw);
                                    ((BulkReply) reply1).updateRaw(bytes);
                                }
                            }
                            i++;
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
            case DELEX:
                if (objects.length == 4 && ("IFEQ".equalsIgnoreCase(Utils.bytesToString(objects[2]))
                        || "IFNE".equalsIgnoreCase(Utils.bytesToString(objects[2])))) {
                    byte[] value = stringConverter.valueConvert(commandContext, objects[1], objects[3]);
                    if (value != null) {
                        objects[3] = value;
                    }
                }
                break;
            case MSETEX:
                int count = countedArgumentCount(objects, 1, 2, 2);
                for (int i = 0; i < count; i++) {
                    int index = 3 + i * 2;
                    byte[] value = stringConverter.valueConvert(commandContext, objects[index - 1], objects[index]);
                    if (value != null) {
                        objects[index] = value;
                    }
                }
                break;
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
                for (int i = 2; i < objects.length; i += 2) {
                    byte[] convertedValue = stringConverter.valueConvert(commandContext, objects[i - 1], objects[i]);
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
            case SADDEX:
            case SEXPIRE:
            case SPEXPIRE:
            case SEXPIREAT:
            case SPEXPIREAT:
            case SPERSIST:
            case STTL:
            case SPTTL:
            case SEXPIRETIME:
            case SPEXPIRETIME:
                int section = argumentSection(objects, "MEMBERS");
                if (section >= 0) {
                    int count = countedArgumentCount(objects, section + 1, section + 2, 1);
                    for (int i = section + 2; i < section + 2 + count; i++) {
                        byte[] value = setConverter.valueConvert(commandContext, objects[1], objects[i]);
                        if (value != null) {
                            objects[i] = value;
                        }
                    }
                }
                break;
            case SADD:
            case SREM:
            case SISMEMBER:
            case SMISMEMBER:
                if (objects.length >= 2) {
                    for (int i = 2; i < objects.length; i++) {
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
                    for (int i = 2; i < objects.length; i++) {
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
            case LMOVEM:
            case BLMOVEM:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies != null) {
                        for (Reply element : replies) {
                            if (element instanceof BulkReply && ((BulkReply) element).getRaw() != null) {
                                byte[] value = listConverter.valueReverseConvert(commandContext, objects[1], ((BulkReply) element).getRaw());
                                ((BulkReply) element).updateRaw(value);
                            }
                        }
                    }
                }
                break;
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
                break;
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
            case HEXPIRE:
            case HPEXPIRE:
            case HEXPIREAT:
            case HPEXPIREAT:
            case HPERSIST:
            case HTTL:
            case HPTTL:
            case HEXPIRETIME:
            case HPEXPIRETIME:
            case HGETDEL:
            case HGETEX:
            case HSETEX:
                int section = argumentSection(objects, "FIELDS");
                if (section >= 0) {
                    int step = redisCommand == RedisCommand.HSETEX ? 2 : 1;
                    int count = countedArgumentCount(objects, section + 1, section + 2, step);
                    for (int i = 0; i < count; i++) {
                        int index = section + 2 + i * step;
                        objects[index] = hashConverter.fieldConvert(commandContext, objects[1], objects[index]);
                        if (step == 2) {
                            objects[index + 1] = hashConverter.valueConvert(commandContext, objects[1], objects[index + 1]);
                        }
                    }
                }
                break;
            case HDEL:
            case HEXISTS:
            case HINCRBY:
            case HINCRBYFLOAT:
            case HGET:
            case HMGET:
                if (objects.length >= 3) {
                    for (int i = 2; i < objects.length; i++) {
                        byte[] bytes = hashConverter.fieldConvert(commandContext, objects[1], objects[i]);
                        objects[i] = bytes;
                    }
                }
                break;
            case HSET:
            case HSETNX:
            case HMSET:
                if (objects.length >= 4 && objects.length % 2 == 0) {
                    for (int i = 2; i < objects.length; i += 2) {
                        byte[] field = objects[i];
                        byte[] value = objects[i + 1];
                        byte[] bytes1 = hashConverter.fieldConvert(commandContext, objects[1], field);
                        byte[] bytes2 = hashConverter.valueConvert(commandContext, objects[1], value);
                        objects[i] = bytes1;
                        objects[i + 1] = bytes2;
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
            case HGETDEL:
            case HGETEX:
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
        for (int i = 0; i < replies.length; i += 2) {
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
        if (reply.getReplies() == null) {
            return;
        }
        for (Reply reply1 : reply.getReplies()) {
            if (reply1 instanceof BulkReply) {
                byte[] raw = ((BulkReply) reply1).getRaw();
                if (raw == null) {
                    continue;
                }
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
            case ZMOVEMEMBER:
                int section = argumentSection(objects, "MEMBERS", 3);
                if (section >= 0) {
                    int count = countedArgumentCount(objects, section + 1, section + 2, 1);
                    for (int i = section + 2; i < section + 2 + count; i++) {
                        objects[i] = zSetConverter.valueConvert(commandContext, objects[1], objects[i]);
                    }
                }
                break;
            case ZADD:
                if (objects.length == 4) {
                    byte[] bytes = zSetConverter.valueConvert(commandContext, objects[1], objects[3]);
                    objects[3] = bytes;
                }
                if (objects.length >= 4) {
                    for (int i = 2; i < objects.length - 1; i++) {
                        byte[] object = objects[i];
                        String s = Utils.bytesToString(object);
                        if (s.equalsIgnoreCase("NX") || s.equalsIgnoreCase("XX")
                                || s.equalsIgnoreCase("GT") || s.equalsIgnoreCase("LT")
                                || s.equalsIgnoreCase("CH") || s.equalsIgnoreCase("INCR")) {
                            continue;
                        }
                        byte[] member = objects[i + 1];
                        byte[] bytes = zSetConverter.valueConvert(commandContext, objects[1], member);
                        objects[i + 1] = bytes;
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
                    for (int i = 2; i < objects.length; i++) {
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
            case ZMOVE:
            case BZMOVE:
            case ZMOVEMEMBER:
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies = ((MultiBulkReply) reply).getReplies();
                    if (replies != null) {
                        for (Reply entry : replies) {
                            if (entry instanceof MultiBulkReply) {
                                Reply[] pair = ((MultiBulkReply) entry).getReplies();
                                if (pair != null && pair.length == 2 && pair[0] instanceof BulkReply) {
                                    BulkReply member = (BulkReply) pair[0];
                                    if (member.getRaw() != null) {
                                        byte[] value = zSetConverter.valueReverseConvert(commandContext, objects[1], member.getRaw());
                                        member.updateRaw(value);
                                    }
                                }
                            }
                        }
                    }
                }
                break;
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
                        for (int i = 0; i < replies.length; i += 2) {
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
            for (int i = 0; i < replies.length; i += 2) {
                Reply member = replies[i];
                byte[] bytes = zSetConverter.valueReverseConvert(commandContext, key, ((BulkReply) member).getRaw());
                ((BulkReply) member).updateRaw(bytes);
            }
        }
    }

    private boolean withScores(byte[][] objects) {
        for (int i = 2; i < objects.length; i++) {
            String s = Utils.bytesToString(objects[i]);
            if (s.equalsIgnoreCase(RedisKeyword.WITHSCORES.name())) {
                return true;
            }
        }
        return false;
    }


}
