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

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.log.IJobAsyncLogger;
import com.baidu.agile.server.job.bean.artifact.Artifact;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class JobArtifact {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobArtifact.class);

    private String uuid;

    private String ws;

    private Artifact artifact;

    private String toDir;

    private FileUploader fileUploader;

    private IJobAsyncLogger jobAsyncLogger;

    public JobArtifact(String uuid) {
        this(uuid, Context.ws(uuid));
    }

    public JobArtifact(String uuid, String ws) {
        this.uuid = uuid;
        this.ws = StringUtils.defaultIfBlank(ws, Context.ws(uuid));
    }

    public JobArtifact(String uuid, String ws, IJobAsyncLogger jobAsyncLogger) {
        this.uuid = uuid;
        this.ws = StringUtils.defaultIfBlank(ws, Context.ws(uuid));
        this.jobAsyncLogger = jobAsyncLogger;
    }

    public JobArtifact artifact(Artifact artifact) {
        this.artifact = artifact;
        return this;
    }

    public JobArtifact toDir(String toDir) {
        this.toDir = toDir;
        return this;
    }

    public JobArtifact fileUploader(FileUploader fileUploader) {
        this.fileUploader = fileUploader;
        return this;
    }

    public int upload() {
        FileScanner scanner;
        String fromDir = this.ws;
        switch (artifact.getPatternType()) {
            case DIR:
                if (new File(artifact.getFilePattern()).isAbsolute()) {
                    fromDir = artifact.getFilePattern();
                } else {
                    fromDir = fromDir + File.separator + artifact.getFilePattern();
                }
                // 当路径所指的目标是单文件时，直接读取单上传文件。
                if (new File(fromDir).exists() && new File(fromDir).isFile()) {
                    String fileName = FilenameUtils.getName(FilenameUtils.normalizeNoEndSeparator(fromDir));
                    String toFile = uuid + "/" + toDir + "/" + fileName;
                    jobAsyncLogger.log("INFO", "single file uploading...");
                    fileUploader.upload(new File(fromDir), toFile);
                    return 1;
                }
                scanner = new DirFileScanner(artifact.getFilePattern());
                break;
            case ANT:
                scanner = new AntFileScanner(artifact.getFilePattern());
                break;
            default:
                throw new AgentException("Not supported file pattern type:" + artifact.getPatternType());
        }
        List<String> files = scanner.scan(ws);
        if (CollectionUtils.isEmpty(files)) {
            return 0;
        }
        new FilesUploader(jobAsyncLogger)
                .fromDir(fromDir)
                .files(files)
                .toDir(uuid + "/" + toDir)
                .fileUploader(fileUploader).upload();
        return files.size();
    }


}
