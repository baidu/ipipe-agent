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

package com.baidu.agile.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;

public enum AgentStatus {

    ONLINE,
    // 指状态的UPDATING，而不是在更新Agent
    UPDATEING() {
        @Override
        public AgentStatus onAgentEvent(AgentEvent event) {
            return UPDATEING;
        }
    },
    OFFLINE;

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentStatus.class);

    // Agent在不同状态，有不同的活跃的线程，并不需要等待线程结束
    public static void maintain(@NotNull AgentEvent event) {
        if (null == event) {
            return;
        }
        LOGGER.info("status:{}, accept event:{}", CURRENT, event);
        synchronized (AgentStatus.class) {
            if (UPDATEING.equals(CURRENT)) {
                if (event.gt(AgentEvent.waiting())) {
                    AgentEvent.wait(event);
                }
                return;
            }
            AgentStatus from = CURRENT;
            CURRENT = UPDATEING;
            AgentStatus to = from.onAgentEvent(event);
            AgentEvent waiting = AgentEvent.popWaiting();
            while (waiting != null) {
                to = waiting.process();
                waiting = AgentEvent.popWaiting();
            }
            if (null == to || UPDATEING.equals(to)) {
                LOGGER.error("Return error status:{}", to);
                to = OFFLINE;
            }
            LOGGER.info("change to status:{}", to);
            CURRENT = to;
        }
    }

    public AgentStatus onAgentEvent(AgentEvent event) {
        return event.process();
    }

    // agent当前状态
    private static volatile AgentStatus CURRENT = OFFLINE;

    @NotNull
    public static AgentStatus current() {
        return CURRENT;
    }
}
