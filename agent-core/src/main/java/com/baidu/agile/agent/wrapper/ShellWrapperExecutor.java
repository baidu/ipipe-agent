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

package com.baidu.agile.agent.wrapper;

import com.baidu.agile.agent.execute.ShellProcessBuilder;
import com.baidu.agile.server.job.bean.wrapper.ShellWraper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class ShellWrapperExecutor<ShellWrapperT extends ShellWraper> {
    public final boolean support(ShellWraper wrapper) {
        if (null == wrapper) {
            return false;
        }
        Type executorType = this.getClass().getGenericSuperclass();
        Type wrapperType = ((ParameterizedType) executorType).getActualTypeArguments()[0];
        return wrapper.getClass() == wrapperType;
    }

    public abstract void before(ShellProcessBuilder builder, ShellWrapperT wrapper);

    public abstract void after(ShellProcessBuilder builder, ShellWrapperT wrapper);
}
