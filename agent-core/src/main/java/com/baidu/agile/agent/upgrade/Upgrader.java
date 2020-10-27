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

import java.util.Date;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;

public class Upgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Upgrader.class);

    /**
     * 升级入口
     *
     * @return
     */
    public static void execute() {

        try {
            System.out.println("=========== upgrade_process_start [" + new Date() + "]==============");
            log("upgrade_process_start");
            UpgradeStatus upgradeStatus = tryUpgrade();

            if (upgradeStatus == null || !upgradeStatus.isOnline()) {
                System.out.println("=========== upgrade_process_fail [" + new Date() + "]==============");
                log("upgrade_process_fail");
                Recover.restart();
                return;
            }

            if (upgradeStatus.isNewestVersion()) {
                System.out.println("=========== upgrade_process_success [" + new Date() + "]==============");
                log("upgrade_process_success");
                Runtime.getRuntime().removeShutdownHook(Launcher.KILLED_HOOK_THREAD);
                Context.flush();
                System.exit(0);
            } else {
                System.out.println("=========== upgrade_process_fail [" + new Date() + "]==============");
                log("upgrade_process_fail_agent_online");
            }

        } catch (Throwable e) {
            log("upgrade_process_fail_error_" + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
        } finally {
            Context.flush();
        }
    }

    private static UpgradeStatus tryUpgrade() {

        try {
            log("upgrade_try_upgrade_start");
            Launcher.INSTANCE.unregisterAndStop();

            SystemCmdInfoFactory.AgentUpdateInfo updateInfo = Context.getServerContext().getAgentUpdateInfo();
            if (!UpgraderExecutor.prepare(updateInfo)) {
                log("upgrade_try_upgrade_fail_prepare_fail");
                return null;
            }

            String[] cmds = UpgraderExecutor.getCmds();
            UpgradeStatus upgradeStatus = UpgraderExecutor.executeUpgrade(cmds);
            log("upgrade_try_upgrade_success");
            return upgradeStatus;
        } catch (Throwable e) {
            log("upgrade_try_upgrade_fail_has_error_" + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
            return null;
        }

    }

    private static void log(String log) {
        Watcher.log(log, LOGGER);
    }

}
