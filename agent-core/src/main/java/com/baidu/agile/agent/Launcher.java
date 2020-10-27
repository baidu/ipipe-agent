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

package com.baidu.agile.agent;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.baidu.agile.agent.java.JavaUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baidu.agile.agent.common.ftp.FtpDownloader;
import com.baidu.agile.agent.common.http.HttpDownloader;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.heart.HeartThread;
import com.baidu.agile.agent.job.JobManager;
import com.baidu.agile.agent.os.OS;
import com.baidu.agile.agent.os.PosixAPI;
import com.baidu.agile.agent.plugin.PluginManager;
import com.baidu.agile.agent.process.Process;
import com.baidu.agile.agent.process.ProcessTree;
import com.baidu.agile.agent.register.ReRegisterThread;
import com.baidu.agile.agent.register.Register;
import com.baidu.agile.agent.run.PollJobThread;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;

/**
 * Agent launcher
 */
public abstract class Launcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
    public static final String AGENT_STARTED_FAIL = "=======Agent started fail!=======";

    public void start() {
        try {
            Register.register();
            Runtime.getRuntime().addShutdownHook(KILLED_HOOK_THREAD);
            HeartThread.doStart();
            // 目前打包，包内不含插件
            PluginManager.init();
            startJobThread();
        } catch (Throwable e) {
            e.printStackTrace(System.out);
            if (JavaUtil.isPreJava6()) {
                System.out.println("=======jdk version is less than 1.6, please upgrade jdk first=======");
            }
            System.out.println(AGENT_STARTED_FAIL);
            Context.log(AGENT_STARTED_FAIL, e);
            System.exit(1);
        }
        System.out.println("=======Agent started success!=======");
        System.out.println("=======Process id: " + Context.labels().get(Context.PROCESS_ID) + "=======");
    }

    public void unregisterAndStop() {
        JobManager.doStop("Upgrade Agent!"); // 若放在unRegister后，则kill掉的任务会被重新调度执行。
        if (StringUtils.isNotEmpty(Context.getServerContext().getSecureKey())) {
            Register.unRegister();
            Context.log("upgrade_unregister_success");
        }
        // doStop是幂等的，没启动没有关系，此处停止重新注册线程。
        ReRegisterThread.doStop();
        HeartThread.doStop();
        Launcher.INSTANCE.stopJobThread();
        Context.log("upgrade_stop_threads_success");
    }


    public void reload() {
    }

    public void stop(int code, String message) {
        try {
            System.out.println(message);
            System.out.println("=======Stop agent!=======");
            JobManager.doStop("Stop agent: " + message);
            ReRegisterThread.doStop();
            HeartThread.doStop();
            stopJobThread();
        } catch (Exception e) {
            LOGGER.error("stop exception", e);
            Context.log("stop exception", e);
        } finally {
            System.exit(code);
        }
    }

    private static final String BLANK_SPACE = " ";

    private static final String JAVA_PATH_SUFFIX = File.separator + "bin" + File.separator + "java";

    public static final Thread KILLED_HOOK_THREAD = new Thread("killed-hook-thread") {
        @Override
        public void run() {
            try {
                Register.unRegister();
            } catch (Exception e) {
                LOGGER.error("unregister exception!", e);
                Context.log("unregister exception", e);
            }

        }
    };

    public static final String RESTART_AGENT_JAR_WITH_COMMAND = "=======Restart agent.jar with command: ";
    static final Thread UPGRADE_HOOK_THREAD = new Thread("upgrade-hook-thread") {
        @Override
        public void run() {
            System.out.println("=======Download the latest agent.jar!=======");
            Context.log("upgrade_start");
            try {
                if (StringUtils.isNotEmpty(Context.getServerContext().getSecureKey())) {
                    Register.unRegister();
                }

                String jarFile = upgradeJar();

                if (isSuperviseRun()) {
                    LOGGER.info("agent run by supervise [uuid={}]", Context.getAgentArgs().getUuid());
                    Context.log("agent run by supervise [uuid={}]", Context.getAgentArgs().getUuid());
                    return;
                }

                List<String> cmds = new ArrayList<String>();

                cmds.add(System.getProperty("java.home") + JAVA_PATH_SUFFIX);

                RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
                List<String> jvmOptions = runtimeMXBean.getInputArguments();
                cmds.addAll(jvmOptions);

                cmds.add("-jar");
                cmds.add(jarFile);
                CollectionUtils.addAll(cmds, Context.getArgs());

                System.out.println(RESTART_AGENT_JAR_WITH_COMMAND + cmds);
                LOGGER.info(RESTART_AGENT_JAR_WITH_COMMAND + "{}", cmds);
                execUpgradeCmd(cmds.toArray(new String[cmds.size()]));
                Context.log("upgrade_finish");
            } catch (Exception e) {
                LOGGER.error("Upgrade exception", e);
                Context.log("upgrade_fail_" + e.getMessage(), e);
            } finally {
                Context.flush();
            }
        }
    };

    @Deprecated
    public void upgrade() {
        System.out.println("=======Stop agent for update!=======");
        Context.log("upgrade_prestart");
        ReRegisterThread.doStop();
        HeartThread.doStop();
        stopJobThread();
        Runtime.getRuntime().removeShutdownHook(KILLED_HOOK_THREAD);
        Runtime.getRuntime().addShutdownHook(UPGRADE_HOOK_THREAD);
        System.exit(0);
    }

    public static boolean isSuperviseRun() {

        try {
            ProcessTree tree = ProcessTree.getTree();
            int pid = Integer.parseInt(Context.labels().get("processId"));
            Process process = tree.getProcess(pid);
            List<String> nameList = new ArrayList<String>();
            getProcessNameList(process, nameList);

            Set<String> targetList = new HashSet();
            targetList.add("run");
            targetList.add("supervise");

            if (nameList.containsAll(targetList)) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("check supervise run error [uuid={}]", Context.getAgentArgs().getUuid(), e);
        }

        return false;
    }

    private static Process getProcessNameList(Process process, List nameList) {

        if (null != process) {
            nameList.add(process.getName());
            Process parent = process.getParent();
            if (null != parent && parent.getPid() > 1) { // mac下pid为0的进程的ppid为0。
                return getProcessNameList(parent, nameList);
            }
        }
        return null;
    }

    public abstract void startJobThread();

    protected abstract void stopJobThread();

    private static final Launcher LAUNCHER_WITH_POLL = new Launcher() {
        @Override
        public void startJobThread() {
            PollJobThread.doStart();
        }

        @Override
        protected void stopJobThread() {
            PollJobThread.doStop();
        }
    };

    public static final Launcher INSTANCE = LAUNCHER_WITH_POLL;

    private static String upgradeJar() throws Exception {
        return new JarUpgrade(Context.getServerContext().getAgentUpdateInfo()).upgrade();
    }

    private static void execCmd(String[] cmd) throws Exception {
        Runtime.getRuntime().exec(cmd);
    }

    private static void execUpgradeCmd(String[] cmd) throws Exception {
        setExecutable4Java();
        execCmd(cmd);
    }

    private static void setExecutable4Java() {

        String javaPath = StringUtils.EMPTY;
        try {
            javaPath = System.getProperty("java.home") + JAVA_PATH_SUFFIX;
            File file = new File(javaPath);
            if (!file.canExecute()) {
                file.setExecutable(true);
                LOGGER.info("set executable for java [uuid={},javaPath={}]",
                        Context.getAgentArgs().getUuid(), javaPath);
                Context.log("set executable for java [javaPath={" + javaPath + "}]");
            }
        } catch (Exception e) {
            LOGGER.error("set executable for java error [uuid={},javaPath={}]",
                    Context.getAgentArgs().getUuid(), javaPath, e);
            Context.log("set executable for java error [javaPath={" + javaPath + "}]", e);
        }
    }

    public static class JarUpgrade {
        private long startTimeMs = 0;

        private long waitMinute = 1;

        private static final long MAX_WAIT_MINUTE = 60;

        private static final long MAX_DURATION_MINUTE = 24 * 60;

        private SystemCmdInfoFactory.AgentUpdateInfo updateInfo;

        public JarUpgrade(SystemCmdInfoFactory.AgentUpdateInfo updateInfo) {
            this.updateInfo = updateInfo;
            this.startTimeMs = System.currentTimeMillis();
            this.waitMinute = 1;
        }

        public String upgrade() throws Exception {
            do {
                try {
                    return upgradeJarOnce();
                } catch (Exception e) {
                    LOGGER.error("UpgradeJarOnce exception!", e);
                    Context.log("upgrade_download_exception", e);
                }
                Thread.sleep(getWaitMinute() * 60 * 1000);
            } while (!isMaxDurationTime());
            throw new AgentException("UpgradeJar fail after MaxDurationTime!");
        }

        private long getWaitMinute() {
            if (this.waitMinute == MAX_WAIT_MINUTE) {
                return MAX_WAIT_MINUTE;
            }
            this.waitMinute *= 2;
            if (this.waitMinute > MAX_WAIT_MINUTE) {
                this.waitMinute = MAX_WAIT_MINUTE;
            }
            return this.waitMinute;
        }

        public void setWaitMinute(long waitMinute) {
            this.waitMinute = waitMinute;
        }

        private boolean isMaxDurationTime() {
            return System.currentTimeMillis() - startTimeMs > 60 * 1000 * MAX_DURATION_MINUTE;
        }

        private String upgradeJarOnce() throws Exception {
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
                if (!OS.WINDOWS.equals(OS.os())) {
                    try {
                        PosixAPI.jnr().chdir(System.getProperty("user.dir"));
                    } catch (Exception e) {
                        LOGGER.warn("chdir exception!", e);
                        Context.log("chdir exception!", e);
                    }
                }

                // 注意：user.dir被删除后，若没有chdir，则new File(upgradeJar).exists()会返回false
                if (new File(upgradeJar).exists()) {
                    if (new File(upgradeJar).renameTo(new File("agent.jar"))) {
                        try {
                            File file = new File(upgradeJar);
                            if (file.exists()) {
                                FileUtils.forceDelete(new File(upgradeJar));
                            }
                        } catch (Exception e) {
                            LOGGER.error("Delete temp upgradeJar:{} exception:", upgradeJar, e);
                        }
                        return "agent.jar";
                    } else {
                        LOGGER.warn("rename upgradeJar to 'agent.jar' error!");
                        return upgradeJar;
                    }
                } else {
                    throw new AgentException("upgradeJar not exists!");
                }
            } catch (Exception e) {
                LOGGER.error("Upgrade download latest jar exception:", e);
                throw e;
            }
        }
    }
}
