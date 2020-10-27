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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(resourceBundle = "com.baidu.agile.agent.Args")
public class Args {

    @Parameter(names = {"--server", "-s"}, required = false, descriptionKey = "server")
    private String server;

    @Parameter(names = {"--token", "-t"}, required = true, descriptionKey = "token")
    private String uuid;

    @Parameter(names = {"--help", "-h"}, help = true, descriptionKey = "help")
    private boolean help;

    @Parameter(names = {"--version", "-v"}, required = false, descriptionKey = "version")
    private boolean version;

    @Parameter(names = {"--mode", "-m"}, required = false, descriptionKey = "mode")
    private String mode = "online";

    @Parameter(names = {"--durable", "-d"}, required = false, descriptionKey = "durable")
    private boolean durable = false;

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isVersion() {
        return version;
    }

    public void setVersion(boolean version) {
        this.version = version;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }


    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }
}
