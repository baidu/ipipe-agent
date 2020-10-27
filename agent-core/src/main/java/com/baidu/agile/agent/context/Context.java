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

package com.baidu.agile.agent.context;

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.Args;
import com.baidu.agile.agent.common.util.LocalUtils;
import com.baidu.agile.agent.log.SaAsyncLogger;
import com.baidu.agile.agent.os.OS;
import com.baidu.agile.server.agent.bean.AgentResponse;
import com.baidu.agile.server.agent.bean.Heartbeat;
import com.baidu.agile.server.agent.bean.RedisBaseConfigs;
import com.baidu.agile.server.agent.bean.RegisterAgentRequest;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;
import com.baidu.agile.server.agent.bean.plugin.PluginInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

public class Context {

    public static final String VERSION = "1.2.6";

    private static final String WS = System.getProperty("user.dir") + File.separator + "workspace";
    public static final String PROCESS_ID = "processId";

    private static ServerContext serverContext;

    private static String[] args;

    private static Args agentArgs;

    // 日志会传输到log服务的log-agent.log
    private static SaAsyncLogger REMOTE_LOGGER_FOR_AGENT;

    private static final String REMOTE_LOGGER_FOR_AGENT_DATA_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";

    private static final String REMOTE_LOGGER_FOR_AGENT_TIME_ZONE = "GMT+8";

    public static void log(String... contents) {
        String uuid = Context.getAgentArgs().getUuid();
        REMOTE_LOGGER_FOR_AGENT.log(uuid, StringUtils.join(contents, "\n"));
    }

    public static void log(String content, Throwable e) {
        log(content, ExceptionUtils.getFullStackTrace(e));
    }

    public static void flush() {
        REMOTE_LOGGER_FOR_AGENT.flush();
    }

    public static String ws() {
        return WS;
    }

    public static String ws(String uuid) {
        return WS + File.separator + uuid;
    }

    public static ServerContext getServerContext() {
        return serverContext;
    }

    public static void setServerContext(ServerContext serverContext) {
        Context.serverContext = serverContext;
    }

    public static void setContext(AgentResponse response) {
        ServerContext serverContext = new ServerContext();
        serverContext.setSecureKey(response.getAgent().getSecureKey());
        serverContext.setRedisConfigs(response.getRedisConfigs());
        serverContext.setJobQueueRedisConfigs(response.getJobQueueRedisConfigs());
        serverContext.setLogServerUrl(response.getLogServerUrl());
        serverContext.setReportServerUrl(response.getReportServerUrl());

        Context.serverContext = serverContext;
    }

    public static String[] getArgs() {
        return args;
    }

    public static Args getAgentArgs() {
        return agentArgs;
    }

    public static void init(String[] args, Args agentArgs) {
        Context.args = args;
        Context.agentArgs = agentArgs;
        File ws = new File(WS);
        if (ws.exists()) {
            if (!ws.isDirectory()) {
                if (!ws.delete()) {
                    throw new AgentException("clear workspace file fail!");
                }
                if (!ws.mkdirs()) {
                    throw new AgentException("create workspace file fail!");
                }
            }
        } else {
            if (!ws.mkdirs()) {
                throw new AgentException("create workspace file fail!");
            }
        }
        REMOTE_LOGGER_FOR_AGENT = new SaAsyncLogger("LOGGER_FOR_AGENT",
                REMOTE_LOGGER_FOR_AGENT_DATA_FORMAT,
                REMOTE_LOGGER_FOR_AGENT_TIME_ZONE);
    }

    public static void setAgentUpdateInfo(SystemCmdInfoFactory.AgentUpdateInfo agentUpdateInfo) {
        if (Context.serverContext == null) {
            Context.serverContext = new ServerContext();
        }
        Context.serverContext.setAgentUpdateInfo(agentUpdateInfo);
    }

    public static RegisterAgentRequest registerRequest() {
        RegisterAgentRequest request = new RegisterAgentRequest();
        request.setVersion(VERSION);
        request.setUuid(agentArgs.getUuid());
        if (null != serverContext) {
            request.setSecureKey(serverContext.getSecureKey());
        }
        request.setLabels(labels());
        return request;
    }

    public static Heartbeat heartRequest() {
        Heartbeat request = new Heartbeat();
        request.setVersion(VERSION);
        request.setUuid(agentArgs.getUuid());
        if (null != serverContext) {
            request.setSecureKey(serverContext.getSecureKey());
            request.setConfigVersion(serverContext.getConfigVersion());
        }
        return request;
    }

    public static PluginInfo pluginInfoRequest(String pluginParamType) {
        PluginInfo request = new PluginInfo();
        request.setAgentVersion(VERSION);
        request.setJobParamType(pluginParamType);

        return request;
    }

    public static class ServerContext {
        private String secureKey;
        private String configVersion;
        private RedisBaseConfigs redisConfigs;
        private RedisBaseConfigs jobQueueRedisConfigs;
        private String logServerUrl;
        private String reportServerUrl;
        // 从server返回的agent信息
        private SystemCmdInfoFactory.AgentUpdateInfo agentUpdateInfo;

        public String getSecureKey() {
            return secureKey;
        }

        public void setSecureKey(String secureKey) {
            this.secureKey = secureKey;
        }

        public String getConfigVersion() {
            return configVersion;
        }

        public void setConfigVersion(String configVersion) {
            this.configVersion = configVersion;
        }

        public RedisBaseConfigs getRedisConfigs() {
            return redisConfigs;
        }

        public void setRedisConfigs(RedisBaseConfigs redisConfigs) {
            this.redisConfigs = redisConfigs;
        }

        public RedisBaseConfigs getJobQueueRedisConfigs() {
            return jobQueueRedisConfigs;
        }

        public void setJobQueueRedisConfigs(RedisBaseConfigs jobQueueRedisConfigs) {
            this.jobQueueRedisConfigs = jobQueueRedisConfigs;
        }

        public String getLogServerUrl() {
            return logServerUrl;
        }

        public void setLogServerUrl(String logServerUrl) {
            this.logServerUrl = logServerUrl;
        }

        public String getReportServerUrl() {
            return reportServerUrl;
        }

        public void setReportServerUrl(String reportServerUrl) {
            this.reportServerUrl = reportServerUrl;
        }

        public SystemCmdInfoFactory.AgentUpdateInfo getAgentUpdateInfo() {
            return agentUpdateInfo;
        }

        public void setAgentUpdateInfo(SystemCmdInfoFactory.AgentUpdateInfo agentUpdateInfo) {
            this.agentUpdateInfo = agentUpdateInfo;
        }
    }

    public static Map<String, String> labels() {
        Map<String, String> labels = new HashMap<String, String>();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String name = runtimeMXBean.getName();
        String pid = name.split("@")[0];
        labels.put(PROCESS_ID, pid);
        labels.put("processStartTime",
                DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(runtimeMXBean.getStartTime()));
        labels.put("agentRegisterTime",
                DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(System.currentTimeMillis()));
        labels.put("agentVersion", VERSION);
        labels.put("hostIp", LocalUtils.getHostIp());
        labels.put("hostName", LocalUtils.getHostName());
        labels.put("os", String.valueOf(OS.os()));
        return labels;
    }
}
