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

package com.baidu.agile.agent.register;

import com.baidu.agile.agent.AgentEvent;
import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.AgentStatus;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.AgentResponse;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;
import com.baidu.agile.server.common.ObjectResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Register {

    private static final Logger LOGGER = LoggerFactory.getLogger(Register.class);

    public static void register() throws AgentException {
        ObjectResponse<AgentResponse> response = new HttpClient(Context.getAgentArgs().getServer())
                .path("/agent/v1/register")
                .jsonBody(Context.registerRequest())
                .timeout(60000)
                .retry(3)
                .post(new ObjectResponse<AgentResponse>().getClass());

        if (null == response) {
            Context.log("[AGENT_REGISTER_ERROR]:null response!");
            throw new AgentException("register fail: null response");
        }
        Context.log("[AGENT_REGISTER_SUCCESS]");
        if (null != response.getSystemCmd()) {
            switch (response.getSystemCmd()) {
                case AGENT_UPDATE:
                    Context.setAgentUpdateInfo(
                            (SystemCmdInfoFactory.AgentUpdateInfo) response.getSystemCmdInfo());
                    AgentStatus.maintain(AgentEvent.AGENT_UPGRADE);
                    return;
                case CONFIG_UPDATE:
                    AgentStatus.maintain(AgentEvent.CONFIG_UPGRADE);
                    return;
                case RE_REGISTOR:
                    AgentStatus.maintain(AgentEvent.RE_REGISTOR);
                    return;
                case REJECT:
                    AgentStatus.maintain(AgentEvent.REJECT);
                    return;
                default:
                    break;
            }
        }

        if (ObjectResponse.STATUS_OK.equals(response.getStatus())) {
            Context.setContext(response.getData());
            AgentStatus.maintain(AgentEvent.ONLINE);
            LOGGER.info("register success, agent key: {}", response.getData().getAgent().getSecureKey());
            try {
                String labels = Context.labels().toString();
                LOGGER.info("agent labels: " + labels);
                Context.log("agent labels: " + labels);
            } catch (Exception e) {
                LOGGER.error("print agent info exception!", e);
            }
            return;
        }

        LOGGER.error("register return: code:{}, error:{}", response.getStatus(), response.getMessage());

        throw new AgentException(response.getMessage());
    }

    public static void unRegister() throws AgentException {
        ObjectResponse<AgentResponse> response = new HttpClient(Context.getAgentArgs().getServer())
                .path("/agent/v1/unRegister")
                .param("secureKey", Context.getServerContext().getSecureKey())
                .timeout(60000)
                .retry(3)
                .post(new ObjectResponse<AgentResponse>().getClass());

        if (null == response) {
            throw new AgentException("unregister fail: null response");
        }

        if (ObjectResponse.STATUS_OK.equals(response.getStatus())) {
            LOGGER.info("unregister success");
            return;
        }

        LOGGER.error("unregister return: code:{}, error:{}", response.getStatus(), response.getMessage());

        throw new AgentException(response.getMessage());
    }

}
