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

package com.baidu.agile.agent.upgrade;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.common.CommonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Watcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Watcher.class);

    private static final Logger UPGRADE_LOGGER = LoggerFactory.getLogger("UPGRADE_LOG");

    private static final String LOG_PREFIX = Context.getAgentArgs().getUuid() + "-";

    public static UpgradeStatus watch(Process process) {

        try {
            boolean alive = false;
            for (int i = 0; i < 10; i++) {
                alive = isAlive(process);
                if (!alive) {
                    break;
                }
                Thread.sleep(1000);
            }

            if (alive) {
                log("upgrade_execute_success");
                UpgradeStatus upgradeStatus = checkVersion();
                return upgradeStatus;
            } else {
                log("upgrade_execute_fail_process_not_alive");
                return null;
            }
        } catch (Throwable e) {
            log("upgrade_watch_error_" + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
        }
        return null;
    }

    /**
     * Tests whether the subprocess represented by this {@code Process} is alive.
     *
     * @return {@code true} if the subprocess represented by this {@code Process} object has not yet terminated.
     */
    public static boolean isAlive(Process process) {

        try {
            // throws IllegalThreadStateException if the subprocess represented by this  object has not yet terminated
            process.exitValue();
            try {
                List<String> errorOutputStr = IOUtils.readLines(process.getErrorStream(), "UTF-8");
                log("upgrade_execute_process_start_fail_" + errorOutputStr.toString());
            } catch (Throwable e) {
                LOGGER.error("get errorOutputStr error ", e);
            }
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    /**
     * 检查自身是否为最新的版本
     *
     * @return
     */
    public static UpgradeStatus checkVersion() throws IOException {
        CommonResponse response;

        try {
            response = new HttpClient(Context.getAgentArgs().getServer())
                    .path("/agent/v1/upgrade/checkVersion")
                    .param("uuid", Context.getAgentArgs().getUuid())
                    .retry(3)
                    .get(CommonResponse.class);

        } catch (Throwable e) {
            log("upgrade_check_version_fail_" + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
            return null;
        }
        if (null == response) {
            log("upgrade_check_version_fail_null_response");
            return null;
        }

        if (!CommonResponse.STATUS_OK.equals(response.getStatus())) {
            log("upgrade_check_version_fail_" + response.getStatus() + "_" + response.getMessage());
            return null;
        }
        UpgradeStatus upgradeStatus = new ObjectMapper().readValue(response.getMessage(), UpgradeStatus.class);
        log("upgrade_check_version_success");
        return upgradeStatus;
    }

    private static void log(String log) {
        log(log, LOGGER);
    }

    protected static void log(String log, Logger LOGGER) {
        UPGRADE_LOGGER.info(LOG_PREFIX + log);
        LOGGER.info(LOG_PREFIX + log);
        Context.log(log);
    }

}
