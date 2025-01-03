package com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.codec;

import com.netease.nim.camellia.codec.Pack;
import com.netease.nim.camellia.codec.Unpack;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress.CompressType;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress.CompressUtils;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.compress.ICompressor;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.constants.EmbeddedStorageConstants;
import com.netease.nim.camellia.redis.proxy.upstream.embedded.storage.key.KeyInfo;
import com.netease.nim.camellia.redis.proxy.upstream.kv.utils.BytesUtils;
import com.netease.nim.camellia.redis.proxy.util.RedisClusterCRC16Utils;
import com.netease.nim.camellia.tools.utils.BytesKey;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by caojiajun on 2025/1/3
 */
public class KeyCodec {

    /**
     * 解码一整个slot
     * @param all data
     * @return 解码结果
     */
    public static Map<BytesKey, KeyInfo> decodeSlot(byte[] all) {
        ByteBuffer buffer = ByteBuffer.wrap(all);
        int bucketSize = all.length / EmbeddedStorageConstants._4k;
        Map<BytesKey, KeyInfo> result = new HashMap<>();
        for (int i=0; i<bucketSize; i++) {
            byte[] bytes = new byte[EmbeddedStorageConstants._4k];
            buffer.get(bytes);
            Map<BytesKey, KeyInfo> map = KeyCodec.decodeBucket(bytes);
            result.putAll(map);
        }
        return result;
    }

    /**
     * 解码bucket
     * @param bytes 固定为4k输入
     * @return 解码结果
     */
    public static Map<BytesKey, KeyInfo> decodeBucket(byte[] bytes) {
        int crc1 = BytesUtils.toInt(bytes, 0);//0,1,2,3
        int crc2 = RedisClusterCRC16Utils.getCRC16(bytes, 9, bytes.length);
        if (crc1 != crc2) {
            return new HashMap<>();
        }
        int decompressLen = BytesUtils.toShort(bytes, 4);//4,5
        int compressLen = BytesUtils.toShort(bytes, 6);//6,7
        byte compressType = bytes[8];//8
        ICompressor compressor = CompressUtils.get(CompressType.getByValue(compressType));
        byte[] decompressData = compressor.decompress(bytes, 9, compressLen, decompressLen);
        Unpack unpack = new Unpack(decompressData);
        int size = unpack.popVarUint();
        Map<BytesKey, KeyInfo> map = new HashMap<>();
        for (int i=0; i<size; i++) {
            KeyInfo key = new KeyInfo();
            unpack.popMarshallable(key);
            map.put(new BytesKey(key.getKey()), key);
        }
        return map;
    }

    /**
     * 编码bucket，可能会压缩
     * 如果编码后超过了4k，则返回null，上层自行拆分；如果不足4k，则补0
     * @param keys keys
     * @return 编码结果，固定为4k
     */
    public static byte[] encodeBucket(Map<BytesKey, KeyInfo> keys) {
        Pack pack = new Pack();
        pack.putVarUint(keys.size());
        for (Map.Entry<BytesKey, KeyInfo> entry : keys.entrySet()) {
            pack.putMarshallable(entry.getValue());
        }
        pack.getBuffer().capacity(pack.getBuffer().readableBytes());
        byte[] array = pack.getBuffer().array();
        short decompressLen = (short) array.length;
        CompressType compressType = CompressType.zstd;
        ICompressor compressor = CompressUtils.get(compressType);
        byte[] compressed = compressor.compress(array, 0, array.length);
        if (compressed.length > array.length) {
            compressType = CompressType.none;
            compressed = array;
        }
        if (compressed.length + 9 > EmbeddedStorageConstants._4k) {
            return null;
        }
        short compressLen = (short) compressed.length;

        ByteBuffer buffer = ByteBuffer.allocate(EmbeddedStorageConstants._4k);
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
