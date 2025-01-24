package com.netease.nim.camellia.redis.proxy.upstream.local.storage.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.Key;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress.CompressType;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress.CompressUtils;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.compress.ICompressor;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants;
import com.netease.nim.camellia.redis.proxy.upstream.local.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.netease.nim.camellia.redis.proxy.upstream.local.storage.constants.LocalStorageConstants.*;

/**
 * Created by caojiajun on 2025/1/3
 */
public class KeyCodec {

    /**
     * 解码多个bucket
     * @param data data
     * @return 解码结果
     */
    public static Map<Key, KeyInfo> decodeBuckets(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int bucketSize = data.length / LocalStorageConstants._4k;
        Map<Key, KeyInfo> result = new HashMap<>();
        for (int i=0; i<bucketSize; i++) {
            byte[] bytes = new byte[LocalStorageConstants._4k];
            buffer.get(bytes);
            Map<Key, KeyInfo> map = KeyCodec.decodeBucket(bytes);
            result.putAll(map);
        }
        return result;
    }

    /**
     * 解码bucket
     * @param bytes 固定为4k输入
     * @return 解码结果
     */
    public static Map<Key, KeyInfo> decodeBucket(byte[] bytes) {
        int crc1 = BytesUtils.toInt(bytes, 0);//0,1,2,3
        int crc2 = RedisClusterCRC16Utils.getCRC16(bytes, 9, bytes.length);
        if (crc1 != crc2) {
            return new HashMap<>();
        }
        int decompressLen = BytesUtils.toShort(bytes, 4);//4,5
        if (decompressLen == 0) {
            return new HashMap<>();
        }
        int compressLen = BytesUtils.toShort(bytes, 6);//6,7
        byte compressType = bytes[8];//8
        ICompressor compressor = CompressUtils.get(CompressType.getByValue(compressType));
        byte[] decompressData = compressor.decompress(bytes, 9, compressLen, decompressLen);
        if (decompressData.length == 0) {
            return new HashMap<>();
        }
        Unpack unpack = new Unpack(decompressData);
        int size = unpack.popVarUint();
        Map<Key, KeyInfo> map = new HashMap<>();
        for (int i=0; i<size; i++) {
            KeyInfo key = new KeyInfo();
            unpack.popMarshallable(key);
            map.put(new Key(key.getKey()), key);
        }
        return map;
    }

    /**
     * 编码bucket，可能会压缩
     * 如果编码后超过了4k，则返回null，上层自行拆分；如果不足4k，则补0
     * @param keys keys
     * @return 编码结果，固定为4k
     */
    public static byte[] encodeBucket(Map<Key, KeyInfo> keys) {
        Pack pack = new Pack();
        pack.putVarUint(keys.size());
        for (Map.Entry<Key, KeyInfo> entry : keys.entrySet()) {
            pack.putMarshallable(entry.getValue());
        }
        pack.getBuffer().capacity(pack.getBuffer().readableBytes());
        byte[] array = pack.getBuffer().array();
        short decompressLen = (short) array.length;
        CompressType compressType;
        if (decompressLen + 9 > _4k) {//4k装不下，才压缩
            compressType = CompressType.zstd;
        } else {
            compressType = CompressType.none;
        }
        byte[] compressed;
        short compressLen;
        if (compressType == CompressType.none) {
            compressed = array;
        } else {
            ICompressor compressor = CompressUtils.get(compressType);
            compressed = compressor.compress(array, 0, array.length);
            if (compressed.length >= array.length) {
                compressed = array;
                compressType = CompressType.none;
            }
        }
        if (compressed.length > Short.MAX_VALUE) {
            return null;
        }
        compressLen = (short) compressed.length;
        if (compressed.length + 9 > _4k) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(_4k);
        buffer.putInt(0);//0,1,2,3
        buffer.putShort(decompressLen);//4,5
        buffer.putShort(compressLen);//6,7
        buffer.put(compressType.getType());//8
        buffer.put(compressed);
        byte[] bytes = buffer.array();
        int crc = RedisClusterCRC16Utils.getCRC16(bytes, 9, bytes.length);
        buffer.putInt(0, crc);
        return buffer.array();
    }
}
