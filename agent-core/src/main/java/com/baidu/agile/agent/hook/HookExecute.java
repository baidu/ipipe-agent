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

import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.server.job.bean.hook.ShellHook;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;


public interface HookExecute {

    /**
     * 执行hook统一接口
     *
     * @param shellHook hook实体类
     * @param uuid      uuid
     * @param ws        ws
     * @param jobLogger jobLogger
     * @return
     */
    ShellHookResponse triggerHook(ShellHook shellHook, String uuid, String ws, IJobLogger jobLogger);

}
