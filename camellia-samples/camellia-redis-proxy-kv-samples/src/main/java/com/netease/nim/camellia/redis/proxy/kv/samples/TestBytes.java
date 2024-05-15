package com.netease.nim.camellia.redis.proxy.kv.samples;

import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.Utils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by caojiajun on 2024/5/15
 */
public class TestBytes {

    public static void main(String[] args) throws InterruptedException {
        int j = 127;
        while (true) {
            byte[] bytes = Utils.stringToBytes(UUID.randomUUID().toString().replace("-", ""));
            for (int i=0; i<bytes.length; i++) {
                bytes[i] += (byte) ThreadLocalRandom.current().nextInt(127);
            }
            System.out.println("j=" + j);
            bytes[bytes.length - 1] = (byte) j;
            j++;

            byte[] bytes1 = BytesUtils.nextBytes(bytes);
            int compare1 = BytesUtils.compare(bytes, bytes1);
            if (compare1 != -1) {
                System.out.println("ERROR");
                System.exit(-1);
            }
            byte[] bytes2 = BytesUtils.lastBytes(bytes);
            int compare2 = BytesUtils.compare(bytes, bytes2);
            if (compare2 != 1) {
                System.out.println("ERROR");
                System.exit(-1);
            }
            System.out.println("success");
            Thread.sleep(100);
        }
    }
}
