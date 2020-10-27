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

package com.baidu.agile.agent.common.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 间歇执行，在执行时间过长超出间隔时，则该次间隔以实际执行时间为准，保证串行
 */
public abstract class IntervalThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalThread.class);

    private static final int MIN_INTERVAL = 1000;

    protected int interval;

    protected boolean end = false;

    protected IntervalThread(int interval) {
        this.interval = Math.max(interval, MIN_INTERVAL);
    }

    @Override
    public final void run() {
        while (!end) {
            long start = System.currentTimeMillis();
            try {
                execute();
            } catch (Throwable e) {
                LOGGER.error("Interval execute exception!", e);
            } finally {
                long interval = System.currentTimeMillis() - start;
                if (interval < this.interval) {
                    try {
                        sleep(this.interval - interval);
                    } catch (Exception e) {
                        LOGGER.error("Interval sleep exception!", e);
                    }
                }
            }
        }
    }

    protected abstract void execute();

    public void end() {
        end = true;
    }

}
