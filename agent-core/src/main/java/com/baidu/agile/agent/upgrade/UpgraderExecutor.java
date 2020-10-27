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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.common.ftp.FtpDownloader;
import com.baidu.agile.agent.common.http.HttpDownloader;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.os.OS;
import com.baidu.agile.agent.os.PosixAPI;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;

public class UpgraderExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgraderExecutor.class);

    private static final String JAVA_PATH_SUFFIX = File.separator + "bin" + File.separator + "java";

    protected UpgraderExecutor() {
    }

    protected static boolean prepare(SystemCmdInfoFactory.AgentUpdateInfo updateInfo) {

        log("upgrade_prepare_start");
        boolean hasJar = upgradeJarOnce(updateInfo);
        boolean javaExecutable = setExecutable4Java();
        if (!hasJar) {
            log("upgrade_prepare_fail_no_jar");
            return false;
        }

        if (!javaExecutable) {
            log("upgrade_prepare_fail_java_cannot_executable");
            return false;
        }
        log("upgrade_prepare_finish");
        return true;
    }

    protected static boolean upgradeJarOnce(SystemCmdInfoFactory.AgentUpdateInfo updateInfo) {

        String timeStamp = DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMdd.HHmmss");
        String upgradeJar = UUID.randomUUID() + "." + timeStamp + ".upgrade.agent.jar";
        try {
            if (updateInfo.getDownloadUrl().startsWith("ftp://")) {
                new FtpDownloader()
                        .from(updateInfo.getDownloadUrl())
                        .to(System.getProperty("user.dir") + File.separator + upgradeJar)
                        .username(updateInfo.getUser())
                        .password(updateInfo.getPassword())
                        .download();
            } else {
                new HttpDownloader()
                        .from(updateInfo.getDownloadUrl())
                        .to(System.getProperty("user.dir") + File.separator + upgradeJar)
                        .download();
            }

            // 启动进程时的目录可能被强删了,修复这种情况; Window情况下有进程占用无法强删且部分情况无chdir实现,故排除
            // 若user.dir被删掉，原有的目录句柄已不存在，这个时候需要chdir，使用新的句柄
            if (!OS.WINDOWS.equals(OS.os())) {
                try {
                    PosixAPI.jnr().chdir(System.getProperty("user.dir"));
                } catch (Throwable e) {
                    LOGGER.warn("chdir exception!", e);
                    Context.log("chdir exception: " + e.getMessage(), e);
                }
            }

            // 注意：user.dir被删除后，若没有chdir，则new File(upgradeJar).exists()会返回false
            if (new File(upgradeJar).exists()) {
                // 重命名为agent.jar
                if (new File(upgradeJar).renameTo(new File("agent.jar"))) {
                    try {
                        // 删除临时文件
                        File file = new File(upgradeJar);
                        if (file.exists()) {
                            FileUtils.forceDelete(new File(upgradeJar));
                        }
                    } catch (Throwable e) {
                        LOGGER.warn("Delete temp upgradeJar:{} exception:", upgradeJar, e);
                    }
                    return true;
                } else {
                    log("upgrade_prepare_error_jar_rename_fail");
                    return false;
                }
            } else {
                log("upgrade_prepare_error_jar_not_exists");
                return false;
            }
        } catch (Throwable e) {
            log("upgrade_prepare_error_jar_download_fail_" + e.getMessage() + "\n"
                    + ExceptionUtils.getFullStackTrace(e));
            return false;
        }
    }

    public static boolean setExecutable4Java() {

        String javaPath = StringUtils.EMPTY;
        try {
            javaPath = System.getProperty("java.home") + JAVA_PATH_SUFFIX;
            File file = new File(javaPath);
            if (!file.canExecute()) {
                file.setExecutable(true);
                log("set executable for java [javaPath={\" + javaPath + \"}]");
            }
            return true;
        } catch (Throwable e) {
            log("upgrade_check_error_java_cannot_executable_" + e.getMessage() +
                    "[javaPath={" + javaPath + "}]" + "\n" + ExceptionUtils.getFullStackTrace(e));
            return false;
        }
    }

    public static String[] getCmds() {

        List<String> cmds = new ArrayList<String>();

        cmds.add(System.getProperty("java.home") + JAVA_PATH_SUFFIX);

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmOptions = runtimeMXBean.getInputArguments();
        cmds.addAll(jvmOptions);

        cmds.add("-jar");
        cmds.add("agent.jar");
        CollectionUtils.addAll(cmds, Context.getArgs());

        String[] cmdArray = cmds.toArray(new String[cmds.size()]);
        return cmdArray;
    }

    protected static UpgradeStatus executeUpgrade(String[] cmd) {

        try {
            // 守护进程启动，则不自动更新；直接退出，由守护进程拉起
            if (Launcher.isSuperviseRun()) {
                log("upgrade_execute_success_agent_run_by_supervise");
                return new UpgradeStatus(true, true);
            }
            Process process = Runtime.getRuntime().exec(cmd);
            return Watcher.watch(process);
        } catch (Throwable e) {
            log("upgrade_execute_fail_error_" + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
            return null;
        }
    }

    private static void log(String log) {
        Watcher.log(log, LOGGER);
    }

}
