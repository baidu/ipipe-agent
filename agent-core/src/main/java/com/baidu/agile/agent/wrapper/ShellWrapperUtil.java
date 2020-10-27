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

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShellWrapperUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShellWrapperUtil.class);

    private static List<ShellWrapperExecutor> executors = new ArrayList<ShellWrapperExecutor>();

    static {
        executors.add(new ShellGitSshWrapperExecutor());
    }

    public static void before(ShellProcessBuilder builder, List<ShellWraper> wrappers) {
        if (null == builder || CollectionUtils.isEmpty(wrappers)) {
            return;
        }
        for (ShellWrapperExecutor executor : executors) {
            for (ShellWraper wrapper : wrappers) {
                if (executor.support(wrapper)) {
                    try {
                        executor.before(builder, wrapper);
                    } catch (Exception e) {
                        LOGGER.error("wrapper.before exception!", e);
                    }
                    break;
                }
            }
        }
    }

    public static void after(ShellProcessBuilder builder, List<ShellWraper> wrapers) {
        if (null == builder || CollectionUtils.isEmpty(wrapers)) {
            return;
        }
        for (ShellWrapperExecutor executor : executors) {
            for (ShellWraper wrapper : wrapers) {
                if (executor.support(wrapper)) {
                    try {
                        executor.after(builder, wrapper);
                    } catch (Exception e) {
                        LOGGER.error("wrapper.after exception!", e);
                    }
                    break;
                }
            }
        }
    }
}
