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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SaLogger implements IJobLogger {

    private String uuid;

    private boolean addTimeStamp = false;

    private String dateFormat = "yyyy-MM-dd HH:mm:ss";

    private TimeZone timeZone;

    public SaLogger(String uuid) {
        this.uuid = uuid;
        this.addTimeStamp = false;
    }

    public SaLogger(String uuid, boolean addTimeStamp) {
        this.uuid = uuid;
        this.addTimeStamp = addTimeStamp;
    }

    public SaLogger(String uuid, String dateFormat, String timeZone) {
        this.uuid = uuid;
        this.addTimeStamp = true;
        this.dateFormat = dateFormat;
        this.timeZone = TimeZone.getTimeZone(timeZone);
    }

    @Override
    public String uuid() {
        return uuid;
    }

    @Override
    public void log(String line) {
        if (StringUtils.isEmpty(line)) {
            return;
        }
        if (addTimeStamp) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
            if (timeZone != null) {
                simpleDateFormat.setTimeZone(timeZone);
            }
            logContent(new StringBuilder(simpleDateFormat.format(new Date()))
                    .append(" ").append(line).append("\n").toString());
        } else {
            logContent(new StringBuilder(line).append("\n").toString());
        }
    }

    /**
     * log content: [level] line
     * @param level
     * @param line
     */
    @Override
    public void log(String level, String line) {
        log(new StringBuilder("[").append(level).append("] ").append(line).toString());
    }

    protected void logContent(String log) {
        LogService.saveOrAppendLog(uuid, log);
    }
}
