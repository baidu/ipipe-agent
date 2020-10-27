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

package com.baidu.agile.agent.process;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class EnvVarsMatcher implements ProcessMatcher {

    private Map<String, String> envVars;

    public EnvVarsMatcher(Map<String, String> envVars) {
        this.envVars = envVars;
    }

    public EnvVarsMatcher(String envVar, String value) {
        this.envVars = new HashMap<String, String>();
        this.envVars.put(envVar, value);
    }

    public boolean match(Process process) {
        if (MapUtils.isEmpty(envVars)) {
            return true;
        }
        if (MapUtils.isEmpty(process.getEnvVars())) {
            return false;
        }
        for (Map.Entry<String, String> entry : this.envVars.entrySet()) {
            String envVar = entry.getKey();
            if (!StringUtils.equals(entry.getValue(), process.getEnvVars().get(envVar))) {
                return false;
            }
        }
        return true;
    }
}
