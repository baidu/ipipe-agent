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

package com.baidu.agile.agent.wrapper;

import com.baidu.agile.agent.execute.ShellProcessBuilder;
import com.baidu.agile.agent.os.PosixAPI;
import com.baidu.agile.server.job.bean.wrapper.GitSshWraper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShellGitSshWrapperExecutor extends ShellWrapperExecutor<GitSshWraper> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellGitSshWrapperExecutor.class);

    private static Map<String, List<String>> filesMap = new HashMap<String, List<String>>();

    @Override
    public void before(ShellProcessBuilder builder, GitSshWraper wrapper) {
        if (null == builder || null == wrapper) {
            return;
        }
        String uuid = builder.getUuid();
        List<String> files = filesMap.get(uuid);
        if (CollectionUtils.isEmpty(files)) {
            files = new ArrayList<String>();
            filesMap.put(uuid, files);
        }
        String filePrefix = UUID.randomUUID().toString();
        String sshKeyFile = createTmpFile(
                filePrefix + ".key",
                wrapper.getPrivateKey(),
                0400);
        if (StringUtils.isEmpty(sshKeyFile)) {
            return;
        }
        files.add(sshKeyFile);
        String sshFileContent = new StringBuilder("#!/bin/sh\n")
                .append("ssh -i \"" + sshKeyFile + "\" -o StrictHostKeyChecking=no \"$@\"")
                .toString();
        String sshFile = createTmpFile(
                filePrefix + ".ssh.sh",
                sshFileContent,
                0500);
        if (StringUtils.isEmpty(sshKeyFile)) {
            return;
        }
        files.add(sshFile);
        builder.getEnvs().put("GIT_SSH", sshFile);
    }

    @Override
    public void after(ShellProcessBuilder builder, GitSshWraper wrapper) {
        if (null == builder || null == wrapper) {
            return;
        }
        String uuid = builder.getUuid();
        List<String> files = filesMap.remove(uuid);
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        for (String file : files) {
            try {
                FileUtils.forceDelete(new File(file));
            } catch (FileNotFoundException fe) {
                // do nothing
            } catch (Exception e) {
                LOGGER.error("exception delete file:{} for job: {}", file, uuid, e);
            }
        }
    }

    private static String createTmpFile(String fileName, String content, int mode) {
        String file = System.getProperty("java.io.tmpdir") + File.separator + fileName;
        try {
            FileUtils.write(new File(file), content, Charset.defaultCharset());
            PosixAPI.jnr().chmod(file, mode);
            return file;
        } catch (Exception e) {
            LOGGER.error("create file:{} exception!", file, e);
        }
        return null;
    }
}
