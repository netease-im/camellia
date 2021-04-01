package com.netease.nim.camellia.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 *
 * Created by caojiajun on 2020/4/15.
 */
public class FileUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static String readFileByName(String fileName) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
        if (resource == null) {
            return readFileByNameInStream(fileName);
        }
        String filePath = resource.getPath();
        String content = readFileByPath(filePath);
        if (content == null) {
            return readFileByNameInStream(fileName);
        } else {
            return content;
        }
    }

    public static String readFileByNameInStream(String file) {
        byte[] buffer;
        InputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            if (fis == null) return null;
            bos = new ByteArrayOutputStream(1024);
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return new String(buffer, StandardCharsets.UTF_8);
    }

    public static String readFileByPath(String filePath) {
        byte[] buffer;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            File file = new File(filePath);
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream(1024);
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return new String(buffer, StandardCharsets.UTF_8);
    }
}
