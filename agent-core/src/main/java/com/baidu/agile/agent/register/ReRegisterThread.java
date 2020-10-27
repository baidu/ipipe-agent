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

package com.baidu.agile.agent.register;

import com.baidu.agile.agent.AgentEvent;
import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.AgentStatus;
import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.common.thread.IntervalThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReRegisterThread extends IntervalThread {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReRegisterThread.class);

    private static final int INTERVAL = 60 * 1000;

    private static IntervalThread THREAD;

    private static final String THREAD_NAME = "re-register-thread";

    private ReRegisterThread() {
        super(INTERVAL);
    }

    @Override
    public void execute() {
        try {
            Register.register();
            Launcher.INSTANCE.reload();
        } catch (AgentException e) {
            LOGGER.error("re-register exception", e);
            return;
        }
        AgentStatus.maintain(AgentEvent.ONLINE);
        doStop();
    }

    public static void doStart() {
        doStop();
        THREAD = new ReRegisterThread();
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
