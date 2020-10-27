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

package com.baidu.agile.agent.execute;

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.common.response.Response;
import com.baidu.agile.agent.common.util.LocalUtils;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.hook.HookExecuteFactory;
import com.baidu.agile.agent.job.JobManager;
import com.baidu.agile.agent.job.ShellJob;
import com.baidu.agile.agent.log.IJobAsyncLogger;
import com.baidu.agile.agent.log.SaAsyncLogger;
import com.baidu.agile.agent.os.OS;
import com.baidu.agile.agent.process.EnvVarsMatcher;
import com.baidu.agile.agent.process.ProcessTree;
import com.baidu.agile.agent.run.artifact.FileHttpUploader;
import com.baidu.agile.agent.run.artifact.JobArtifact;
import com.baidu.agile.agent.wrapper.ShellWrapperUtil;
import com.baidu.agile.server.job.bean.JobStatus;
import com.baidu.agile.server.job.bean.concrete.ScriptType;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;
import com.baidu.agile.server.job.bean.hook.ShellHook;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.cov.ShellCoverageHookResponse;
import com.baidu.agile.server.job.bean.report.Report;
import com.baidu.agile.server.job.bean.report.ReportType;
import com.baidu.agile.server.job.bean.wrapper.ShellWraper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 进程执行
 */
