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

import com.baidu.agile.agent.log.IJobAsyncLogger;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class FilesUploader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesUploader.class);

    private List<String> files;

    private String fromDir;

    private String toDir;

    private FileUploader fileUploader;

    private IJobAsyncLogger jobAsyncLogger;

    public FilesUploader(IJobAsyncLogger jobAsyncLogger) {
        this.jobAsyncLogger = jobAsyncLogger;
    }

    public FilesUploader files(List<String> files) {
        this.files = files;
        return this;
    }

    public FilesUploader fromDir(String fromDir) {
        this.fromDir = fromDir;
        return this;
    }

    public FilesUploader toDir(String toDir) {
        this.toDir = toDir;
        return this;
    }

    public FilesUploader fileUploader(FileUploader fileUploader) {
        this.fileUploader = fileUploader;
        return this;
    }

    public void upload() {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        int idx = 0;
        jobAsyncLogger.log("INFO", "Start uploading files, total:" + files.size() + ", please wait ...");
        for (String file : files) {
            jobAsyncLogger.log("INFO", "Uploading file (" + ++idx + "/" + files.size() + ") : " + file + "...");
            try {
                if (new File(file).isAbsolute()) {
                    fileUploader.upload(new File(file),
                            StringUtils.trimToEmpty(this.toDir) + "/" + file.replace(File.separator, "/"));
                } else {
                    fileUploader.upload(new File(fromDir + File.separator + file),
                            StringUtils.trimToEmpty(this.toDir) + "/" + file.replace(File.separator, "/"));
                }
            } catch (Exception e) {
                LOGGER.error("exception when upload file:" + file, e);
            }

        }
    }
}
