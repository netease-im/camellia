package com.netease.nim.camellia.redis.proxy.plugin.bigkey;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.enums.RedisCommand;
import com.netease.nim.camellia.redis.proxy.enums.RedisKeyword;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.util.ExecutorUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;

/**
 *
 * Created by caojiajun on 2020/11/10
 */
public class BigKeyHunter {

    private final String CALLBACK_NAME;

    private final BigKeyMonitorCallback bigKeyMonitorCallback;

    public BigKeyHunter(BigKeyMonitorCallback bigKeyMonitorCallback) {
        this.bigKeyMonitorCallback = bigKeyMonitorCallback;
        this.CALLBACK_NAME = bigKeyMonitorCallback.getClass().getName();
    }

    private boolean isEnable(Long bid, String bgroup) {
        return ProxyDynamicConf.getBoolean("big.key.monitor.enable", bid, bgroup, true);
    }

    /**
     * 校验请求
     */
    public void checkRequest(Command command) {
        if (command == null) return;
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return;
        if (redisCommand.getType() != RedisCommand.Type.WRITE) return;
        RedisCommand.CommandType commandType = redisCommand.getCommandType();
        if (commandType == null) return;
        byte[][] objects = command.getObjects();
        Long bid = command.getCommandContext().getBid();
        String bgroup = command.getCommandContext().getBgroup();
        boolean enable = isEnable(bid, bgroup);
        if (!enable) return;
        switch (commandType) {
            case STRING:
                switch (redisCommand) {
                    case SET:
                    case SETNX:
                    case GETSET:
                        if (objects.length >= 3) {
                            byte[] value = objects[2];
                            int threshold = stringSizeThreshold(bid, bgroup);
                            if (value != null && value.length > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], value.length, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackRequest(command, objects[1], value.length, threshold));
                            }
                        }
                        break;
                    case SETEX:
                    case PSETEX:
                        if (objects.length >= 4) {
                            byte[] value = objects[3];
                            int threshold = stringSizeThreshold(bid, bgroup);
                            if (value != null && value.length > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], value.length, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackRequest(command, objects[1], value.length, threshold));
                            }
                        }
                        break;
                    case MSET:
                    case MSETNX:
                        for (int i=2; i<objects.length; i+=2) {
                            //mset k1 v1 k2 v2
                            byte[] value = objects[i];
                            int threshold = stringSizeThreshold(bid, bgroup);
                            if (value != null && value.length > threshold) {
                                int index = i - 1;
                                BigKeyMonitor.bigKey(command, objects[index], value.length, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackRequest(command, objects[index], value.length, threshold));
                            }
                        }
                        break;
                }
                break;
            case HASH:
                switch (redisCommand) {
                    case HSET:
                    case HSETNX:
                    case HMSET:
                        int size = ((objects.length - 2) / 2);
                        int threshold = hashSizeThreshold(bid, bgroup);
                        if (size > threshold) {
                            BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                            ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackRequest(command, objects[1], size, threshold));
                        }
                        break;
                }
                break;
            case ZSET:
                //as zadd command support [NX|XX] [GT|LT] [CH] [INCR], so big key monitor maybe not very exact
                if (redisCommand == RedisCommand.ZADD) {
                    int size = ((objects.length - 2) / 2);
                    int threshold = zsetSizeThreshold(bid, bgroup);
                    if (size > threshold) {
                        BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                        ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackRequest(command, objects[1], size, threshold));
                    }
                }
                break;
            case LIST:
                switch (redisCommand) {
                    case LPUSH:
                    case LPUSHX:
                    case RPUSH:
                    case RPUSHX:
                        int size = objects.length - 2;
                        int threshold = listSizeThreshold(bid, bgroup);
                        if (size > threshold) {
                            BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                            ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackRequest(command, objects[1], size, threshold));
                        }
                        break;
                }
                break;
            case SET:
                if (redisCommand == RedisCommand.SADD) {
                    int size = objects.length - 2;
                    int threshold = setSizeThreshold(bid, bgroup);
                    if (size > threshold) {
                        BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                        ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackRequest(command, objects[1], size, threshold));
                    }
                }
                break;
        }
    }

    /**
     * 校验响应
     */
    public void checkReply(Command command, Reply reply) {
        if (command == null) return;
        RedisCommand redisCommand = command.getRedisCommand();
        if (redisCommand == null) return;
        if (redisCommand.getType() != RedisCommand.Type.READ
                && redisCommand != RedisCommand.GETSET && redisCommand != RedisCommand.GETEX && redisCommand != RedisCommand.GETDEL) return;
        RedisCommand.CommandType commandType = redisCommand.getCommandType();
        if (commandType == null) return;
        if (reply == null) return;
        if (reply instanceof ErrorReply) return;
        byte[][] objects = command.getObjects();
        Long bid = command.getCommandContext().getBid();
        String bgroup = command.getCommandContext().getBgroup();
        boolean enable = isEnable(bid, bgroup);
        if (!enable) return;
        switch (commandType) {
            case STRING:
                switch (redisCommand) {
                    case GETEX:
                    case GETDEL:
                    case GETSET:
                    case GET:
                        if (reply instanceof BulkReply) {
                            byte[] raw = ((BulkReply) reply).getRaw();
                            int threshold = stringSizeThreshold(bid, bgroup);
                            if (raw != null && raw.length > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], raw.length, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], raw.length, threshold));
                            }
                        }
                        break;
                    case MGET:
                        if (reply instanceof MultiBulkReply) {
                            Reply[] replies = ((MultiBulkReply) reply).getReplies();
                            if (replies != null) {
                                int i=1;
                                int threshold = stringSizeThreshold(bid, bgroup);
                                for (Reply reply1 : replies) {
                                    if (reply1 instanceof BulkReply) {
                                        byte[] raw = ((BulkReply) reply1).getRaw();
                                        if (raw != null && raw.length > threshold) {
                                            int index = i;
                                            BigKeyMonitor.bigKey(command, objects[index], raw.length, threshold);
                                            ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[index], raw.length, threshold));
                                        }
                                    }
                                    i ++;
                                }
                            }
                        }
                        break;
                    case STRLEN:
                        if (reply instanceof IntegerReply) {
                            Long integer = ((IntegerReply) reply).getInteger();
                            int threshold = stringSizeThreshold(bid, bgroup);
                            if (integer != null && integer > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], integer, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], integer, threshold));
                            }
                        }
                        break;
                }
                break;
            case HASH:
                switch (redisCommand) {
                    case HLEN:
                        if (reply instanceof IntegerReply) {
                            Long integer = ((IntegerReply) reply).getInteger();
                            int threshold = hashSizeThreshold(bid, bgroup);
                            if (integer != null && integer > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], integer, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], integer, threshold));
                            }
                        }
                        break;
                    case HKEYS:
                    case HVALS:
                        if (reply instanceof MultiBulkReply) {
                            Reply[] replies = ((MultiBulkReply) reply).getReplies();
                            if (replies != null) {
                                int size = replies.length;
                                int threshold = hashSizeThreshold(bid, bgroup);
                                if (size > threshold) {
                                    BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                                    ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], size, threshold));
                                }
                            }
                        }
                        break;
                    case HGETALL:
                        if (reply instanceof MultiBulkReply) {
                            Reply[] replies = ((MultiBulkReply) reply).getReplies();
                            if (replies != null) {
                                int size = replies.length / 2;
                                int threshold = hashSizeThreshold(bid, bgroup);
                                if (size > threshold) {
                                    BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                                    ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], size, threshold));
                                }
                            }
                        }
                        break;
                }
                break;
            case ZSET:
                switch (redisCommand) {
                    case ZCARD:
                    case ZCOUNT:
                    case ZLEXCOUNT:
                        if (reply instanceof IntegerReply) {
                            Long integer = ((IntegerReply) reply).getInteger();
                            int threshold = zsetSizeThreshold(bid, bgroup);
                            if (integer != null && integer > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], integer, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], integer, threshold));
                            }
                        }
                        break;
                    case ZRANGEBYLEX:
                    case ZREVRANGEBYLEX:
                        if (reply instanceof MultiBulkReply) {
                            Reply[] replies = ((MultiBulkReply) reply).getReplies();
                            if (replies != null) {
                                int size = replies.length;
                                int threshold = zsetSizeThreshold(bid, bgroup);
                                if (size > threshold) {
                                    BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                                    ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], size, threshold));
                                }
                            }
                        }
                        break;
                    case ZRANGE:
                    case ZREVRANGE:
                    case ZRANGEBYSCORE:
                    case ZREVRANGEBYSCORE:
                        if (reply instanceof MultiBulkReply) {
                            Reply[] replies = ((MultiBulkReply) reply).getReplies();
                            if (replies != null) {
                                boolean withScores = false;
                                for (int i=4; i<objects.length; i++) {
                                    withScores = Utils.bytesToString(objects[i]).equalsIgnoreCase(RedisKeyword.WITHSCORES.name());
                                    if (withScores) break;
                                }
                                int size = withScores ? replies.length / 2 : replies.length;
                                int threshold = zsetSizeThreshold(bid, bgroup);
                                if (size > threshold) {
                                    BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                                    ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], size, threshold));
                                }
                            }
                        }
                        break;
                }
                break;
            case LIST:
                switch (redisCommand) {
                    case LLEN:
                        if (reply instanceof IntegerReply) {
                            Long integer = ((IntegerReply) reply).getInteger();
                            int threshold = listSizeThreshold(bid, bgroup);
                            if (integer != null && integer > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], integer, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], integer, threshold));
                            }
                        }
                        break;
                    case LRANGE:
                        if (reply instanceof MultiBulkReply) {
                            Reply[] replies = ((MultiBulkReply) reply).getReplies();
                            if (replies != null) {
                                int size = replies.length;
                                int threshold = listSizeThreshold(bid, bgroup);
                                if (size > threshold) {
                                    BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                                    ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], size, threshold));
                                }
                            }
                        }
                        break;
                }
                break;
            case SET:
                switch (redisCommand) {
                    case SCARD:
                        if (reply instanceof IntegerReply) {
                            Long integer = ((IntegerReply) reply).getInteger();
                            int threshold = setSizeThreshold(bid, bgroup);
                            if (integer != null && integer > threshold) {
                                BigKeyMonitor.bigKey(command, objects[1], integer, threshold);
                                ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], integer, threshold));
                            }
                        }
                        break;
                    case SMEMBERS:
                        if (reply instanceof MultiBulkReply) {
                            Reply[] replies = ((MultiBulkReply) reply).getReplies();
                            if (replies != null) {
                                int size = replies.length;
                                int threshold = setSizeThreshold(bid, bgroup);
                                if (size > threshold) {
                                    BigKeyMonitor.bigKey(command, objects[1], size, threshold);
                                    ExecutorUtils.submitCallbackTask(CALLBACK_NAME, () -> bigKeyMonitorCallback.callbackReply(command, reply, objects[1], size, threshold));
                                }
                            }
                        }
                        break;
                }
                break;
        }
    }

    private int stringSizeThreshold(Long bid, String bgroup) {
        return ProxyDynamicConf.getInt("big.key.monitor.string.threshold", bid, bgroup, 2*1024*1024);
    }

    private int hashSizeThreshold(Long bid, String bgroup) {
        return ProxyDynamicConf.getInt("big.key.monitor.hash.threshold", bid, bgroup, 5000);
    }

    private int setSizeThreshold(Long bid, String bgroup) {
        return ProxyDynamicConf.getInt("big.key.monitor.set.threshold", bid, bgroup, 5000);
    }

    private int zsetSizeThreshold(Long bid, String bgroup) {
        return ProxyDynamicConf.getInt("big.key.monitor.zset.threshold", bid, bgroup, 5000);
    }

    private int listSizeThreshold(Long bid, String bgroup) {
        return ProxyDynamicConf.getInt("big.key.monitor.list.threshold", bid, bgroup, 5000);
    }
}
