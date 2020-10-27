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

package com.baidu.agile.agent.os;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public enum OS {
    WINDOWS("Windows"),
    LINUX("Linux"),
    SOLARIS("Sun os"),
    DARWIN("Mac os"),
    UNKNOWN("Unknown os");

    private String desc;

    private OS(String desc) {
        this.desc = desc;
    }

    private static final OS os = parseOs();

    private static OS parseOs() {
        if (File.pathSeparatorChar == ';') {
            return WINDOWS;
        }
        String os = System.getProperty("os.name");
        if ("Linux".equals(os)) {
            return LINUX;
        } else if ("SunOS".equals(os)) {
            return SOLARIS;
        } else if ("Mac OS X".equals(os)) {
            return DARWIN;
        }
        return UNKNOWN;
    }

    public static OS os() {
        return os;
    }

    @Override
    public String toString() {
        return desc;
    }

    private static List<OS> supportedOs = Arrays.asList(LINUX, DARWIN, SOLARIS, WINDOWS);

    public static List<OS> getSupportedOs() {
        return supportedOs;
    }

    public static boolean support() {
        return supportedOs.contains(os);
    }

}
