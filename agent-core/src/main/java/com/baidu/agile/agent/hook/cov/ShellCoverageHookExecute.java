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

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.hook.HookExecute;
import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.agent.run.artifact.FileCompress;
import com.baidu.agile.server.job.bean.hook.ShellHook;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.cov.CoverageErrorResponse;
import com.baidu.agile.server.job.bean.hook.cov.CoverageSuccResponse;
import com.baidu.agile.server.job.bean.hook.cov.ShellCoverageHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellCoverageHookResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ShellCoverageHookExecute implements HookExecute {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellCoverageHookExecute.class);

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
        ShellCoverageHook hook = (ShellCoverageHook) shellHook;
        jobLogger.log("INFO", "start upload cov file: " + hook.getCovFile());
        Map<String, String> params = hook.getParams();
        if (MapUtils.isEmpty(params)) {
            params = new HashMap();
        }
        params.put("module_path", hook.getModulePath());
        params.put("module_branch", hook.getModuleBranch());
        params.put("revision", hook.getRevision());
        params.put("base_revision", hook.getBaseRevision());
        params.put("change_id", hook.getChangeId());
        params.put("method", hook.getEngmapKey());
        params.put("author", hook.getCheckinAuthor());
        params.put("commit_time", hook.getCommitTimeSecond());
        params.put("language", hook.getLanguage());
        params.put("callback_url", hook.getCallbackUrl());
        params.put("kind", "SHELL");

        ShellCoverageHookResponse hookResponse = new ShellCoverageHookResponse();
        hookResponse.setHookId(shellHook.getHookId());

        CovResponseResult response = null;
        try {
            Map<String, FileBody> fileParam = getFileParam(hook, uuid, ws, jobLogger);
            LOGGER.info("Request url: {}, params:{}.", hook.getRequestUrl(), params.toString());
            response = CovHttpClientUtil.postWithFileBody(
                    hook.getRequestUrl(), params, fileParam, hook.getTimeoutSecond() * 1000);
            LOGGER.info("Response is {}.", response);
        } catch (Exception e) {
            jobLogger.log("ERROR", ExceptionUtils.getFullStackTrace(e));
            LOGGER.error("Request cov api fail! Exception:{}", e);
            hookResponse.setSuccess(false);
            hookResponse.setError(e.getMessage());
            return hookResponse;
        }

        if (response == null) {
            hookResponse.setSuccess(false);
            hookResponse.setError("Response of Cov platform is empty.");
        } else {
            switch (response.getStatusCode()) {
                case 200: {
                    try {
                        CoverageSuccResponse succResponse = MAPPER.readValue(response.getResult(),
                                CoverageSuccResponse.class);
                        hookResponse.setSuccess(true);
                        hookResponse.setSuccResponse(succResponse);
                        jobLogger.log("INFO", "upload cov file success!");
                    } catch (IOException e) {
                        LOGGER.error("parse response result failed, response: {}, exception:{}.",
                                response.getResult(), e);
                        hookResponse.setSuccess(false);
                        hookResponse.setError("parse response result failed for json: " + response.getResult());
                        jobLogger.log("ERROR", hookResponse.getError());
                    }
                    break;
                }
                default: {
                    hookResponse.setSuccess(false);
                    String error = "error response code:" + response.getStatusCode();
                    try {
                        CoverageErrorResponse errorResponse = MAPPER.readValue(response.getResult(),
                                CoverageErrorResponse.class);
                        error = StringUtils.defaultString(errorResponse.getErrorMessage(), error);
                    } catch (Exception e) {
                        LOGGER.error("parse response result failed, response: {}, exception:{}.",
                                response.getResult(), e);
                    }
                    hookResponse.setError(error);
                    jobLogger.log("ERROR", error);
                    break;
                }
            }
        }
        return hookResponse;
    }

    private Map<String, FileBody> getFileParam(ShellCoverageHook hook, String uuid, String ws, IJobLogger jobLogger) {
        String covFile = hook.getCovFile();
        if (StringUtils.isEmpty(covFile)) {
            throw new AgentException("cov file is empty, invalid file path!");
        }

        Map<String, FileBody> fileParam = new HashMap<String, FileBody>();

        if (!new File(covFile).isAbsolute()) {
            covFile = ws + File.separator + covFile;
        }

        if (new File(covFile).exists()) {
            fileParam.put("cov_file", new FileBody(new File(covFile)));
        } else {
            throw new AgentException("cov file:" + covFile + " not exist!");
        }

        String reportDir = hook.getReportDir();
        if (StringUtils.isEmpty(reportDir)) {
            return fileParam;
        }

        if (!new File(reportDir).isAbsolute()) {
            reportDir = ws + File.separator + reportDir;
        }
        if (reportDir.endsWith(File.separator)) {
            reportDir = reportDir.substring(0, reportDir.length() - 2);
        }
        if (new File(reportDir).exists()) {
            if (new File(reportDir).isDirectory()) {
                jobLogger.log("INFO", "compress report dir: " + hook.getReportDir());
                try {
                    String tarFilePath = reportDir + ".zip";
                    FileCompress.compress(reportDir, tarFilePath, "");
                    fileParam.put("report", new FileBody(new File(tarFilePath), ContentType.parse("application/zip")));
                } catch (Exception e) {
                    LOGGER.error("Compress file is error for uuid: {}.", uuid, e);
                    jobLogger.log("WARN", "compress file exception: " + e.getMessage());
                }
            } else {
                fileParam.put("report", new FileBody(new File(reportDir)));
            }
        } else {
            jobLogger.log("WARN", "report dir/file not exist: " + hook.getReportDir());
        }
        return fileParam;
    }
}
