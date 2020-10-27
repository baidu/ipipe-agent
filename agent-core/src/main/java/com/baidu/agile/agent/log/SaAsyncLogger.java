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

public class SaAsyncLogger extends SaBufferLogger implements IJobAsyncLogger {

    /**
     * 短时间内缓存而不立刻发送增量日志
     */
    private static final int FORBID_MS_COUNT = 1000;
    /**
     * 长时间后及时发送已缓存日志
     */
    private static final int FORCE_MS_COUNT = 10 * 1000;
    /**
     * 短时间内行数较少时暂时积累日志，达到一定行数后批量发送
     */
    private static final int FORCE_LINE_COUNT = 20;

    private int lineCount = 0;

    private IntervalThread flushThread;

    public SaAsyncLogger(String uuid) {
        super(uuid);
    }

    public SaAsyncLogger(String uuid, boolean addTimeStamp) {
        super(uuid, addTimeStamp);
    }

    public SaAsyncLogger(String uuid, String dateFormat, String timeZone) {
        super(uuid, dateFormat, timeZone);
    }

    @Override
    public void log(String line) {
        super.log(line);
        this.lineCount++;
        long msCount = System.currentTimeMillis() - this.latestFlushMills;
        if (msCount >= FORCE_MS_COUNT || msCount >= FORBID_MS_COUNT && lineCount >= FORCE_LINE_COUNT) {
            super.flush();
        }
    }

    public void start() {
        this.flushThread = new IntervalThread(FORCE_MS_COUNT) {
            @Override
            protected void execute() {
                SaAsyncLogger.this.flush();
            }
        };
        this.flushThread.start();
    }

    public void end() {
        if (null != this.flushThread) {
            this.flushThread.end();
        }
        // 结束的时候最后一次flush，防止丢日志
        this.flush();
    }

}
