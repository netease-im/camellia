package com.netease.nim.camellia.redis.samples;

/**
 * Created by caojiajun on 2023/1/13
 */
public class MiscTest {

    public static void main(String[] args) throws InterruptedException {
        pubsub();
        blocking();
        transaction();
    }

    private static void blocking() {
        TestBlockingCommand.testProduce();
        TestBlockingCommand.testConsume();
    }

    private static void transaction() {
        TestTransaction.testTransaction(10);
    }

    private static void pubsub() {
        new Thread(() -> {
            TestPubSubLettuce.pub();
            while (true) {
                TestPubSubLettuce.sub();
                sleep(1000);
            }
        }).start();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
