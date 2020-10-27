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

package com.baidu.agile.agent.plugin;

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.plugin.IContext;
import com.baidu.agile.server.agent.plugin.ILocalDataManager;
import com.baidu.agile.server.agent.plugin.PluginExecutorWrapper;
import com.baidu.agile.server.job.bean.JobBaseParameter;

/**
 * 插件执行上下文
 */
public class PluginContext implements IContext {

    private JobBaseParameter jobBaseParameter;

    private PluginExecutorWrapper pluginExecutorWrapper;

    private ILocalDataManager localDataManager = new LocalDataManager();

    private String jobUuid;

    private String runMode;

    public PluginContext(String jobUuid,
                         JobBaseParameter jobBaseParameter,
                         PluginExecutorWrapper pluginExecutorWrapper) {
        this.jobUuid = jobUuid;
        this.jobBaseParameter = jobBaseParameter;
        this.pluginExecutorWrapper = pluginExecutorWrapper;
        this.runMode = Context.getAgentArgs().getMode();
    }

    @Override
    public JobBaseParameter getJobParameter() {
        return jobBaseParameter;
    }

    @Override
    public String getJobUuid() {
        return jobUuid;
    }

    @Override
    public String getWorkspace() {
        return Context.ws(jobUuid);
    }

    @Override
    public ILocalDataManager getLocalDataMannager() {
        return localDataManager;
    }

    @Override
    public String getPluginInstallPath() {
        return PluginManager.PLUGIN_PATH + pluginExecutorWrapper.getName();
    }

    @Override
    public String runMode() {
        return this.runMode;
    }
}
