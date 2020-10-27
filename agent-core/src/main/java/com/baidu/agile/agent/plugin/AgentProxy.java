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

package com.baidu.agile.agent.plugin;

import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.job.PluginJob;
import com.baidu.agile.agent.log.IJobMultiAsyncLogger;
import com.baidu.agile.agent.log.SaAsyncMultiLogger;
import com.baidu.agile.server.agent.bean.AgentResponse;
import com.baidu.agile.server.agent.bean.job.AgentJobResult;
import com.baidu.agile.server.agent.plugin.IAgentProxy;
import com.baidu.agile.server.common.ObjectResponse;
import com.baidu.agile.server.exception.PluginUpdateEndStatusException;
import com.baidu.agile.server.job.bean.JobBaseOutput;
import com.baidu.agile.server.job.bean.JobBaseParameter;
import com.baidu.agile.server.job.bean.JobStatus;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent proxy for plugin
 */
public class AgentProxy implements IAgentProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentProxy.class);

    private String uuid;

    private String ws;

    // 中断标识
    private boolean interruptedFlag = false;

    private Map<String, String> envVars;

    private JobBaseParameter jobBaseParameter;

    private IJobMultiAsyncLogger jobMultiAsyncLogger;

    private Object updateOutputLock = new Object();

    public AgentProxy(String uuid, JobBaseParameter jobBaseParameter) {
        this.uuid = uuid;
        this.jobBaseParameter = jobBaseParameter;
        this.envVars = new HashMap<String, String>();
        if (MapUtils.isNotEmpty(jobBaseParameter.getParameters())) {
            envVars.putAll(jobBaseParameter.getParameters());
        }
        this.ws = Context.ws(uuid);
        this.jobMultiAsyncLogger = new SaAsyncMultiLogger(false);
    }

    @Override
    public void log(final String logUuid, String data) {
        this.jobMultiAsyncLogger.log(logUuid, data);
    }

    public void startLogger() {
        this.jobMultiAsyncLogger.start();
    }

    public void stopLogger() {
        this.jobMultiAsyncLogger.end();
    }

    @Override
    public String readEnvVar(String varName) {
        return MapUtils.isNotEmpty(envVars) ? envVars.get(varName) : null;
    }

    @Override
    public void exportEnvVar(String varName, String varValue) {
        if (StringUtils.isNotBlank(varName)) {
            envVars.put(varName, varValue);
        }
    }

    @Override
    public void writeOutput(String name, String value) {
        String param = name + '=' + value + '\n';
        synchronized (updateOutputLock) {
            File file = new File(ws + File.separator + PluginJob.OUT_PARAMETER_FILE);
            try {
                FileUtils.write(file, param, "utf-8", true);
            } catch (IOException e) {
                LOGGER.error("write outParameters file exception", e);
            }
        }
    }

    @Override
    public boolean isInterrupted() {
        return interruptedFlag;
    }

    public void setInterruptedFlag(boolean flag) {
        interruptedFlag = flag;
    }


    @Override
    public void updateJobOutputWithRunningStatus(JobBaseOutput output) throws PluginUpdateEndStatusException {
        if (null == output) {
            LOGGER.error("output is null!");
            return;
        }
        if (null != output.getStatus() && !JobStatus.RUNNING.equals(output.getStatus())) {
            throw new PluginUpdateEndStatusException();
        }
        output.setJobUuid(uuid);
        output.setStatus(JobStatus.RUNNING);

        AgentJobResult<JobBaseOutput> request = new AgentJobResult<JobBaseOutput>();
        request.setAgentUuid(Context.getServerContext().getSecureKey());
        request.setOutput(output);
        ObjectResponse<AgentResponse> response = new HttpClient(Context.getAgentArgs().getServer())
                .path("/agent/job/v1/update")
                .jsonBody(request)
                .timeout(30000)
                .retry(10)
                .post(new ObjectResponse<AgentResponse>().getClass());

        if (null == response) {
            LOGGER.error("notice job:{} status return null!", output.getJobUuid());
            return;
        }

        if (!ObjectResponse.STATUS_OK.equals(response.getStatus())) {
            LOGGER.error("notice job:{} ,return: code:{}, error:{}",
                    output.getJobUuid(), response.getStatus(), response.getMessage());
        }
    }

    @Override
    public int runCommand(String cmd, String logFileName, String uuid, String ws) {
        int retValue = 1;
        String scriptFile = scriptFile(uuid);
        try {
            FileUtils.write(new File(scriptFile), cmd);

            ProcessBuilder processBuilder = new ProcessBuilder()
                    .command(new String[] {"sh", "-xe", scriptFile})
                    .directory(new File(ws))
                    .redirectErrorStream(true);
            Map<String, String> env = processBuilder.environment();
            env.put(PluginJob.AGENT_JOB_BUILD_ID, uuid);
            if (MapUtils.isNotEmpty(envVars)) {
                env.putAll(envVars);
            }
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            process.getOutputStream().close();
            // newLog2FileThread(uuid, process.getInputStream(), logFileName);
            this.jobMultiAsyncLogger.log(uuid, process.getInputStream());
            retValue = process.waitFor();
        } catch (Exception e) {
            LOGGER.error("run command process fail", e);
        } finally {
            try {
                FileUtils.forceDelete(new File(scriptFile));
            } catch (IOException e) {
                LOGGER.error("clear tmp script file failed! [name: {}]", scriptFile);
            }
        }
        return retValue;
    }

    @Override
    public int runCommand(String cmd, String logFileName) {
        return runCommand(cmd, logFileName, uuid, ws);
    }

    private String scriptFile(String uuid) {
        return System.getProperty("java.io.tmpdir") + File.separator + uuid + ".agent.tmp.sh";
    }
}
