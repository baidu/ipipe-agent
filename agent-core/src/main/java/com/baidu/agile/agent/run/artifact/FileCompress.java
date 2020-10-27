/**
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.agile.agent.run.artifact;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileCompress {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCompress.class);

    private static final int BUFFER = 1024;

    private static final String BASE_DIR = "";

    private static final String SUFFIX_FORMAT = ".%s";

    /**
     * 压缩
     *
     * @param srcFilePath  原文件路径
     * @param destFilePath 目标文件路径
     * @param suffix       需要压缩的文件后缀，一个筛选的操作
     * @throws Exception
     */
    public static void compress(String srcFilePath, String destFilePath, String suffix) throws Exception {

        File srcFile = new File(srcFilePath);
        File zipFile = new File(destFilePath);

        FileOutputStream outputStream = null;
        ZipArchiveOutputStream zipOut = null;

        try {
            outputStream = new FileOutputStream(zipFile);
            zipOut = new ZipArchiveOutputStream(outputStream);
            archive(srcFile, zipOut, BASE_DIR, suffix);
        } catch (IOException e) {
            LOGGER.error("File compress exception!", e);
        } finally {
            if (zipOut != null) {
                zipOut.flush();
                zipOut.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    /**
     * 递归归档，目录和文件采用两种处理方式
     *
     * @param srcFile
     * @param zipOut
     * @param basePath 归档包内相对路径
     * @param suffix   需要压缩的文件后缀
     */
    private static void archive(File srcFile, ZipArchiveOutputStream zipOut, String basePath, String suffix) {
        try {
            if (srcFile.isDirectory()) {
                archiveDir(srcFile, zipOut, basePath, suffix);
            } else {
                archiveFile(srcFile, zipOut, basePath, suffix);
            }
        } catch (Exception e) {
            LOGGER.error("Archive file is error! Exception:{}.", e);
        }
    }

    /**
     * 处理目录
     *
     * @param fileDir
     * @param zipOut
     * @throws Exception
     */
    private static void archiveDir(File fileDir, ZipArchiveOutputStream zipOut, String basePath, String suffix)
            throws Exception {

        File[] files = fileDir.listFiles();
        if (files.length < 1) {
            ZipArchiveEntry entry = new ZipArchiveEntry(basePath + fileDir.getName() + File.separator);
            zipOut.putArchiveEntry(entry);
            zipOut.closeArchiveEntry();
        }
        for (File file : files) {
            // 递归归档
            archive(file, zipOut, basePath + fileDir.getName() + File.separator, suffix);
        }
    }

    /**
     * 处理文件
     *
     * @param file
     * @param zipOut
     * @throws Exception
     */
    private static void archiveFile(File file, ZipArchiveOutputStream zipOut, String basePath, String suffix)
            throws Exception {

        suffix = StringUtils.isBlank(suffix) ? StringUtils.EMPTY : String.format(SUFFIX_FORMAT, suffix);
        if (!file.getName().endsWith(suffix)) {
            return;
        }

        ZipArchiveEntry entry = new ZipArchiveEntry(basePath + file.getName());
        entry.setSize(file.length());
        zipOut.putArchiveEntry(entry);

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fileInputStream);
            int count;
            byte[] data = new byte[BUFFER];
            while ((count = bis.read(data, 0, BUFFER)) != -1) {
                zipOut.write(data, 0, count);
            }
            bis.close();
            zipOut.closeArchiveEntry();

        } catch (Exception e) {
            LOGGER.error("Archive file is exception:", e);

        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }
}
