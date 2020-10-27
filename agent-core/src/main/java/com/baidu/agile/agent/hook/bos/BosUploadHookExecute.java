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

package com.baidu.agile.agent.hook.bos;

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.hook.HookExecute;
import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.server.job.bean.hook.ShellHook;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.bos.BosUploadHook;
import com.baidu.agile.server.job.bean.hook.bos.BosUploadHookResponse;
import com.baidubce.auth.DefaultBceSessionCredentials;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class BosUploadHookExecute implements HookExecute {

    private static final Logger LOGGER = LoggerFactory.getLogger(BosUploadHookExecute.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 反序列化时忽未知字段
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public ShellHookResponse triggerHook(ShellHook shellHook, String uuid, String ws, IJobLogger jobLogger) {
        if (null == shellHook) {
            return null;
        }

        BosUploadHookResponse hookResponse = new BosUploadHookResponse();
        hookResponse.setHookId(shellHook.getHookId());

        BosUploadHook hook = (BosUploadHook) shellHook;
        jobLogger.log("INFO", "start upload artifact: " + hook.getLocalFile());

        hookResponse.setObjectKey(hook.getObjectKey());
        BosClientConfiguration config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceSessionCredentials(
                hook.getAccessKeyId(), hook.getSecretAccessKey(), hook.getSessionToken()));
        if (StringUtils.isNotEmpty(hook.getEndPoint())) {
            config.setEndpoint(hook.getEndPoint());
        }
        try {
            BosClient client = new BosClient(config);
            File file = new File(ws + File.separator + hook.getLocalFile());
            if (new File(hook.getLocalFile()).isAbsolute()) {
                file = new File(hook.getLocalFile());
            }
            if (!file.exists()) {
                throw new AgentException("file not exists!");
            }
            if (!file.isFile()) {
                throw new AgentException("file type error!");
            }
            client.putObject(hook.getBucketName(), hook.getObjectKey(), file);
            hookResponse.setSuccess(true);
            hookResponse.setFileSizeBytes(file.length());
            jobLogger.log("INFO", "upload artifact success!");
        } catch (Exception e) {
            LOGGER.error("bos upload exception!", e);
            if (hook.isAllowFail()) {
                jobLogger.log("WARN", ExceptionUtils.getFullStackTrace(e));
            } else {
                jobLogger.log("ERROR", ExceptionUtils.getFullStackTrace(e));
            }
            hookResponse.setSuccess(false);
            hookResponse.setError(e.getMessage());
        }
        return hookResponse;
    }
}
