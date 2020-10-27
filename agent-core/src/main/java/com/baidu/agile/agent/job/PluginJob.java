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
import com.baidu.agile.agent.plugin.AgentProxy;
import com.baidu.agile.agent.plugin.PluginContext;
import com.baidu.agile.agent.plugin.PluginManager;
import com.baidu.agile.agent.process.EnvVarsMatcher;
import com.baidu.agile.agent.process.ProcessTree;
import com.baidu.agile.server.agent.plugin.IExecutor;
import com.baidu.agile.server.agent.plugin.PluginExecutorWrapper;
import com.baidu.agile.server.exception.PluginClassLoadException;
import com.baidu.agile.server.job.bean.JobBaseOutput;
import com.baidu.agile.server.job.bean.JobBaseParameter;
import com.baidu.agile.server.job.bean.JobStatus;
import com.baidu.agile.server.job.bean.concrete.ShellJobOutput;
import com.baidu.agile.server.utils.PluginClasspathLoader;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件job
 */
public class PluginJob extends Job<JobBaseParameter, JobBaseOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginJob.class);

    private String ws;

    private PluginExecutorWrapper pluginExecutorWrapper;

    private IExecutor executor;

    private JobBaseOutput jobBaseOutput;

    private PluginContext jobContext;

    private AgentProxy agentProxy;

    public static final String OUT_PARAMETER_FILE = "AGILE_OUT";

    public static final String AGENT_JOB_BUILD_ID = "AGENT_JOB_BUILD_ID";

    @Override
    public void start() {

        this.status = JobStatus.RUNNING;
        notice();

        try {
            init();

            LOGGER.info("execute plugin job, parameter: {}", jobContext.getJobParameter());
            jobBaseOutput = executor.execute(jobContext, agentProxy);
            LOGGER.info("execute plugin job end, output: {}", jobBaseOutput);

            end(jobBaseOutput.getStatus());
        } catch (Exception e) {
            LOGGER.error("execute plugin job error", e);
            end(JobStatus.FAILED);
        } finally {
            after();
        }

    }

    private void after() {
        agentProxy.stopLogger();
        cleanWS();
    }

    private void init() throws IOException, PluginClassLoadException {
        JobBaseParameter parameter = getAgentJob().getJobParameter();
        // 此parameter.getClass()类加载的时机：拉取任务，当发现是插件任务的时候，json反序列化会加载插件
        pluginExecutorWrapper = PluginManager.getPluginExecutorByParameterType(parameter.getClass().getName());
        if (null == pluginExecutorWrapper || null == pluginExecutorWrapper.getExecutorInstance()) {
            LOGGER.error("Plugin executor not found, ParameterType: {}", parameter.getClass().getName());
            throw new PluginClassLoadException("Plugin executor not found, parameter class name: "
                    + parameter.getClass().getName());
        }
        executor = pluginExecutorWrapper.getExecutorInstance();
        jobContext = new PluginContext(getAgentJob().getJobUuid(), parameter, pluginExecutorWrapper);
        agentProxy = new AgentProxy(getAgentJob().getJobUuid(), parameter);

        agentProxy.startLogger();

        ws = Context.ws(getAgentJob().getJobUuid());
        FileUtils.forceMkdir(new File(ws));
        LOGGER.info("create workdir: {}", ws);
    }

    @Override
    public void stop(String cause) {
        killAllProcess();
        try {
            agentProxy.setInterruptedFlag(true);
            boolean killed = executor.kill(jobContext, agentProxy);
            if (killed) {
                LOGGER.info("kill plugin job success");
                end(JobStatus.KILLED);
                after();
            } else {
                LOGGER.info("kill plugin job failed");
                end(JobStatus.KILL_FAILED);
            }
        } catch (Exception e) {
            LOGGER.info("kill plugin job failed");
            end(JobStatus.KILL_FAILED);
            after();
        }
    }

    private void killAllProcess() {
        try {
            ProcessTree.getTree().killAll(new EnvVarsMatcher(AGENT_JOB_BUILD_ID, getAgentJob().getJobUuid()));
        } catch (Exception e) {
            LOGGER.error("destroy process exception", e);
        }
    }

    private void cleanWS() {
        if (Context.getAgentArgs().isDurable()) {
            return;
        }
        try {
            FileUtils.deleteDirectory(new File(ws));
        } catch (Exception e) {
            LOGGER.error("exception delete workspace for job:" + getAgentJob().getJobUuid(), e);
        }
    }


    @Override
    protected JobBaseOutput result(JobStatus status) {
        JobStatus finalStatus = null == status ? JobStatus.FAILED : status;
        if (null == jobBaseOutput) {
            JobBaseParameter parameter = getAgentJob().getJobParameter();
            try {
                jobBaseOutput = (JobBaseOutput) PluginClasspathLoader.getClassLoader()
                        .loadClass(parameter.getOutputType()).newInstance();
            } catch (Exception e) {
                LOGGER.error("create output instance error!", e);
                jobBaseOutput = new ShellJobOutput();
            }
        }
        jobBaseOutput.setStatus(finalStatus);
        jobBaseOutput.setJobUuid(getAgentJob().getJobUuid());
        jobBaseOutput.setOutParameters(outParameters());
        return jobBaseOutput;
    }

    private Map<String, String> outParameters() {
        Map<String, String> outParameters = new HashMap<String, String>();
        File file = new File(ws + File.separator + OUT_PARAMETER_FILE);
        if (file.exists() && file.isFile()) {
            try {
                for (String line : FileUtils.readFileToString(file).split("\r\n|\n")) {
                    int sep = line.indexOf('=');
                    if (sep > 0) {
                        outParameters.put(line.substring(0, sep), line.substring(sep + 1));
                    }
                }
            } catch (IOException e) {
                LOGGER.error("parse outParameters " + OUT_PARAMETER_FILE + " exception", e);
            }
        }
        return outParameters;
    }
}
