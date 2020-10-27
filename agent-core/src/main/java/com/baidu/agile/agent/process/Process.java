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

package com.baidu.agile.agent.process;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public abstract class Process {

    private static final Logger LOGGER = LoggerFactory.getLogger(Process.class);

    protected final int pid;

    protected Process(int pid) {
        this.pid = pid;
    }

    public abstract Process getParent();

    public abstract List<Process> getChildren();

    public abstract Map<String, String> getEnvVars();

    public abstract void kill() throws InterruptedException;

    public void killRecursively() throws InterruptedException {
        LOGGER.info("Recursively killing pid=" + pid);
        for (Process p : getChildren()) {
            p.killRecursively();
        }
        kill();
    }

    public int getPid() {
        return pid;
    }

    public String getName() {
        return StringUtils.EMPTY;
    }
}
