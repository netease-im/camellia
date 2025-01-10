package com.netease.nim.camellia.redis.proxy.upstream.local.storage.file;


import com.netease.nim.camellia.redis.proxy.upstream.local.storage.value.block.BlockType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by caojiajun on 2025/1/10
 */
public class FileNames {

    private static final Logger logger = LoggerFactory.getLogger(FileNames.class);

    public static String createKeyManifestFileIfNotExists(String dir) throws IOException {
        String fileName = keyManifestFile(dir);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create key manifest file, file = {}, result = {}", fileName, result);
        }
        return fileName;
    }

    public static String keyManifestFile(String dir) {
        return dir + "/" + "key.manifest";
    }

    public static String createKeyFile(String dir, long fileId) throws IOException {
        String fileName = keyFile(dir, fileId);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create key file, file = {}, result = {}", file, result);
        }
        return fileName;
    }

    public static String keyFile(String dir, long fileId) {
        return dir + "/" + fileId + ".key";
    }

    public static String createStringDataFileIfNotExists(String dir, BlockType blockType, long fileId) throws IOException {
        String fileName = stringBlockFile(dir, blockType, fileId);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create string block file, file = {}, result = {}", file, result);
        }
        return fileName;
    }

    public static String stringBlockFile(String dir, BlockType blockType, long fileId) {
        return dir + "/" + fileId + "_" + blockType.getType() + ".data";
    }

    public static String createStringSlotFileIfNotExists(String dir, BlockType blockType, long fileId) throws IOException {
        String fileName = stringSlotFile(dir, blockType, fileId);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create string slot file, file = {}, result = {}", file, result);
        }
        return fileName;
    }

    public static String stringSlotFile(String dir, BlockType blockType, long fileId) {
        return dir + "/" + fileId + "_" + blockType.getType() + ".slot";
    }

    public static String createStringIndexFileIfNotExists(String dir, BlockType blockType, long fileId) throws IOException {
        String fileName = stringIndexFile(dir, blockType, fileId);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create string index file, file = {}, result = {}", file, result);
        }
        return fileName;
    }

    public static String stringIndexFile(String dir, BlockType blockType, long fileId) {
        return dir + "/" + fileId + "_" + blockType.getType() + ".index";
    }
}
