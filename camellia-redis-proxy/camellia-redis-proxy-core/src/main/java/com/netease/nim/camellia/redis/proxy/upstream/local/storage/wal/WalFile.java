package com.netease.nim.camellia.redis.proxy.upstream.local.storage.wal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Created by caojiajun on 2025/1/8
 */
public record WalFile(long fileId) {

    private static final Logger logger = LoggerFactory.getLogger(WalFile.class);

    public static WalFile parse(File file) {
        try {
            String name = file.getName();
            if (!name.endsWith(".wal")) {
                return null;
            }
            long fileId = Long.parseLong(name.split("\\.")[0]);
            return new WalFile(fileId);
        } catch (Exception e) {
            logger.error("parse wal file name error, file = {}", file.getName());
            return null;
        }
    }
}
