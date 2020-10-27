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

package com.baidu.agile.agent.job;

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.execute.ShellProcessBuilder;
import com.baidu.agile.agent.process.EnvVarsMatcher;
import com.baidu.agile.agent.process.ProcessTree;
import com.baidu.agile.agent.wrapper.ShellWrapperUtil;
import com.baidu.agile.server.job.bean.JobStatus;
import com.baidu.agile.server.job.bean.concrete.ShellJobOutput;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class ShellJob extends Job<ShellJobParameter, ShellJobOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellJob.class);

    public static final String AGENT_JOB_BUILD_ID = "AGENT_JOB_BUILD_ID";

    public static final String WORKSPACE = "WORKSPACE";

    private Process process;

    private ShellProcessBuilder shellProcessBuilder;

    private String ws;

    @Override
    public void start() {
        try {
            this.status = JobStatus.RUNNING;
            notice();
            ShellJobParameter shellJobParameter = this.getAgentJob().getJobParameter();
            this.ws = ws(this.getAgentJob().getJobUuid(), shellJobParameter.getWsDirType(),
                    shellJobParameter.getWsParentDir(), shellJobParameter.getWsDir());
            shellProcessBuilder = new ShellProcessBuilder(
                    this.getAgentJob().getJobUuid(),
                    shellJobParameter.getScriptType(),
                    shellJobParameter.getShellScript(),
                    shellJobParameter.getParameters(),
                    this.ws, shellJobParameter.getAgentName());
            shellProcessBuilder
                    .reports(shellJobParameter.getReports())
                    .hook(shellJobParameter.getShellHook())
                    .wrappers(shellJobParameter.getWrapers())
                    .retainWorkspaceCondition(shellJobParameter.getRetainWorkspaceCondition())
                    .killDescendant(shellJobParameter.isKillDescendant());
            ShellWrapperUtil.before(shellProcessBuilder, shellJobParameter.getWrapers());
            process = shellProcessBuilder.start();
        } catch (Exception e) {
            LOGGER.error("exception when start job: " + this.getAgentJob(), e);
            end(JobStatus.FAILED);
        }
    }

    public String ws(String uuid, ShellJobParameter.WsDirType wsDirType, String wsParentDir, String wsDir) {
        if (ShellJobParameter.WsDirType.FIXED.equals(wsDirType)) {
            if (StringUtils.isEmpty(wsDir)) {
                return Context.ws(uuid);
            } else if (new File(wsDir).isAbsolute()) {
                return wsDir;
            } else {
                return Context.ws() + File.separator + wsDir;
            }
        } else {
            if (StringUtils.isEmpty(wsParentDir)) {
                return Context.ws(uuid);
            } else if (new File(wsParentDir).isAbsolute()) {
                return wsParentDir + File.separator + uuid;
            } else {
                return Context.ws() + File.separator + wsParentDir + File.separator + uuid;
            }
        }
    }

    @Override
    public void stop(String cause) {
        if (null == process) {
            return;
        }
        String message = "Stop this job! ";
        if (StringUtils.isNotEmpty(cause)) {
            message += "Cause: " + cause;
        }
        try {
            shellProcessBuilder.log("INFO", message);
            end(JobStatus.KILLED);
            ProcessTree.getTree().killAll(new EnvVarsMatcher(AGENT_JOB_BUILD_ID, this.getAgentJob().getJobUuid()));
            process.destroy();
        } catch (Exception e) {
            LOGGER.error("destroy process exception", e);
            end(JobStatus.KILL_FAILED);
        } finally {
            if (!Context.getAgentArgs().isDurable()) {
                try {
                    shellProcessBuilder.clearFile();
                } catch (Exception e) {
                    LOGGER.error("clear job build files exception", e);
                }
            }
        }
    }

    @Override
    protected ShellJobOutput result(JobStatus status) {
        ShellJobOutput result = new ShellJobOutput();
        result.setJobUuid(getAgentJob().getJobUuid());
        result.setLogUuid(getAgentJob().getJobUuid());
        result.setStatus(status);
        // 初始化时没有shellProcessBuilder
        if (this.shellProcessBuilder != null) {
            result.setHookResponse(shellProcessBuilder.getHookResponses());
        }
        result.setOutParameters(outParameters());
        if (JobStatus.FAILED.equals(status) && null != process) {
            try {
                result.setExitCode(process.exitValue());
            } catch (Exception e) {
                LOGGER.error("get process exit code exception", e);
            }
        }
        return result;
    }

    //  "AGILE_OUT": jenkins default file for agile
    private static final String[] OUT_PARAMETER_FILES = {"export", "AGILE_OUT"};

    private Map<String, String> outParameters() {
        Map<String, String> outParameters = new HashMap<String, String>();
        for (String fileName : OUT_PARAMETER_FILES) {
            File file = new File(this.ws + File.separator + fileName);
            if (file.exists() && file.isFile()) {
                try {
                    for (String line : FileUtils.readFileToString(file, Charset.defaultCharset()).split("\r\n|\n")) {
                        int sep = line.indexOf('=');
                        if (sep > 0) {
                            outParameters.put(line.substring(0, sep), line.substring(sep + 1));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("parse outParameters " + fileName + " exception", e);
                }
            }
        }
        return outParameters;
    }
}
