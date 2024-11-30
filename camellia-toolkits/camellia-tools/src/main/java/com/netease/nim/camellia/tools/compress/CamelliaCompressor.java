package com.netease.nim.camellia.tools.compress;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 一个解压缩的工具类，会通过magic、长度校验等方法来判断是否压缩过，从而可以对解压缩前后的数据进行兼容性处理（压缩过的则会解压，没压缩过的则直接返回）
 * 数据结构：tag（1字节，用于判断解压缩类型）+ 魔数（自定义，n个字节） + 压缩后长度（4字节） + 解压后长度（4字节） + 压缩后内容（m个字节）
 * Created by caojiajun on 2021/8/13
 */
public class CamelliaCompressor {

    private static final Logger logger = LoggerFactory.getLogger(CamelliaCompressor.class);

    private static final String DEFAULT_MAGIC = "camellia~c";
    private static final int DEFAULT_THRESHOLD = 1024;

    private static final byte NO_COMPRESSED = (byte) 0;
    private static final byte LZ4_COMPRESSED = (byte) 1;//可以用于扩展解压缩的类型

    private static final LZ4Compressor compressor;
    private static final LZ4FastDecompressor decompressor;
    static {
        LZ4Factory factory = LZ4Factory.fastestInstance();
        compressor = factory.fastCompressor();
        decompressor = factory.fastDecompressor();
    }

    private final byte[] magicBytes;
    private final int headerLen;
    private final int threshold;//超过多少字节才压缩，阈值是可以动态变更的，不会影响解压正确性

    public static final CamelliaCompressor DEFAULT = new CamelliaCompressor();

    public CamelliaCompressor() {
        this(DEFAULT_MAGIC, DEFAULT_THRESHOLD);
    }

    public CamelliaCompressor(int threshold) {
        this(DEFAULT_MAGIC, threshold);
    }

    public CamelliaCompressor(String magic, int threshold) {
        this.magicBytes = magic.getBytes(StandardCharsets.UTF_8);
        this.headerLen = 1 + magicBytes.length + 4 + 4;
        this.threshold = threshold;
    }

    /**
     * 压缩，会判断是否需要压缩（是否超过压缩阈值，压缩后是否变小）
     * @param originalData 原始数据
     * @return 压缩后数据
     */
    public byte[] compress(byte[] originalData) {
        try {
            if (originalData == null) return null;
            if (originalData.length <= threshold) {
                return originalData;
            }
            int maxLength = compressor.maxCompressedLength(originalData.length);
            byte[] compressed = new byte[maxLength];
            int compressedLength = compressor.compress(originalData, 0, originalData.length, compressed, 0, maxLength);
            if (headerLen + compressedLength >= originalData.length) {
                return originalData;
            }
            byte[] compressedData = new byte[headerLen + compressedLength];
            System.arraycopy(compressed, 0, compressedData, headerLen, compressedLength);
            ByteBuffer buffer = ByteBuffer.wrap(compressedData);
            buffer.put(LZ4_COMPRESSED);
            buffer.put(magicBytes);
            buffer.putInt(compressedData.length);
            buffer.putInt(originalData.length);
            return compressedData;
        } catch (Exception e) {
            logger.error("compress error", e);
            throw new CamelliaCompressException(e);
        }
    }

    /**
     * 解压，会判断是否压缩过
     * @param compressedData 压缩后数据
     * @return 原始数据
     */
    public byte[] decompress(byte[] compressedData) {
        try {
            if (compressedData == null) return null;
            if (compressedData.length <= headerLen) {
                return compressedData;
            }
            ByteBuffer buffer = ByteBuffer.wrap(compressedData);
            byte tag = buffer.get();
            if (tag == NO_COMPRESSED) {
                return compressedData;
            }
            byte[] magicTest = new byte[magicBytes.length];
            buffer.get(magicTest);
            if (!Arrays.equals(magicTest, magicBytes)) {
                return compressedData;
            }
            int len = buffer.getInt();
            if (len != compressedData.length) {
                return compressedData;
            }
            int originalLen = buffer.getInt();
            byte[] originalData = new byte[originalLen];
            decompressor.decompress(compressedData, headerLen, originalData, 0, originalLen);
            return originalData;
        } catch (Exception e) {
            logger.error("decompress error", e);
            throw new CamelliaCompressException(e);
        }
    }
}
