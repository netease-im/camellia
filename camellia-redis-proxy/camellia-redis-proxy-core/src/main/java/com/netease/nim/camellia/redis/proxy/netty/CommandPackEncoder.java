package com.netease.nim.camellia.redis.proxy.netty;

import com.netease.nim.camellia.redis.proxy.command.Command;
import com.netease.nim.camellia.redis.proxy.upstream.client.RedisClient;
import com.netease.nim.camellia.redis.proxy.reply.ErrorReply;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import com.netease.nim.camellia.redis.proxy.util.CommandsEncodeUtil;
import com.netease.nim.camellia.redis.proxy.util.ErrorLogCollector;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 *
 * Created by caojiajun on 2020/8/19
 */
public class CommandPackEncoder extends MessageToMessageEncoder<CommandPack> {

    private static final Logger logger = LoggerFactory.getLogger(CommandPackEncoder.class);
    private final Queue<CompletableFuture<Reply>> queue;
    private final RedisClient redisClient;

    public CommandPackEncoder(RedisClient redisClient, Queue<CompletableFuture<Reply>> queue) {
        super();
        this.redisClient = redisClient;
        this.queue = queue;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommandPack msg, List<Object> out) {
        try {
            List<Command> commands = msg.getCommands();
            long startTime = msg.getStartTime();
            for (CompletableFuture<Reply> future : msg.getCompletableFutureList()) {
                if (!redisClient.isValid()) {
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    continue;
                }
                boolean offer;
                if (startTime > 0) {
                    offer = queue.offer(new CompletableFutureWithTime<>(future, redisClient.getAddr(), startTime));
                } else {
                    offer = queue.offer(future);
                }
                if (!offer) {
                    String log = redisClient.getClientName() + ", queue full, will stop";
                    ErrorLogCollector.collect(CommandPackEncoder.class, log);
                    future.complete(ErrorReply.NOT_AVAILABLE);
                    redisClient.stop();
                }
            }
            if (!redisClient.isValid()) {
                return;
            }
            if (commands.isEmpty()) return;
            ByteBufAllocator allocator = ctx.channel().alloc();
            ByteBuf buf = CommandsEncodeUtil.encode(allocator, commands);
            if (logger.isDebugEnabled()) {
                List<String> commandNames = new ArrayList<>();
                for (Command command : commands) {
                    commandNames.add(command.getName());
                }
                logger.debug("send commands to {}, commands = {}", redisClient.getClientName(), commandNames);
            }
            out.add(buf);
        } catch (Exception e) {
            logger.error("{} error", redisClient.getClientName(), e);
        }
    }
}
