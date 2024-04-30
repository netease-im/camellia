package com.netease.nim.camellia.redis.proxy.util;

import com.netease.nim.camellia.redis.proxy.auth.IdentityInfo;
import com.netease.nim.camellia.redis.proxy.conf.CamelliaServerProperties;
import com.netease.nim.camellia.redis.proxy.netty.ChannelType;
import com.netease.nim.camellia.redis.proxy.reply.*;
import com.netease.nim.camellia.redis.proxy.upstream.connection.RedisConnectionAddr;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringSocketChannel;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by caojiajun on 2019/11/5.
 */
public class Utils {

    public static final String syntaxError = "syntax error";

    public static final byte[] EMPTY_ARRAY = new byte[0];

    public static final byte[] NEG_ONE = convert(-1, false);
    public static final byte[] NEG_ONE_WITH_CRLF = convert(-1, true);
    public static final char CR = '\r';
    public static final char LF = '\n';
    public static final char ZERO = '0';
    public static Charset utf8Charset = StandardCharsets.UTF_8;

    private static final int NUM_MAP_LENGTH = 256;
    private static final byte[][] numMap = new byte[NUM_MAP_LENGTH][];
    private static final byte[][] numMapWithCRLF = new byte[NUM_MAP_LENGTH][];

    static {
        for (int i = 0; i < NUM_MAP_LENGTH; i++) {
            numMapWithCRLF[i] = convert(i, true);
            numMap[i] = convert(i, false);
        }
    }

    public static byte[] numToBytes(long value, boolean withCRLF) {
        if (value >= 0 && value < NUM_MAP_LENGTH) {
            int index = (int) value;
            return withCRLF ? numMapWithCRLF[index] : numMap[index];
        } else if (value == -1) {
            return withCRLF ? NEG_ONE_WITH_CRLF : NEG_ONE;
        }
        return convert(value, withCRLF);
    }

    private static byte[] convert(long value, boolean withCRLF) {
        boolean negative = value < 0;
        // Checked javadoc: If the argument is equal to 10^n for integer n, then the result is n.
        // Also, if negative, leave another slot for the sign.
        long abs = Math.abs(value);
        int index = (value == 0 ? 0 : (int) Math.log10(abs)) + (negative ? 2 : 1);
        // Append the CRLF if necessary
        byte[] bytes = new byte[withCRLF ? index + 2 : index];
        if (withCRLF) {
            bytes[index] = CR;
            bytes[index + 1] = LF;
        }
        // Put the sign in the slot we saved
        if (negative) bytes[0] = '-';
        long next = abs;
        while ((next /= 10) > 0) {
            bytes[--index] = (byte) ('0' + (abs % 10));
            abs = next;
        }
        bytes[--index] = (byte) ('0' + abs);
        return bytes;
    }

    public static boolean checkStringIgnoreCase(byte[] bytes, String string) {
        if (bytes == null) {
            return string == null;
        }
        return new String(bytes, utf8Charset).equalsIgnoreCase(string);
    }

