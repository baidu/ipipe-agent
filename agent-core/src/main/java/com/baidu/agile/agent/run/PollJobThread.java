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
import com.baidu.agile.agent.AgentStatus;
import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.common.thread.IntervalThread;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.RedisBaseConfigs;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import java.util.Random;

public class PollJobThread extends IntervalThread {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollJobThread.class);

    private static final int INTERVAL = 1 * 1000;

    private static IntervalThread THREAD;

    private static final String THREAD_NAME = "poll-job-thread";

    private PollJobThread() {
        super(INTERVAL);
    }

    private static final int MAX_FAIL = 7 * 24 * 3600;

    private static final int FORCE = 60;

    private int fail = 0;

    private static final JobScheduler JOB_SCHEDULER = new JobScheduler(JobGetter.REST);

    @Override
    public void execute() {
        if (!AgentStatus.ONLINE.equals(AgentStatus.current())) {
            return;
        }

        boolean needPullJob = needPullJob();
        if (!needPullJob) {
            return;
        }

        JOB_SCHEDULER.schedule();
    }

    private static final Random RANDOM = new Random();

    /**
     * true if there is jobs dispatched to this agent or persist redis fail to MAX_FAIL
     *
     * @return
     */
    private boolean needPullJob() {
        boolean pullJob = false;
        String uuid = Context.getServerContext().getSecureKey();
        RedisBaseConfigs config = Context.getServerContext().getRedisConfigs();
        Jedis jedis = null;
        try {
            jedis = new Jedis(config.getHost(), config.getPort());
            if (StringUtils.isNotBlank(config.getPassword())) {
                jedis.auth(config.getPassword());
            }
            jedis.select(config.getDatabase());
            long number = NumberUtils.toLong(jedis.get(uuid));
            if (number > 0) {
                pullJob = true;
            }
        } catch (Throwable e) {
            LOGGER.error("listen redis exception!", e);
            try {
                String response = new HttpClient(Context.getAgentArgs().getServer())
                        .path("/agent/job/v1/notice")
                        .param("secureKey", Context.getServerContext().getSecureKey())
                        .timeout(500)
                        .get(String.class);
                if (null == response) {
                    LOGGER.error("agent notice from server return null!");
                    throw new AgentHttpException("agent notice from server return null!");
                }
                long number = NumberUtils.toLong(response);
                if (number > 0) {
                    pullJob = true;
                }
            } catch (Exception ie) {
                LOGGER.error("listen server exception!", ie);
                fail++;
                if (fail > MAX_FAIL) {
                    Launcher.INSTANCE.stop(1, "agent cannot listen job, exit!");

                } else if (fail % FORCE == 0) {
                    LOGGER.info("force pull job from server!");
                    try {
                        sleep(RANDOM.nextInt(INTERVAL));
                    } catch (InterruptedException ide) {
                        Thread.currentThread().interrupt();
                    }
                    pullJob = !this.end;
                }
            }
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
        return pullJob;
    }

    public static void doStart() {
        THREAD = new PollJobThread();
        THREAD.setName(THREAD_NAME);
        THREAD.start();
    }

    public static void doStop() {
        if (null == THREAD) {
            return;
        }
        THREAD.end();
        THREAD = null;
    }
}
