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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SaBufferLogger extends SaLogger implements IJobBufferLogger {

    private StringBuilder logBuffer = new StringBuilder();

    protected long latestFlushMills = System.currentTimeMillis();

    public SaBufferLogger(String uuid) {
        super(uuid);
    }

    public SaBufferLogger(String uuid, boolean addTimeStamp) {
        super(uuid, addTimeStamp);
    }

    public SaBufferLogger(String uuid, String dateFormat, String timeZone) {
        super(uuid, dateFormat, timeZone);
    }

    @Override
    protected void logContent(String log) {
        if (StringUtils.isNotEmpty(log)) {
            synchronized (this) {
                this.logBuffer.append(log);
            }
        }
    }

    @Override
    public void flush() {
        if (this.logBuffer.length() > 0) {
            synchronized (this) {
                super.logContent(logBuffer.toString());
                this.logBuffer = new StringBuilder();
                this.latestFlushMills = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void log(InputStream inputStream) {
        if (null == inputStream) {
            this.log("ERROR", "Null log stream!");
            return;
        }
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader reader = new BufferedReader(inputStreamReader);
        new Thread() {
            @Override
            public void run() {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        SaBufferLogger.this.log(line);
                    }
                    SaBufferLogger.this.flush();
                } catch (IOException e) {
                    SaBufferLogger.this.log("ERROR", ExceptionUtils.getFullStackTrace(e));
                }
            }
        }.start();
    }
}
