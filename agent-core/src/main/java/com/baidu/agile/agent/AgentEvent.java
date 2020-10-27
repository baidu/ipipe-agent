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

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.job.JobManager;
import com.baidu.agile.agent.register.ReRegisterThread;
import com.baidu.agile.agent.upgrade.Upgrader;

import javax.validation.constraints.NotNull;

public enum AgentEvent {
    ONLINE(0) {
        @Override
        public AgentStatus process() {
            return AgentStatus.ONLINE;
        }
    },
    OFFLINE(0) {
        @Override
        public AgentStatus process() {
            JobManager.doStop("Agent offline!");
            // 看调用处，当心跳多次失败后，agent主动将自身设置为OFFLINE，不断尝试重新注册
            ReRegisterThread.doStart();
            return AgentStatus.OFFLINE;
        }
    },
    CONFIG_UPGRADE(10) {
        @Override
        public AgentStatus process() {
            ReRegisterThread.doStart();
            return AgentStatus.OFFLINE;
        }
    },
    // 解决由于网络问题，导致心跳丢失，但是agent还实际在线的情况
    RE_REGISTOR(10) {
        @Override
        public AgentStatus process() {
            Context.getServerContext().setSecureKey(null);
            ReRegisterThread.doStart();
            return AgentStatus.OFFLINE;
        }
    },
    AGENT_UPGRADE(20) {
        @Override
        public AgentStatus process() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Upgrader.execute();
                }
            }).start();
            return AgentStatus.OFFLINE;
        }
    },
    REJECT(20) {
        @Override
        public AgentStatus process() {
            Launcher.INSTANCE.stop(1, "Server reject!");
            return AgentStatus.OFFLINE;
        }
    };


    // 待执行的Event
    private static volatile AgentEvent WAITING;

    // 若待执行的Event优先级小于"入参"，则将"入参"放入待执行
    public static void wait(AgentEvent event) {
        if (null != event && event.gt(WAITING)) {
            WAITING = event;
        }
    }

    public static AgentEvent waiting() {
        return WAITING;
    }

    public static AgentEvent popWaiting() {
        AgentEvent waiting = WAITING;
        WAITING = null;
        return waiting;
    }

    private int priority;

    AgentEvent(int priority) {
        this.priority = priority;
    }

    public boolean gt(AgentEvent event) {
        return null == event || priority > event.priority;
    }

    @NotNull
    public abstract AgentStatus process();

}