    public static double bytesToDouble(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException(syntaxError);
        }
        try {
            return Double.parseDouble(new String(bytes, utf8Charset));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(syntaxError);
        }
    }

    public static byte[] doubleToBytes(Double d) {
        if (d == null) return null;
        return String.valueOf(d).getBytes(utf8Charset);
    }

    public static String bytesToString(byte[] bytes) {
        if (bytes == null) return null;
        return new String(bytes, utf8Charset);
    }

    public static byte[] stringToBytes(String s) {
        if (s == null) return null;
        return s.getBytes(utf8Charset);
    }

    public static IllegalArgumentException illegalArgumentException() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 4) {
            String methodName = stackTrace[3].getMethodName();
            return new IllegalArgumentException("wrong number of arguments for '" + methodName + "' command");
        }
        return new IllegalArgumentException("wrong number of arguments");
    }

    public static long bytesToNum(byte[] bytes) {
        int length = bytes.length;
        if (length == 0) {
            throw new IllegalArgumentException("value is not an integer or out of range");
        }
        int position = 0;
        int sign;
        int read = bytes[position++];
        if (read == '-') {
            read = bytes[position++];
            sign = -1;
        } else {
            sign = 1;
        }
        long number = 0;
        do {
            int value = read - '0';
            if (value >= 0 && value < 10) {
                number *= 10;
                number += value;
            } else {
                throw new IllegalArgumentException("value is not an integer or out of range");
            }
            if (position == length) {
                return number * sign;
            }
            read = bytes[position++];
        } while (true);
    }

    public static Reply mergeMultiIntegerReply(List<Reply> replies) {
        if (replies == null || replies.isEmpty()) {
            return MultiBulkReply.EMPTY;
        } else {
            int size = 0;
            Map<Integer, AtomicLong> map = new HashMap<>();
            for (Reply reply : replies) {
                if (reply instanceof MultiBulkReply) {
                    Reply[] replies1 = ((MultiBulkReply) reply).getReplies();
                    size = Math.max(replies1.length, size);
                    for (int i = 0; i < replies1.length; i++) {
                        Reply reply1 = replies1[i];
                        if (reply1 instanceof IntegerReply) {
                            Long integer = ((IntegerReply) reply1).getInteger();
                            AtomicLong atomicLong = map.get(i);
                            if (atomicLong == null) {
                                atomicLong = new AtomicLong(0);
                                map.put(i, atomicLong);
                            }
                            atomicLong.addAndGet(integer);
                        } else {
                            return checkErrorReply(reply);
                        }
                    }
                } else {
                    return checkErrorReply(reply);
                }
            }
            Reply[] replies1 = new Reply[size];
            for (int i = 0; i < size; i++) {
                AtomicLong atomicLong = map.get(i);
                replies1[i] = new IntegerReply(atomicLong == null ? 0 : atomicLong.get());
            }
            return new MultiBulkReply(replies1);
        }
    }

    public static Reply mergeIntegerReply(List<Reply> replies) {
        if (replies == null || replies.isEmpty()) {
            return new IntegerReply(0L);
        } else {
            long ret = 0;
            for (Reply reply : replies) {
                if (reply instanceof IntegerReply) {
                    ret += ((IntegerReply) reply).getInteger();
                } else {
                    return checkErrorReply(reply);
                }
            }
            return new IntegerReply(ret);
        }
    }

    public static Reply mergeStatusReply(List<Reply> replies) {
        if (replies == null || replies.isEmpty()) {
            return StatusReply.OK;
        }
        for (Reply reply : replies) {
            if (reply instanceof StatusReply) {
                String status = ((StatusReply) reply).getStatus();
                if (!status.equalsIgnoreCase(StatusReply.OK.getStatus())) {
                    return reply;
                }
            } else {
                return checkErrorReply(reply);
            }
        }
        return StatusReply.OK;
    }

    private static Reply checkErrorReply(Reply reply) {
        if (reply instanceof ErrorReply) {
            return reply;
        } else {
            return ErrorReply.NOT_AVAILABLE;
        }
    }

    public static boolean hasChange(String md5, String newMd5) {
        return md5 == null || (newMd5 != null && !md5.equals(newMd5));
    }

    /**
     * 缓存key拼接，代表不同租户
     *
     * @param bid    bid
     * @param bgroup bgroup
     * @return bid + "|" + bgroup
     */
    public static String getCacheKey(Long bid, String bgroup) {
        return bid + "|" + bgroup;
    }

    /**
     * Get namespace by splicing bid and bgroup.
     * <p> eg. bid + "|" + bgroup
     *
     * @param bid    bid
     * @param bgroup bgroup
     * @return namespace string
     */
    public static String getNamespaceOrSetDefault(Long bid, String bgroup) {
        if (bid == null || bgroup == null) {
            return "default|default";
        } else {
            return bid + "|" + bgroup;
        }
    }

    /**
     * Get namespace by splicing bid and bgroup.
     * <p> eg. bid + "|" + bgroup
     *
     * @param identityInfo identityInfo
     * @return namespace string
     */
    public static String getNamespaceOrSetDefault(IdentityInfo identityInfo) {
        Long bid = identityInfo.getBid();
        String bgroup = identityInfo.getBgroup();
        if (bid == null || bgroup == null) {
            return "default|default";
        } else {
            return bid + "|" + bgroup;
        }
    }

    /**
     * if ping fail, return null
     * @param reply reply
     * @return resp string
     */
    public static String checkPingReply(Reply reply) {
        if (reply == null) {
            return null;
        }
        if (reply instanceof StatusReply) {
            String resp = ((StatusReply) reply).getStatus();
            if (resp != null && resp.equalsIgnoreCase(StatusReply.PONG.getStatus())) {
                return resp;
            }
        }
        if (reply instanceof MultiBulkReply) {
            Reply[] replies = ((MultiBulkReply) reply).getReplies();
            if (replies.length > 0) {
                Reply reply1 = replies[0];
                if (reply1 instanceof BulkReply) {
                    String resp = Utils.bytesToString(((BulkReply) reply1).getRaw());
                    if (resp != null && resp.equalsIgnoreCase(StatusReply.PONG.getStatus())) {
                        return resp;
                    }
                }
            }
        }
        return null;
    }

    public static boolean idleCloseHandlerEnable(CamelliaServerProperties serverProperties) {
        return serverProperties.getReaderIdleTimeSeconds() >= 0 && serverProperties.getWriterIdleTimeSeconds() >= 0
                && serverProperties.getAllIdleTimeSeconds() >= 0;
    }

    public static ChannelType channelType(RedisConnectionAddr addr) {
        if (addr.getHost() != null && addr.getPort() > 0) {
            return ChannelType.tcp;
        } else if (addr.getUdsPath() != null) {
            return ChannelType.uds;
        } else {
            return ChannelType.unknown;
        }
    }

    public static Class<? extends Channel> channelClass(ChannelType channelType, EventLoop eventLoop) {
        EventLoopGroup parent = eventLoop.parent();
        if (channelType == ChannelType.tcp) {
            if (parent instanceof EpollEventLoopGroup) {
                return EpollSocketChannel.class;
            } else if (parent instanceof KQueueEventLoopGroup) {
                return KQueueSocketChannel.class;
            } else if (parent instanceof IOUringEventLoopGroup) {
                return IOUringSocketChannel.class;
            } else if (parent instanceof NioEventLoopGroup) {
                return NioSocketChannel.class;
            }
            return NioSocketChannel.class;
        } else if (channelType == ChannelType.uds) {
            if (parent instanceof EpollEventLoopGroup) {
                return EpollDomainSocketChannel.class;
            } else if (parent instanceof KQueueEventLoopGroup) {
                return KQueueDomainSocketChannel.class;
            }
            return null;
        } else {
            throw new IllegalArgumentException("unknown channelType");
        }
    }

    public static String className(Object obj, boolean simpleClassName) {
        if (obj == null) {
            return "";
        }
        Class<?> clazz = obj.getClass();
        if (simpleClassName) {
            return clazz.getSimpleName();
        } else {
            return clazz.getName();
        }
    }

    public static int count(boolean[] booleans) {
        int count = 0;
        for (boolean b : booleans) {
            if (b) {
                count ++;
            }
        }
        return count;
    }
}
