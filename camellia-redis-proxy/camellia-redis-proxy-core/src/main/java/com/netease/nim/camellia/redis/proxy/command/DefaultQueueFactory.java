package com.netease.nim.camellia.redis.proxy.command;

import com.netease.nim.camellia.redis.proxy.conf.ProxyDynamicConf;
import com.netease.nim.camellia.redis.proxy.reply.Reply;
import org.jctools.queues.MpmcArrayQueue;
import org.jctools.queues.SpscArrayQueue;
import org.jctools.queues.SpscLinkedQueue;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.jctools.queues.atomic.SpscLinkedAtomicQueue;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by caojiajun on 2023/4/6
 */
public class DefaultQueueFactory implements QueueFactory {

    /**
     * multi-producer, multi-consumer
     * @return Queue
     */
    @Override
    public Queue<CommandTask> generateCommandTaskQueue() {
        String queue = ProxyDynamicConf.getString("command.task.queue.type", CommandTaskQueueType.LinkedBlockingQueue.name());
        CommandTaskQueueType type = CommandTaskQueueType.getByName(queue);
        int capacity = ProxyDynamicConf.getInt("command.task.queue.capacity", 1024*32);
        if (type == CommandTaskQueueType.ConcurrentLinkedQueue) {
            return new ConcurrentLinkedQueue<>();
        } else if (type == CommandTaskQueueType.LinkedBlockingQueue) {
            return new LinkedBlockingQueue<>(capacity);
        } else if (type == CommandTaskQueueType.ArrayBlockingQueue) {
            return new ArrayBlockingQueue<>(capacity);
        } else if (type == CommandTaskQueueType.MpmcArrayQueue) {
            return new MpmcArrayQueue<>(capacity);
        } else if (type == CommandTaskQueueType.MpmcAtomicArrayQueue) {
            return new MpmcAtomicArrayQueue<>(capacity);
        } else {
            return new LinkedBlockingQueue<>(capacity);
        }
    }

    /**
     * single-producer, single-consumer
     * @return Queue
     */
    @Override
    public Queue<CompletableFuture<Reply>> generateCommandReplyQueue() {
        String queue = ProxyDynamicConf.getString("command.reply.queue.type", CommandReplyQueueType.LinkedBlockingQueue.name());
        int capacity = ProxyDynamicConf.getInt("command.reply.queue.capacity", 1024*32);
        CommandReplyQueueType type = CommandReplyQueueType.getByName(queue);
        if (type == CommandReplyQueueType.SpscLinkedQueue) {
            return new SpscLinkedQueue<>();
        } else if (type == CommandReplyQueueType.LinkedBlockingQueue) {
            return new LinkedBlockingQueue<>(capacity);
        } else if (type == CommandReplyQueueType.ArrayBlockingQueue) {
            return new ArrayBlockingQueue<>(capacity);
        } else if (type == CommandReplyQueueType.ConcurrentLinkedQueue) {
            return new ConcurrentLinkedQueue<>();
        } else if (type == CommandReplyQueueType.SpscArrayQueue) {
            return new SpscArrayQueue<>(capacity);
        } else if (type == CommandReplyQueueType.SpscAtomicArrayQueue) {
            return new SpscAtomicArrayQueue<>(capacity);
        } else if (type == CommandReplyQueueType.ArrayDeque) {
            int initCapacity = ProxyDynamicConf.getInt("command.reply.queue.init.capacity", 1024);
            return new ArrayDeque<>(initCapacity);
        } else if (type == CommandReplyQueueType.LinkedList) {
            return new LinkedList<>();
        } else if (type == CommandReplyQueueType.SpscLinkedAtomicQueue) {
            return new SpscLinkedAtomicQueue<>();
        } else {
            return new LinkedBlockingQueue<>(capacity);
        }
    }

    private enum CommandTaskQueueType {
        LinkedBlockingQueue,
        ArrayBlockingQueue,
        ConcurrentLinkedQueue,
        MpmcArrayQueue,
        MpmcAtomicArrayQueue,
        ;
        public static CommandTaskQueueType getByName(String name) {
            for (CommandTaskQueueType type : CommandTaskQueueType.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    private enum CommandReplyQueueType {
        LinkedBlockingQueue,
        ArrayBlockingQueue,
        ConcurrentLinkedQueue,
        SpscLinkedQueue,
        SpscLinkedAtomicQueue,
        SpscArrayQueue,
        SpscAtomicArrayQueue,
        ArrayDeque,
        LinkedList,
        ;
        public static CommandReplyQueueType getByName(String name) {
            for (CommandReplyQueueType type : CommandReplyQueueType.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
