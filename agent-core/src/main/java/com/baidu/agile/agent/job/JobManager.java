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
import com.baidu.agile.server.agent.bean.job.AgentJobResult;
import com.baidu.agile.server.common.ObjectResponse;
import com.baidu.agile.server.job.bean.JobBaseOutput;
import com.baidu.agile.server.job.bean.JobStatus;
import com.baidu.agile.server.job.bean.concrete.KillJobOutput;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JobManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobManager.class);

    private static final Map<String, Job> JOBS = new ConcurrentHashMap<String, Job>();

    public static void registerJob(Job job) {
        if (JOBS.containsKey(job.getAgentJob().getJobUuid())) {
            return;
        }
        JOBS.put(job.getAgentJob().getJobUuid(), job);
    }

    public static void removeJob(String uuid) {
        if (JOBS.containsKey(uuid)) {
            JOBS.remove(uuid);
        }
    }

    public static void endJob(String uuid, JobStatus status) {
        if (JOBS.containsKey(uuid)) {
            JOBS.get(uuid).end(status);
        }
    }


    public static void killJob(String uuid) {
        LOGGER.info("kill job=[{}]", uuid);
        if (JOBS.containsKey(uuid)) {
            JOBS.get(uuid).stop("Kill job!");
        } else {
            notice4NotExistJob(uuid);
        }
    }

    public static void notice4NotExistJob(String uuid) {

        LOGGER.info("notice not exist job:{}", uuid);
        try {
            JobBaseOutput output = new KillJobOutput();
            output.setStatus(JobStatus.KILLED);
            output.setJobUuid(uuid);

            AgentJobResult<JobBaseOutput> request = new AgentJobResult<JobBaseOutput>();
            request.setAgentUuid(Context.getServerContext().getSecureKey());
            request.setOutput(output);
            ObjectResponse response = new HttpClient(Context.getAgentArgs().getServer())
                    .path("/agent/job/v1/update")
                    .jsonBody(request)
                    .timeout(30000)
                    .retry(3)
                    .post(new ObjectResponse().getClass());

            if (null == response) {
                LOGGER.error("notice not exist job:{} fail", uuid);
                return;
            }

            if (!ObjectResponse.STATUS_OK.equals(response.getStatus())) {
                LOGGER.error("notice not exist job:{} fail, return: code:{}, error:{}",
                        uuid, response.getStatus(), response.getMessage());
                return;
            }
        } catch (Exception e) {
            LOGGER.error("notice not exist job:{} fail ", uuid, e);
        }
    }

    public static void doStop(String cause) {
        for (Map.Entry<String, Job> entry : JOBS.entrySet()) {
            entry.getValue().stop(cause);
        }
    }

    public static void doStop() {
        doStop(StringUtils.EMPTY);
    }

}
