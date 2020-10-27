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

package com.baidu.agile.agent.heart;

import com.baidu.agile.agent.AgentEvent;
import com.baidu.agile.agent.AgentStatus;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.common.thread.IntervalThread;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;
import com.baidu.agile.server.common.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartThread extends IntervalThread {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartThread.class);

    private static final int INTERVAL = 60 * 1000;

    private static IntervalThread THREAD;

    private static final String THREAD_NAME = "heart-thread";

    private static final int TTL = 2;

    private int fail = 0;

    protected HeartThread() {
        super(INTERVAL);
    }

    @Override
    public void execute() {
        LOGGER.info("HeartBeat Thread Run, Agent Status:{}", AgentStatus.current());
        if (!AgentStatus.ONLINE.equals(AgentStatus.current())) {
            return;
        }

        CommonResponse response;

        try {
            response = new HttpClient(Context.getAgentArgs().getServer())
                    .path("/agent/v1/heartbeat")
                    .jsonBody(Context.heartRequest())
                    .timeout(INTERVAL)
                    .uploadLog(true)
                    .post(CommonResponse.class);
        } catch (Exception e) {
            LOGGER.error("Heart exception!", e);
            Context.log("[AGENT_HEART_ERROR]", e);
            fail();
            return;
        }
        if (null == response) {
            LOGGER.error("Heart return null!");
            Context.log("[AGENT_HEART_ERROR]:null response!");
            fail();
            return;
        }

        Context.log("[AGENT_HEART_SUCCESS]");

        if (!CommonResponse.STATUS_OK.equals(response.getStatus())) {
            LOGGER.error("Heart error return status:{}, message:{}!", response.getStatus(), response.getMessage());
            fail();
            return;
        }
        fail = 0;
        if (null != response.getSystemCmd()) {
            switch (response.getSystemCmd()) {
                case AGENT_UPDATE:
                    Context.setAgentUpdateInfo(
                            (SystemCmdInfoFactory.AgentUpdateInfo) response.getSystemCmdInfo());
                    AgentStatus.maintain(AgentEvent.AGENT_UPGRADE);
                    return;
                case CONFIG_UPDATE:
                    AgentStatus.maintain(AgentEvent.CONFIG_UPGRADE);
                    return;
                case RE_REGISTOR:
                    AgentStatus.maintain(AgentEvent.RE_REGISTOR);
                    return;
                case REJECT:
                    AgentStatus.maintain(AgentEvent.REJECT);
                    return;
                default:
                    return;
            }
        }

    }

    private void fail() {
        fail++;
        if (fail >= TTL) {
            AgentStatus.maintain(AgentEvent.OFFLINE);
            fail = 0;
        }
    }

    public static void doStart() {
        THREAD = new HeartThread();
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
