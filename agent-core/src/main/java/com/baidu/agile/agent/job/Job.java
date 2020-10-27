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

import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.AgentResponse;
import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.agent.bean.job.AgentJobResult;
import com.baidu.agile.server.common.ObjectResponse;
import com.baidu.agile.server.job.bean.JobBaseOutput;
import com.baidu.agile.server.job.bean.JobBaseParameter;
import com.baidu.agile.server.job.bean.JobStatus;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public abstract class Job<J extends JobBaseParameter, R extends JobBaseOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Job.class);

    private AgentJob<J> agentJob;

    protected volatile JobStatus status;

    public abstract void start();

    public abstract void stop(String cause);

    public void stop() {
        stop(StringUtils.EMPTY);
    }

    private static final List<JobStatus> END_STATUS = Arrays.asList(JobStatus.SUCC, JobStatus.FAILED, JobStatus.KILLED);

    public void end(JobStatus status) {
        // 这里判断的是 this.status，而不是入参status
        if (END_STATUS.contains(this.status)) {
            return;
        }
        synchronized (this) {
            if (END_STATUS.contains(this.status)) {
                return;
            }
            this.status = status;
        }
        try {
            notice();
        } catch (Exception e) {
            LOGGER.error("notice error", e);
        }
        JobManager.removeJob(getAgentJob().getJobUuid());
    }

    protected final void notice() {
        AgentJobResult<R> request = new AgentJobResult<R>();
        request.setAgentUuid(Context.getServerContext().getSecureKey());
        request.setOutput(result(status));
        ObjectResponse<AgentResponse> response = new HttpClient(Context.getAgentArgs().getServer())
                .path("/agent/job/v1/update")
                .jsonBody(request)
                .timeout(30000)
                .retry(10)
                .post(new ObjectResponse<AgentResponse>().getClass());

        if (null == response) {
            LOGGER.error("notice job:{} status return null!", agentJob.getJobUuid());
            Context.log("[AGENT_NOTICE_JOB_ERROR]:reponse null");
            return;
        }
        Context.log("[AGENT_NOTICE_JOB_SUCCESS]");

        if (!ObjectResponse.STATUS_OK.equals(response.getStatus())) {
            LOGGER.error("notice job:{} ,return: code:{}, error:{}",
                    agentJob.getJobUuid(), response.getStatus(), response.getMessage());
            return;
        }
    }

    public AgentJob<J> getAgentJob() {
        return agentJob;
    }

    public void setAgentJob(AgentJob<?> agentJob) {
        this.agentJob = new AgentJob<J>();
        this.agentJob.setJobUuid(agentJob.getJobUuid());
        this.agentJob.setJobParameter((J) agentJob.getJobParameter());
    }

    protected abstract R result(JobStatus status);

}
