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

package com.baidu.agile.agent.log;

import com.baidu.agile.agent.common.thread.IntervalThread;

import org.apache.commons.lang.StringUtils;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SaAsyncMultiLogger implements IJobMultiAsyncLogger {

    private static final int FORCE_MS_COUNT = 10 * 1000;

    private boolean addTimeStamp = false;

    private IntervalThread flushThread;

    private Map<String, SaAsyncLogger> loggers = new ConcurrentHashMap<String, SaAsyncLogger>();

    public SaAsyncMultiLogger() {
        this.addTimeStamp = false;
    }

    public SaAsyncMultiLogger(boolean addTimeStamp) {
        this.addTimeStamp = addTimeStamp;
    }

    @Override
    public void log(String uuid, String line) {
        if (StringUtils.isEmpty(uuid)) {
            return;
        }
        getLogger(uuid).log(line);
    }

    @Override
    public void log(String uuid, String level, String line) {
        if (StringUtils.isEmpty(uuid)) {
            return;
        }
        getLogger(uuid).log(level, line);
    }

    @Override
    public void log(String uuid, InputStream inputStream) {
        if (StringUtils.isEmpty(uuid)) {
            return;
        }
        getLogger(uuid).log(inputStream);
    }

    private SaAsyncLogger getLogger(String uuid) {
        SaAsyncLogger logger = loggers.get(uuid);
        if (null == logger) {
            logger = new SaAsyncLogger(uuid, this.addTimeStamp);
            loggers.put(uuid, logger);
        }
        return logger;
    }

    public void start() {
        this.flushThread = new IntervalThread(FORCE_MS_COUNT) {
            @Override
            protected void execute() {
                SaAsyncMultiLogger.this.flush();
            }
        };
        this.flushThread.start();
    }

    private void flush() {
        for (Map.Entry<String, SaAsyncLogger> entry : loggers.entrySet()) {
            SaAsyncLogger logger = entry.getValue();
            if (null != logger) {
                logger.flush();
            }
        }
    }

    public void end() {
        if (null != this.flushThread) {
            this.flush();
            this.flushThread.end();
        }
    }

}
