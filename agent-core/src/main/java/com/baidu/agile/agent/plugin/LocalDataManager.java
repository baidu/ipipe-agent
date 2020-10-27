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

import com.baidu.agile.server.agent.plugin.ILocalDataManager;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * plugin job local data manager
 * ci-build插件中有使用
 */
public class LocalDataManager implements ILocalDataManager {

    private ConcurrentHashMap<String, Object> localDataMapper = new ConcurrentHashMap<String, Object>();

    @Override
    public void put(String key, Object value) {
        localDataMapper.put(key, value);
    }

    @Override
    public Object get(String key) {
        if (localDataMapper.contains(key)) {
            return localDataMapper.get(key);
        }
        return null;
    }

    @Override
    public Set<String> getKeySet() {
        return localDataMapper.keySet();
    }

    @Override
    public Set<Object> getValueSet() {
        return new HashSet<Object>(localDataMapper.values());
    }
}
