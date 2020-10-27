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

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.heart.HeartThread;
import com.baidu.agile.agent.register.Register;
import com.baidu.agile.server.agent.bean.AgentResponse;
import com.baidu.agile.server.common.ObjectResponse;

public class Recover {

    private static final Logger LOGGER = LoggerFactory.getLogger(Recover.class);

    /**
     * 升级豁免(添加到白名单) + 重新注册 +  + 恢复心跳 + 再次轮询任务
     */
    protected static void restart() {
        try {
            log("upgrade_restart_start");
            exemptUpgrade();
            Register.register();
            HeartThread.doStart();
            Launcher.INSTANCE.startJobThread();
            System.out.println("=========== upgrade_restart_finish [" + new Date() + "]==============");
            log("upgrade_restart_finish");
        } catch (Throwable e) {
            log("upgrade_restart_fail_has_error_" + e.getMessage() + "\n" + ExceptionUtils.getFullStackTrace(e));
        }
    }

    /**
     * 将自己添加升级白名单中
     */
    protected static void exemptUpgrade() throws AgentException, InterruptedException {

        ObjectResponse<AgentResponse> response = new HttpClient(Context.getAgentArgs().getServer())
                .path("/agent/v1/upgrade/exempt")
                .param("uuid", Context.getAgentArgs().getUuid())
                .retry(3)
                .post(new ObjectResponse<AgentResponse>().getClass());

        if (null == response) {
            log("upgrade_exempt_fail_no_response");
            throw new AgentException("upgrade_exempt_fail_no_response");
        }

        if (!ObjectResponse.STATUS_OK.equals(response.getStatus())) {
            log("upgrade_exempt_fail_" + response.getMessage());
            throw new AgentException("upgrade_exempt_fail_" + response.getMessage());
        }

        if (!"true".equals(response.getMessage())) {
            throw new AgentException("upgrade_exempt_fail_return_false");
        }

        Thread.sleep(60000); // 等待白名单生效
        log("upgrade_exempt_success");
    }

    private static void log(String log) {
        Watcher.log(log, LOGGER);
    }
}
