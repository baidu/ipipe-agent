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

package com.baidu.agile.agent.run;

import com.baidu.agile.agent.AgentEvent;
import com.baidu.agile.agent.AgentStatus;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.job.JobManager;
import com.baidu.agile.agent.job.PluginJob;
import com.baidu.agile.agent.job.ShellJob;
import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.agent.bean.job.AgentJobConfirm;
import com.baidu.agile.server.common.CommonResponse;
import com.baidu.agile.server.job.bean.JobBaseParameter;
import com.baidu.agile.server.job.bean.concrete.KillAgentParameter;
import com.baidu.agile.server.job.bean.concrete.KillJobParameter;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class JobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);

    private JobGetter jobGetter;

    public JobScheduler(JobGetter jobGetter) {
        this.jobGetter = jobGetter;
    }

    public void schedule() {
        List<AgentJob<?>> agentJobs = ListUtils.EMPTY_LIST;
        try {
            agentJobs = jobGetter.getJobs();
            Context.log("[AGENT_GET_JOB_SUCCESS]");
        } catch (Exception e) {
            LOGGER.error("get job exception!", e);
            Context.log("[AGENT_GET_JOB_ERROR]:get job exception!", e);
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        if (CollectionUtils.isEmpty(agentJobs)) {
            return;
        }
        if (!confirm(agentJobs)) {
            return;
        }
        for (AgentJob<? extends JobBaseParameter> agentJob : agentJobs) {
            JobBaseParameter parameter = agentJob.getJobParameter();
            if (parameter instanceof ShellJobParameter) {
                try {
                    final ShellJob job = new ShellJob();
                    job.setAgentJob(agentJob);
                    JobManager.registerJob(job);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            job.start();
                        }
                    }).start();
                } catch (Exception e) {
                    LOGGER.error("process job exception!", e);
                }
            } else if (parameter instanceof KillJobParameter) {
                JobManager.killJob(((KillJobParameter) parameter).getTargetJobUuid());
            } else if (parameter instanceof KillAgentParameter) {
                System.out.println("=========== receive killAgentJob, JVM exit immediately... ["
                        + new Timestamp(System.currentTimeMillis()) + "]==============");
                LOGGER.info("receive killAgentJob, JVM exit immediately...");
                AgentStatus.maintain(AgentEvent.REJECT);
            } else {
                try {
                    final PluginJob job = new PluginJob();
                    job.setAgentJob(agentJob);
                    JobManager.registerJob(job);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            job.start();
                        }
                    }).start();
                } catch (Exception e) {
                    LOGGER.error("process job exception!", e);
                }
            }
        }
    }

    private boolean confirm(List<AgentJob<?>> agentJobs) {
        List<String> uuids = new ArrayList<String>();

        for (AgentJob<? extends JobBaseParameter> agentJob : agentJobs) {
            uuids.add(agentJob.getJobUuid());
        }

        AgentJobConfirm confirm = new AgentJobConfirm();
        confirm.setAgentUuid(Context.getServerContext().getSecureKey());
        confirm.setJobUuids(uuids);

        CommonResponse confirmResponse =
                new HttpClient(Context.getAgentArgs().getServer())
                        .path("/agent/job/v1/confirm")
                        .jsonBody(confirm)
                        .timeout(30000)
                        .retry(3)
                        .post(CommonResponse.class);

        if (null == confirmResponse) {
            LOGGER.error("confirm job return null!");
            return false;
        }

        if (!CommonResponse.STATUS_OK.equals(confirmResponse.getStatus())) {
            LOGGER.error("confirm return: code:{}, error:{}",
                    confirmResponse.getStatus(), confirmResponse.getMessage());
            return false;
        }
        return true;
    }
}
