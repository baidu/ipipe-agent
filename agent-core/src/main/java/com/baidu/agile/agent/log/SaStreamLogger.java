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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class SaStreamLogger extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaStreamLogger.class);

    private static final int FORBID_MS_COUNT = 1000;
    private static final int FORCE_MS_COUNT = 10 * 1000;
    private static final int FORCE_LINE_COUNT = 20;

    private IJobBufferLogger jobBufferLogger;
    private InputStream inputStream;

    private long msStart;
    private int lineCount;

    public SaStreamLogger(IJobBufferLogger logger, InputStream inputStream) {
        this.jobBufferLogger = logger;
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        IntervalThread flushLogThread = new FlushLogThread();
        flushLogThread.start();
        InputStreamReader inputStreamReader = new InputStreamReader(this.inputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        this.msStart = System.currentTimeMillis();
        this.lineCount = 0;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                this.jobBufferLogger.log(line);
                this.lineCount++;
                long msCount = System.currentTimeMillis() - this.msStart;
                if (msCount >= FORCE_MS_COUNT || msCount >= FORBID_MS_COUNT && lineCount >= FORCE_LINE_COUNT) {
                    flush();
                }
            }
            this.jobBufferLogger.flush();
        } catch (IOException e) {
            jobBufferLogger.log("ERROR", "Save stream log error:" + e.getMessage());
            LOGGER.error("Save log fail", e);
        } finally {
            flushLogThread.end();
            if (null != this.inputStream) {
                try {
                    this.inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("Close input stream exception:{}", e.getMessage());
                }
            }
        }
    }

    private synchronized void flush() {
        this.jobBufferLogger.flush();
        this.lineCount = 0;
        this.msStart = System.currentTimeMillis();
    }

    private class FlushLogThread extends IntervalThread {

        protected FlushLogThread() {
            super(FORCE_MS_COUNT);
        }

        @Override
        public void execute() {
            SaStreamLogger.this.flush();
        }
    }
}
