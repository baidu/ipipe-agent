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

package com.baidu.agile.agent.classload;

import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentClassloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentClassloader.class);

    protected static ClassLoader classLoader = AgentClassloader.class.getClassLoader();

    // 所有的class都加载，防止agent.jar被删除后无法加载需要使用的类
    public static void loadAgileClass() {
        try {
            Enumeration<URL> urls = classLoader.getResources("com/baidu/agile");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if ("jar".equals(url.getProtocol())) {
                    JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                    JarFile jarFile = jarURLConnection.getJarFile();
                    if (jarFile == null) {
                        continue;
                    }
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String jarEntryName = jarEntry.getName();
                        if (jarEntryName.endsWith(".class") && jarEntryName.startsWith("com/baidu/agile")) {
                            String className =
                                    jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                            classLoader.loadClass(className);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("load agile class error", e);
        }
    }
}
