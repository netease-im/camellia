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

    public static String walFileDictionary(String dir) {
        return dir;
    }

    public static String dataFileDictionary(String dir) {
        return dir;
    }

    public static void createWalFileIfNotExists(String dir, long fileId) throws IOException {
        String fileName = walFile(dir, fileId);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create wal file, file = {}, result = {}", fileName, result);
        }
    }

    public static String walFile(String dir, long fileId) {
        return walFileDictionary(dir) + "/" + fileId + ".wal";
    }

    public static void createWalManifestFileIfNotExists(String dir) throws IOException {
        String fileName = walManifestFile(dir);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create wal manifest file, file = {}, result = {}", fileName, result);
        }
    }

    public static String walManifestFile(String dir) {
        return walFileDictionary(dir) + "/wal.manifest";
    }

    public static void createKeyManifestFileIfNotExists(String dir) throws IOException {
        String fileName = keyManifestFile(dir);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create key manifest file, file = {}, result = {}", fileName, result);
        }
    }

    public static String keyManifestFile(String dir) {
        return dataFileDictionary(dir) + "/key.manifest";
    }

    public static void createKeyFile(String dir, long fileId) throws IOException {
        String fileName = keyFile(dir, fileId);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create key file, file = {}, result = {}", file, result);
        }
    }

    public static String keyFile(String dir, long fileId) {
        return dataFileDictionary(dir) + "/" + fileId + ".key";
    }

    public static void createStringDataFileIfNotExists(String dir, BlockType blockType, long fileId) throws IOException {
        String fileName = stringBlockFile(dir, blockType, fileId);
        File file = new File(fileName);
        if (!file.exists()) {
            boolean result = file.createNewFile();
            logger.info("create string data file, file = {}, result = {}", file, result);
        }
    }

    public static String stringBlockFile(String dir, BlockType blockType, long fileId) {
        return dataFileDictionary(dir) + "/" + fileId + "_" + blockType.getType() + ".data";
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
        return dataFileDictionary(dir) + "/" + fileId + "_" + blockType.getType() + ".slot";
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
        return dataFileDictionary(dir) + "/" + fileId + "_" + blockType.getType() + ".index";
    }
}
