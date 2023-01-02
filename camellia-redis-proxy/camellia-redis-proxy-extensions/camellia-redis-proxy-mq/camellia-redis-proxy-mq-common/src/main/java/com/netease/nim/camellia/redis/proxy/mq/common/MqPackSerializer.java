package com.netease.nim.camellia.redis.proxy.mq.common;


import com.netease.nim.camellia.redis.proxy.command.Command;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;

public class MqPackSerializer {

    /**
     * 序列化
     * @param mqPack mqPack
     * @return 序列化结果
     * @throws IOException 异常
     */
    public static byte[] serialize(MqPack mqPack) throws IOException {
        try (MessageBufferPacker packer = MessagePack.newDefaultBufferPacker()) {
            Command command = mqPack.getCommand();
            byte[][] objects = command.getObjects();

            packer.packArrayHeader(objects.length);
            for (byte[] object : objects) {
                packer.packBinaryHeader(object.length);
                packer.writePayload(object);
            }
            Long bid = mqPack.getBid();
            String bgroup = mqPack.getBgroup();
            String ext = mqPack.getExt();
            if (bid == null) {
                packer.packBoolean(false);
            } else {
                packer.packBoolean(true);
                packer.packLong(bid);
            }
            if (bgroup == null) {
                packer.packBoolean(false);
            } else {
                packer.packBoolean(true);
                packer.packString(bgroup);
            }
            if (ext == null) {
                packer.packBoolean(false);
            } else {
                packer.packBoolean(true);
                packer.packString(ext);
            }
            return packer.toByteArray();
        }
    }

    /**
     * 反序列化
     * @param data 数据
     * @return MqPack
     * @throws IOException 异常
     */
    public static MqPack deserialize(byte[] data) throws IOException {
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data)) {
            MqPack mqPack = new MqPack();
            int size = unpacker.unpackArrayHeader();
            byte[][] command = new byte[size][0];
            for (int i = 0; i < size; i++) {
                int len = unpacker.unpackBinaryHeader();
                byte[] bytes = new byte[len];
                unpacker.readPayload(bytes);
                command[i] = bytes;
            }
            mqPack.setCommand(new Command(command));
            boolean hasBid = unpacker.unpackBoolean();
            if (hasBid) {
                long bid = unpacker.unpackLong();
                mqPack.setBid(bid);
            }
            boolean hasBgroup = unpacker.unpackBoolean();
            if (hasBgroup) {
                String brgoup = unpacker.unpackString();
                mqPack.setBgroup(brgoup);
            }
            boolean hasExt = unpacker.unpackBoolean();
            if (hasExt) {
                String ext = unpacker.unpackString();
                mqPack.setExt(ext);
            }
            return mqPack;
        }
    }
}
