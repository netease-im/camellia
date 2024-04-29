package com.netease.nim.camellia.redis.proxy.kv.samples;

import org.tikv.common.TiConfiguration;
import org.tikv.common.TiSession;
import org.tikv.kvproto.Kvrpcpb;
import org.tikv.raw.RawKVClient;
import org.tikv.shade.com.google.protobuf.ByteString;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Created by caojiajun on 2024/4/29
 */
public class TestTikv {

    public static void main(String[] args) throws InterruptedException {
        String string = "127.0.0.1:2379";
        TiConfiguration conf = TiConfiguration.createRawDefault(string);
        conf.setWarmUpEnable(false);
        TiSession session = TiSession.create(conf);
        RawKVClient rawClient = session.createRawClient();

////        rawClient.put(ByteString.copyFrom("k1".getBytes(StandardCharsets.UTF_8)), ByteString.copyFrom("v1".getBytes(StandardCharsets.UTF_8)));
////        rawClient.put(ByteString.copyFrom("k2".getBytes(StandardCharsets.UTF_8)), ByteString.copyFrom("v2".getBytes(StandardCharsets.UTF_8)));
////        rawClient.put(ByteString.copyFrom("k3".getBytes(StandardCharsets.UTF_8)), ByteString.copyFrom("v3".getBytes(StandardCharsets.UTF_8)));
////        rawClient.put(ByteString.copyFrom("k4".getBytes(StandardCharsets.UTF_8)), ByteString.copyFrom("v4".getBytes(StandardCharsets.UTF_8)));
////        rawClient.put(ByteString.copyFrom("k5".getBytes(StandardCharsets.UTF_8)), ByteString.copyFrom("v5".getBytes(StandardCharsets.UTF_8)));
//
//        rawClient.put(ByteString.copyFromUtf8("k1"), ByteString.copyFromUtf8("v11"), 10);
//        rawClient.put(ByteString.copyFromUtf8("k1"), ByteString.copyFromUtf8("v1"));
//
//
//        while (true) {
//            Optional<ByteString> bytes = rawClient.get(ByteString.copyFromUtf8("k1"));
//            System.out.println(bytes.isPresent());
//            if (bytes.isPresent()) {
//                System.out.println(bytes.get());
//            }
//            Thread.sleep(1000);
//        }


//        Optional<ByteString> bytes = rawClient.get(ByteString.copyFrom("k1".getBytes(StandardCharsets.UTF_8)));
//        System.out.println(bytes.get());
//
//        Iterator<Kvrpcpb.KvPair> iterator = rawClient.scan0(ByteString.copyFromUtf8("k1"), ByteString.copyFromUtf8("k5"), 100);
//        while (iterator.hasNext()) {
//            Kvrpcpb.KvPair next = iterator.next();
//            System.out.println(next.getKey() + "=" + next.getValue());
//        }
//
//        List<Kvrpcpb.KvPair> kvPairs = rawClient.scanPrefix(ByteString.copyFromUtf8("k"));
//        for (Kvrpcpb.KvPair kvPair : kvPairs) {
//            System.out.println(kvPair.getKey() + "=" + kvPair.getValue());
//        }
//
//        System.out.println("===");
//
//        List<Kvrpcpb.KvPair> scan = rawClient.scan(ByteString.copyFromUtf8("k5"), ByteString.copyFromUtf8("k1"), 100);
//        for (Kvrpcpb.KvPair kvPair : scan) {
//            System.out.println(kvPair.getKey() + "=" + kvPair.getValue());
//        }

//        List<Kvrpcpb.KvPair> scan1 = rawClient.scan(ByteString.copyFromUtf8("k5"), ByteString.copyFromUtf8("k1"), 100);
//        while (iterator2.hasNext()) {
//            Kvrpcpb.KvPair next = iterator2.next();
//            System.out.println(next.getKey() + "=" + next.getValue());
//        }
    }
}
