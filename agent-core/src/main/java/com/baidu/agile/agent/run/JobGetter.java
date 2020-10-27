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

import com.baidu.agile.agent.AgentHttpException;
import com.baidu.agile.agent.AgentRedisException;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.RedisBaseConfigs;
import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.common.ListResponse;
import com.baidu.agile.server.job.bean.JobBaseParameter;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public interface JobGetter {

    Logger LOGGER = LoggerFactory.getLogger(JobGetter.class);

    public List<AgentJob<?>> getJobs();

    /**
     * 从redis pop待构建任务信息，失败后降级通过server rest api获取；
     */
    JobGetter REDIS_REST = new JobGetter() {
        @Override
        public List<AgentJob<?>> getJobs() {
            try {
                return REDIS.getJobs();
            } catch (Exception e) {
                LOGGER.error("get job from redis exception, try rest from server!", e);
                try {
                    return REST.getJobs();
                } catch (Exception ex) {
                    LOGGER.error("get job from server rest exception!", ex);
                }
            }
            return ListUtils.EMPTY_LIST;
        }
    };

    JobGetter REDIS = new JobGetter() {
        @Override
        public List<AgentJob<?>> getJobs() {
            String uuid = Context.getServerContext().getSecureKey();
            RedisBaseConfigs config = Context.getServerContext().getJobQueueRedisConfigs();
            Jedis jedis = null;
            try {
                jedis = new Jedis(config.getHost(), config.getPort());
                if (StringUtils.isNotBlank(config.getPassword())) {
                    jedis.auth(config.getPassword());
                }
                jedis.select(config.getDatabase());
                List<AgentJob<?>> agentJobs = new ArrayList<AgentJob<?>>();
                String json = jedis.lpop(uuid);
                while (StringUtils.isNotBlank(json)) {
                    try {
                        agentJobs.add(HttpClient.MAPPER.readValue(json, AgentJob.class));
                    } catch (Exception e) {
                        LOGGER.error("parse job json exception", e);
                    } finally {
                        json = jedis.lpop(uuid);
                    }
                }
                return agentJobs;
            } catch (Exception e) {
                throw new AgentRedisException(e);
            } finally {
                if (null != jedis) {
                    try {
                        jedis.quit();
                        jedis.disconnect();
                    } catch (Exception e) {
                        LOGGER.info("disconnect redis exception!");
                    }
                }
            }
        }
    };

    JobGetter REST = new JobGetter() {
        @Override
        public List<AgentJob<?>> getJobs() {
            ListResponse<AgentJob<? extends JobBaseParameter>> response =
                    new HttpClient(Context.getAgentArgs().getServer())
                            .path("/agent/job/v1/get")
                            .param("uuid", Context.getServerContext().getSecureKey())
                            .timeout(30000)
                            .post(new ListResponse<AgentJob<? extends JobBaseParameter>>().getClass());

            if (null == response) {
                LOGGER.error("Pull job return null!");
                throw new AgentHttpException("Pull job return null!");
            }

            if (!ListResponse.STATUS_OK.equals(response.getStatus())) {
                LOGGER.error("register return: code:{}, error:{}", response.getStatus(), response.getMessage());
                throw new AgentHttpException("Pull job return error:" + response.getMessage());
            }

            return response.getData();
        }
    };

}
