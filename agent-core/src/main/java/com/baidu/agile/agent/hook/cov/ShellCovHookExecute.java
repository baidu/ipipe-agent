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

package com.baidu.agile.agent.hook.cov;

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.hook.HookExecute;
import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.agent.log.LogService;
import com.baidu.agile.agent.run.artifact.FileCompress;
import com.baidu.agile.server.job.bean.hook.ShellHook;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.cov.CovResponseError;
import com.baidu.agile.server.job.bean.hook.cov.ShellCovHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellCovHookResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by niuwanpeng on 17/12/21.
 *
 * cov hook执行者
 */
@Deprecated
public class ShellCovHookExecute implements HookExecute {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellCovHookExecute.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 反序列化时忽未知字段
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * shell命令结束之后触发hook操作
     */
    @Override
    public ShellHookResponse triggerHook(ShellHook shellHook, String uuid, String ws, IJobLogger jobLogger) {
        ShellCovHook covHook = (ShellCovHook) shellHook;
        Map<String, String> params = new HashMap();
        params.put("commit_time", covHook.getCommitTime().toString());
        params.put("author", covHook.getAuthor());
        params.put("module_path", covHook.getModulePath());
        params.put("branch", covHook.getBranch());
        params.put("revision", covHook.getRevision());
        params.put("method", covHook.getMethod());
        params.put("tool", covHook.getTool());
        Map<String, File> fileParam = getFileParam(covHook, uuid, ws);
        CovResponseResult response = null;
        try {
            LOGGER.info("Request url: {}, params:{}.", covHook.getRequestUrl(), params.toString());
            response = CovHttpClientUtil.post(covHook.getRequestUrl(), params, fileParam, 60 * 1000);
            LOGGER.info("Response is {}.", response);
        } catch (IOException e) {
            LOGGER.error("Request cov api fail! Exception:", e);
        }
        ShellCovHookResponse covHookResponse = null;
        if (response == null) {
            LogService.saveOrAppendLog(uuid, "Response of Cov platform is empty.");
        } else {
            switch (response.getStatusCode()) {
                case 200: {
                    try {
                        covHookResponse = MAPPER.readValue(response.getResult(), ShellCovHookResponse.class);
                        covHookResponse.setHookId(covHook.getHookId());
                        LOGGER.info("Request Cov platform is successful by hookId:{}!", covHook.getHookId());
                    } catch (IOException e) {
                        LOGGER.error("parse response result failed, response: {}, exception:{}.",
                                response.getResult(), e);
                    }
                    break;
                }
                case 400: {
                    try {
                        Map<String, String> resultFor400 = MAPPER.readValue(response.getResult(),
                                new TypeReference<HashMap<String, String>>() { });
                        CovResponseError error = CovResponseError.valueOf(resultFor400.get("error"));
                        LogService.saveOrAppendLog(uuid, error.getError());
                    } catch (IOException e) {
                        LOGGER.error("parse response result failed, response: {}, exception:{}.",
                                response.getResult(), e);
                    }
                    break;
                }
                case 500: {
                    LogService.saveOrAppendLog(uuid, "Cov platform server is exception!");
                    break;
                }
                default: {
                    LogService.saveOrAppendLog(uuid,
                            "An unknown error occur when attempting to request url.");
                    break;
                }
            }
        }
        return covHookResponse;
    }

    /**
     * 组装file参数
     * @param covHook
     * @return
     */
    private Map<String, File> getFileParam(ShellCovHook covHook, String uuid, String ws) {
        Map<String, File> fileParam = new HashMap<String, File>();
        String covFilePath = covHook.getCovFilePath();
        String supportFilePath = covHook.getSupportHtmlPath();

        if (StringUtils.isNotEmpty(supportFilePath)) {
            String fromDir = ws + File.separator + supportFilePath;
            String tarFilePath = fromDir + uuid + ".tar.gz";
            try {
                FileCompress.compress(fromDir, tarFilePath, "");
            } catch (Exception e) {
                LOGGER.error("Compress file is error for uuid: {}.", uuid, e);
            }
            fileParam.put("html_report", new File(tarFilePath));
        }
        fileParam.put("cover_file", new File(Context.ws(uuid) + File.separator + covFilePath));
        return fileParam;
    }
}
