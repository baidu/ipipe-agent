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

package com.baidu.agile.agent;

import com.baidu.agile.agent.classload.AgentClassloader;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.os.OS;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.commons.lang.StringUtils;

/**
 * Agent main entrance
 */
public class Main {

    private static final JCommander USAGE = new JCommander(new Args());

    static {
        USAGE.setProgramName("java -jar agent.jar");
    }

    public static void main(String[] arguments) {
        if (!OS.support()) {
            System.out.println("Not support current os: " + OS.os());
            System.out.println("Supported os are: " + StringUtils.join(OS.getSupportedOs(), ", "));
            System.exit(1);
        }
        Args args = new Args();
        try {
            new JCommander(args, arguments);
        } catch (ParameterException pe) {
            System.out.println(pe.getMessage());
            USAGE.usage();
            return;
        }
        if (args.isVersion()) {
            System.out.println(Context.VERSION);
            return;
        }
        if (args.isHelp()) {
            USAGE.usage();
            return;
        }
        Context.init(arguments, args);
        AgentClassloader.loadAgileClass();
        Launcher.INSTANCE.start();
    }

}
