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

package com.baidu.agile.agent.hook;

import com.baidu.agile.agent.hook.bos.BosUploadHookExecute;
import com.baidu.agile.agent.hook.cov.ShellCovHookExecute;
import com.baidu.agile.agent.hook.cov.ShellCoverageHookExecute;
import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.server.job.bean.hook.ShellHook;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.bos.BosUploadHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellCovHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellCoverageHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellInvalidateGitSshHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellInvalidateGitSshResponse;

/**
 * Hook执行工厂
 */
public class HookExecuteFactory {

    private static final ShellCovHookExecute covHookExecutor = new ShellCovHookExecute();

    private static final BosUploadHookExecute bosUploadHookExecutor = new BosUploadHookExecute();

    private static final HookExecute invalidateGitSshHookExecutor = new HookExecute() {
        @Override
        public ShellHookResponse triggerHook(ShellHook shellHook, String uuid, String ws, IJobLogger jobLogger) {
            if (shellHook instanceof ShellInvalidateGitSshHook) {
                ShellInvalidateGitSshResponse response = new ShellInvalidateGitSshResponse();
                response.setPublicKey(((ShellInvalidateGitSshHook) shellHook).getPublicKey());
                return response;
            }
            return null;
        }
    };

    private static final ShellCoverageHookExecute coverageHookExecutor = new ShellCoverageHookExecute();

    public static ShellHookResponse execute(ShellHook shellHook, String uuid, String ws, IJobLogger jobLogger) {
        if (null == shellHook) {
            jobLogger.log("ERROR", "null hook!");
            return null;
        }
        if (shellHook instanceof ShellCovHook) {
            return covHookExecutor.triggerHook(shellHook, uuid, ws, jobLogger);
        } else if (shellHook instanceof ShellInvalidateGitSshHook) {
            return invalidateGitSshHookExecutor.triggerHook(shellHook, uuid, ws, jobLogger);
        } else if (shellHook instanceof ShellCoverageHook) {
            return coverageHookExecutor.triggerHook(shellHook, uuid, ws, jobLogger);
        } else if (shellHook instanceof BosUploadHook) {
            return bosUploadHookExecutor.triggerHook(shellHook, uuid, ws, jobLogger);
        }
        jobLogger.log("ERROR", "unknown hook: " + shellHook.getClass());
        return null;
    }

}
