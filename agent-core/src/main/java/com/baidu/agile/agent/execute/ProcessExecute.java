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

package com.baidu.agile.agent.execute;

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.job.JobManager;
import com.baidu.agile.agent.log.LogService;
import com.baidu.agile.server.job.bean.JobStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 进程执行
 */
@Deprecated
public class ProcessExecute {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessExecute.class);

    /**
     * 执行命令
     *
     * @param uuid    任务id
     * @param command 执行的命令
     * @return
     * @throws IOException
     */
    public static Process executeCommand(String uuid, String command, String[] envs) throws IOException {


        String[] realCommand = {"/bin/sh", "-c", setWorkSpace(uuid, command)};
        LOGGER.info("start execute command.");
        Process process = Runtime.getRuntime().exec(realCommand, envs,
                new File(Context.ws() + File.separator + uuid));

        // 等到任务结束线程
        newTaskThread(uuid, process);

        // 实时日志线程
        LOGGER.info("start collect log.");
        InputStream inputStream = process.getInputStream();
        newLogThread(uuid, inputStream);

        return process;
    }

    /**
     * 设置工作空间
     *
     * @param uuid    任务id
     * @param command 用户的命令
     * @return
     */
    private static String setWorkSpace(String uuid, String command) {
        String ws = Context.ws();
        StringBuilder sb = new StringBuilder();
        sb.append("mkdir -p ").append(ws).append(File.separator).append(uuid).append("\n");
        sb.append("cd ").append(ws).append(File.separator).append(uuid).append("\n");
        sb.append(command).append("\n");
        sb.append("rm -rf ").append(ws).append(File.separator).append(uuid).append("\n");
        return sb.toString();
    }

    /**
     * 执行任务的线程
     *
     * @param uuid    任务id
     * @param process
     * @return 任务执行成功返回true，失败返回false
     */
    public static void newTaskThread(final String uuid, final Process process) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int returnVal = process.waitFor();
                    if (returnVal == 0) {
                        JobManager.endJob(uuid, JobStatus.SUCC);
                    } else {
                        JobManager.endJob(uuid, JobStatus.FAILED);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("wait process failed!!!");
                    JobManager.endJob(uuid, JobStatus.FAILED);
                }
            }
        }).start();

    }

    /**
     * 写日志线程
     *
     * @param inputStream
     */
    private static void newLogThread(final String uuid, final InputStream inputStream) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 存储日志
                LogService.saveOrAppendLog(uuid, inputStream);
            }
        }).start();
    }

    /**
     * kill进程
     *
     * @param process 进程
     */
    public static void killProcess(Process process) {
        process.destroy();
    }
}
