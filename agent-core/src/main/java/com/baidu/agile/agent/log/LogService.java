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

import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogService.class);

    public static final int TIMEOUT = 10 * 1000;

    /**
     * 保存或追加日志流到log Server
     *
     * @param uuid        文件名(必须唯一，如果不唯一，则追加到uuid的文件上)
     * @param inputStream 日志流
     */
    public static void saveOrAppendLog(String uuid, InputStream inputStream) {
        saveOrAppendLog(uuid, inputStream, null);
    }

    /**
     * 保存或追加日志到log Server
     *
     * @param uuid 文件名(必须唯一，如果不唯一，则追加到uuid的文件上)
     * @param log  日志
     */
    public static void saveOrAppendLog(String uuid, String log) {
        saveOrAppendLog(uuid, log, null);
    }

    private static void saveOrAppendLog(String uuid, InputStream inputStream, String logFileName) {
        final int forbidMsCount = 1000;
        final int forceMsCount = 10 * 1000;
        final int forceLineCount = 20;
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        try {
            int lineCount = 0;
            long msStart = System.currentTimeMillis();
            StringBuilder build = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                build.append(line).append("\n");
                lineCount++;
                long msCount = System.currentTimeMillis() - msStart;
                if (msCount >= forceMsCount || msCount >= forbidMsCount && lineCount >= forceLineCount) {
                    saveOrAppendLog(uuid, build.toString(), logFileName);
                    build = new StringBuilder();
                    lineCount = 0;
                    msStart = System.currentTimeMillis();
                }
            }
            if (build.length() > 0) {
                saveOrAppendLog(uuid, build.toString(), logFileName);
            }
        } catch (IOException e) {
            LOGGER.error("Save log fail", e);
        }
    }

    private static void saveOrAppendLog(String uuid, String log, String logFileName) {
        if (StringUtils.isEmpty(logFileName)) {
            saveOrAppendLog2Server(uuid, log);
        } else {
            saveOrAppendLog2File(uuid, log, logFileName);
        }
    }

    private static void saveOrAppendLog2Server(String uuid, String log) {
        try {
            new HttpClient(Context.getServerContext().getLogServerUrl())
                    .path("/log/saveLogNew")
                    .param("uuid", uuid)
                    .body("text/plain", log)
                    .timeout(TIMEOUT)
                    .post(String.class);

        } catch (Throwable e) {
            LOGGER.error("Call log server fail", e);
        }
    }

    /**
     * 保存或追加日志流到本地文件
     * 日志地址为Context.ws(uuid)/${logFileName}
     *
     * @param uuid        jobUuid
     * @param inputStream 日志流
     * @param logFileName 日志文件名
     */
    public static void saveOrAppendLog2File(String uuid, InputStream inputStream, String logFileName) {
        saveOrAppendLog(uuid, inputStream, logFileName);
    }

    /**
     * 保存或追加日志到本地文件
     * 日志地址为Context.ws(uuid)/${logFileName}
     *
     * @param uuid        jobUuid
     * @param log         日志流
     * @param logFileName 日志文件名
     */
    public static void saveOrAppendLog2File(String uuid, String log, String logFileName) {
        File logFile = new File(Context.ws(uuid) + File.separator + logFileName);
        try {
            FileUtils.write(logFile, log, "utf-8", true);
        } catch (IOException e) {
            LOGGER.error("Save log to file fail", e);
        }
    }

}
