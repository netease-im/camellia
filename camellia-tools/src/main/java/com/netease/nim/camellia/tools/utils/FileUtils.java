package com.netease.nim.camellia.tools.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * Created by caojiajun on 2023/8/22
 */
public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static class FileInfo {
        private final String fileContent;//文件内容
        private final String filePath;//文件路径

        public FileInfo(String filePath, String fileContent) {
            this.filePath = filePath;
            this.fileContent = fileContent;
        }

        public String getFileContent() {
            return fileContent;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    public static FileInfo readDynamic(String fileNameOrFilePath) {
        //当作fileName处理
        String filePath = getClasspathFilePath(fileNameOrFilePath);
        if (filePath != null) {
            String fileContent = readFileContentByFilePath(filePath);
            if (fileContent != null) {
                return new FileInfo(filePath, fileContent);
            }
        } else {
            //当作filePath处理
            String fileContent = readFileContentByFilePath(fileNameOrFilePath);
            if (fileContent != null) {
                return new FileInfo(fileNameOrFilePath, fileContent);
            }
        }
        //当作jar包内部的fileName处理，此时需要用stream去读
        String fileContent = readFileContentByFileNameInStream(fileNameOrFilePath);
        if (fileContent != null) {
            return new FileInfo(null, fileContent);
        }
        return null;
    }

    public static FileInfo readByFileName(String fileName) {
        //当作classpath下的fileName处理
        String filePath = getClasspathFilePath(fileName);
        if (filePath != null) {
            String fileContent = readFileContentByFilePath(filePath);
            if (fileContent != null) {
                return new FileInfo(filePath, fileContent);
            }
        }
        //当作jar包内部的fileName处理，此时需要用stream去读
        String fileContent = readFileContentByFileNameInStream(fileName);
        if (fileContent != null) {
            return new FileInfo(null, fileContent);
        }
        return null;
    }

    public static FileInfo readByFilePath(String filePath) {
        String fileContent = readFileContentByFilePath(filePath);
        if (fileContent != null) {
            return new FileInfo(filePath, fileContent);
        }
        return null;
    }

    public static String getClasspathFilePath(String fileName) {
        try {
            URL resource = FileUtils.class.getClassLoader().getResource(fileName);
            if (resource == null) {
                return null;
            }
            return Paths.get(resource.toURI()).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean write(String filePath, String content) {
        try (FileWriter fileWriter = new FileWriter(filePath, false)) {
            fileWriter.write(content);
            return true;
        } catch (Exception e) {
            logger.error("write error, filePath = {}", filePath, e);
            return false;
        }
    }

    private static String readFileContentByFileNameInStream(String fileName) {
        byte[] buffer;
        InputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {
            fis = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
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

    private static String readFileContentByFilePath(String filePath) {
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