public class ShellProcessBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellProcessBuilder.class);

    private String uuid;

    private ScriptType scriptType;

    private String command;

    private Map<String, String> envs;

    private String scriptFile;

    private String ws;

    private String agentName;

    private boolean killDescendant;

    private List<Report> reports;

    private List<ShellHook> hooks;

    private List<ShellWraper> wrappers;

    // 用于存放hook的response
    private List<ShellHookResponse> hookResponses;

    private IJobAsyncLogger jobAsyncLogger;

    private ShellJobParameter.RetainWorkspaceCondition retainWorkspaceCondition;

    public ShellProcessBuilder(String uuid, ScriptType scriptType, String command, Map<String, String> envs,
                               String ws, String agentName) {
        this.uuid = uuid;
        if (null == scriptType) {
            this.scriptType = OS.WINDOWS.equals(OS.os()) ? ScriptType.BAT : ScriptType.SH;
        } else {
            this.scriptType = scriptType;
        }
        this.agentName = agentName;
        this.command = command;
        this.envs = envs;
        this.scriptFile = scriptFile(uuid);
        this.ws = StringUtils.defaultIfBlank(ws, Context.ws(uuid));
        this.jobAsyncLogger = new SaAsyncLogger(this.uuid, true);
    }

    private String scriptFile(String uuid) {
        return System.getProperty("java.io.tmpdir") + File.separator
                + uuid + ".agent.tmp" + scriptType.getFileExtension();
    }

    public ShellProcessBuilder reports(List<Report> reports) {
        this.reports = reports;
        return this;
    }

    public ShellProcessBuilder hook(List<ShellHook> hooks) {
        this.hooks = hooks;
        this.hookResponses = new ArrayList<ShellHookResponse>();
        return this;
    }

    public ShellProcessBuilder wrappers(List<ShellWraper> wrappers) {
        this.wrappers = wrappers;
        return this;
    }

    public ShellProcessBuilder killDescendant(boolean killDescendant) {
        this.killDescendant = killDescendant;
        return this;
    }

    public ShellProcessBuilder retainWorkspaceCondition(
            ShellJobParameter.RetainWorkspaceCondition retainWorkspaceCondition) {
        this.retainWorkspaceCondition = retainWorkspaceCondition;
        return this;
    }

    public List<ShellHookResponse> getHookResponses() {
        return hookResponses;
    }

    public Process start() throws IOException {
        this.jobAsyncLogger.start();
        if (StringUtils.isNotEmpty(agentName)) {
            this.jobAsyncLogger.log("INFO", String.format("Running on (%s) with agent(v%s) in workspace %s",
                    agentName, Context.VERSION, new File(ws).getAbsolutePath()));
        } else {
            this.jobAsyncLogger.log("INFO", String.format("Running on %s(%s) with agent(v%s) in workspace %s",
                    LocalUtils.getHostName(), LocalUtils.getHostIp(), Context.VERSION, new File(ws).getAbsolutePath()));
        }
        try {
            before();

            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(command());
            processBuilder.directory(new File(ws));
            processBuilder.redirectErrorStream(true);
            Map<String, String> env = processBuilder.environment();
            env.put(ShellJob.AGENT_JOB_BUILD_ID, uuid);
            env.put(ShellJob.WORKSPACE, ws);
            if (MapUtils.isNotEmpty(envs)) {
                for (Map.Entry<String, String> entry : envs.entrySet()) {
                    env.put(entry.getKey(), StringUtils.defaultString(entry.getValue()));
                }
            }
            Process process = processBuilder.start();

            // close interaction in case of blocked because of waiting keyboard input
            process.getOutputStream().close();

            // 实时日志线程
            LOGGER.info("start collect log.");
            this.jobAsyncLogger.log(process.getInputStream());

            // 等到任务结束线程
            newTaskThread(uuid, process);

            return process;
        } catch (IOException e) {
            this.jobAsyncLogger.log("ERROR", "start process error:" + e.getMessage());
            LOGGER.error("start process error", e);
            throw e;
        }
    }

    public void clearFile() throws IOException {
        try {
            FileUtils.deleteDirectory(new File(ws));
        } catch (Exception e) {
            LOGGER.error("exception delete workspace for job:" + uuid, e);
        }
        try {
            FileUtils.forceDelete(new File(scriptFile));
        } catch (FileNotFoundException fe) {
            // do nothing
        } catch (Exception e) {
            LOGGER.error("exception delete tmp script file for job:" + uuid, e);
        }

    }

    public void log(String level, String line) {
        this.jobAsyncLogger.log(level, line);
    }

    private void before() throws IOException {
        String command = StringUtils.defaultString(this.command);
        if (OS.WINDOWS.equals(OS.os())) {
            String lineSeparator = System.getProperty("line.separator");
            // exit %ERRORLEVEL%: 处理参考jenkins，防止windows出现退出码不正确的情况
            command = command.replace("\n", lineSeparator) + lineSeparator + "exit %ERRORLEVEL%";
        }
        FileUtils.write(new File(scriptFile), command, Charset.defaultCharset());
        FileUtils.forceMkdir(new File(ws));
    }

    protected String[] command() {
        switch (scriptType) {
            case BAT:
                return new String[] {"cmd", "/c", "call", scriptFile};
            case BASH:
                return new String[] {"bash", "-xe", scriptFile};
            case SH:
            default:
                return new String[] {"sh", "-xe", scriptFile};
        }
    }

    private void after(boolean doClearFile) throws IOException {
        if (doClearFile) {
            clearFile();
        }
    }

    /**
     * 执行任务的线程
     *
     * @param uuid    任务id
     * @param process
     * @return 任务执行成功返回true，失败返回false
     */
    private void newTaskThread(final String uuid, final Process process) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = false;
                try {
                    int returnVal = process.waitFor();
                    success = returnVal == 0;
                    try {
                        report();
                    } catch (Exception e) {
                        success = false;
                        LOGGER.error("Process report exception", e);
                    }
                    try {
                        // 强制触发Hook
                        success &= triggerHook();
                    } catch (Exception e) {
                        success = false;
                        LOGGER.error("trigger hook with status exception", e);
                    }

                    try {
                        if (killDescendant) {
                            // 杀掉进程的衍生进程以及自己
                            killSubprocess();
                        }
                    } catch (Exception e) {
                        LOGGER.error("kill subprocess exception", e);
                    }
                } catch (Exception e) {
                    LOGGER.error("wait process failed!!!", e);
                } finally {
                    try {
                        ShellWrapperUtil.after(ShellProcessBuilder.this, ShellProcessBuilder.this.wrappers);
                    } catch (Exception e) {
                        LOGGER.error("wrapper.after excpetion!", e);
                    }
                    try {
                        JobManager.endJob(uuid, success ? JobStatus.SUCC : JobStatus.FAILED);
                    } catch (Exception e) {
                        LOGGER.error("End job with status exception", e);
                    }
                    try {
                        after(isClearWorkspace(success, retainWorkspaceCondition));
                    } catch (IOException ioe) {
                        LOGGER.error("Exception:", ioe);
                    }
                    try {
                        jobAsyncLogger.end();
                    } catch (Exception e) {
                        LOGGER.error("Exception:", e);
                    }
                }
            }
        }).start();
    }

    private boolean isClearWorkspace(boolean success,
                                     ShellJobParameter.RetainWorkspaceCondition retainWorkspaceCondition) {
        boolean isClear = true;
        if (retainWorkspaceCondition == null) {
            // 默认清理
            return isClear;
        }

        switch (retainWorkspaceCondition) {
            case ALL:
                isClear = false;
                break;
            case JOB_FAIL:
                if (!success) {
                    isClear = false;
                }
                break;
            default:
                break;
        }
        return isClear;
    }

    private void report() throws Exception {
        if (CollectionUtils.isEmpty(reports)) {
            return;
        }
        try {
            reportStart(uuid);
        } catch (AgentException e) {
            this.jobAsyncLogger.log("ERROR", ExceptionUtils.getFullStackTrace(e));
            throw e;
        }
        for (Report report : reports) {
            try {
                this.jobAsyncLogger.log("INFO", "Start process report: " + report.getReportName());
                int fileCount = new JobArtifact(uuid, ws, jobAsyncLogger)
                        .fileUploader(new FileHttpUploader(Context.getServerContext().getReportServerUrl()))
                        .artifact(report.getArtifact())
                        .toDir(report.getReportName())
                        .upload();
                if (fileCount > 0) {
                    this.jobAsyncLogger.log("INFO", "Transferred (" + fileCount + ") files.\n");
                    reportFinish(uuid, report.getReportName(), report.getReportType());
                } else {
                    throw new AgentException("Find none files.\n");
                }
            } catch (Exception e) {
                if (BooleanUtils.isTrue(report.getAllowMissing())) {
                    this.jobAsyncLogger.log("WARN", ExceptionUtils.getFullStackTrace(e));
                } else {
                    this.jobAsyncLogger.log("ERROR", ExceptionUtils.getFullStackTrace(e));
                    throw e;
                }
            }
        }
    }

    public void reportStart(String uuid) {
        Response response = new HttpClient(Context.getServerContext().getReportServerUrl())
                .path("/report/preStart")
                .param("uuid", uuid)
                .retry(2)
                .get(Response.class);
        if (null == response) {
            throw new AgentException("prepare report fail, result is null!");
        }
        if (!response.isSuccess()) {
            throw new AgentException("prepare report fail, error:" + response.getError());
        }
    }

    public void reportFinish(String uuid, String report, ReportType reportType) {
        Response response = new HttpClient(Context.getServerContext().getReportServerUrl())
                .path("/report/finish")
                .param("uuid", uuid)
                .param("report", report)
                .param("reportType", reportType.name())
                .timeout(120 * 1000) // 结束上传后需要解析report，暂时设置时间为1分钟
                .retry(2)
                .get(Response.class);
        if (null == response) {
            throw new AgentException("finish or parse report fail, result is null!");
        }
        if (!response.isSuccess()) {
            throw new AgentException("finish or parse report fail, error:" + response.getError());
        }
    }

    /**
     * 触发Hook
     */
    private boolean triggerHook() throws Exception {
        boolean flag = true;
        if (CollectionUtils.isNotEmpty(hooks)) {
            for (ShellHook hook : hooks) {
                ShellHookResponse shellHookResponse = HookExecuteFactory.execute(hook, uuid, ws, jobAsyncLogger);
                if (shellHookResponse != null) {
                    hookResponses.add(shellHookResponse);
                    // 其他hook如果失败，由上层业务进行判断
                    if (shellHookResponse instanceof ShellCoverageHookResponse) {
                        flag &= ((ShellCoverageHookResponse) shellHookResponse).isSuccess();
                    }
                } else {
                    flag = false;
                }
            }
        }
        return flag;
    }

    private boolean killSubprocess() throws Exception {
        // 通过环境变量得到agent启动的任务的进程以及衍生进程
        ProcessTree.getTree().killAll(new EnvVarsMatcher(ShellJob.AGENT_JOB_BUILD_ID, uuid));
        return true;
    }

    public String getUuid() {
        return uuid;
    }

    public Map<String, String> getEnvs() {
        return envs;
    }

    public void setEnvs(Map<String, String> envs) {
        this.envs = envs;
    }

    public ShellJobParameter.RetainWorkspaceCondition getRetainWorkspaceCondition() {
        return retainWorkspaceCondition;
    }

    public void setRetainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition retainWorkspaceCondition) {
        this.retainWorkspaceCondition = retainWorkspaceCondition;
    }
}
