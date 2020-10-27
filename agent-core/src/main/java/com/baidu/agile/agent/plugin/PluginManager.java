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

import com.baidu.agile.agent.common.ftp.FtpDownloader;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.common.http.HttpDownloader;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.plugin.PluginInfo;
import com.baidu.agile.server.agent.bean.plugin.PluginInfos;
import com.baidu.agile.server.agent.plugin.PluginExecutorWrapper;
import com.baidu.agile.server.common.ObjectResponse;
import com.baidu.agile.server.exception.PluginClassLoadException;
import com.baidu.agile.server.utils.IPluginPackageDownloader;
import com.baidu.agile.server.utils.PluginClasspathLoader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * 插件管理器
 */
public class PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginManager.class);

    private static final int DOWNLOAD_TIMEOUT = 60 * 1000;

    public static final String PLUGIN_PATH = System.getProperty("user.dir") + File.separator + ".plugin/";

    /**
     * 初始化插件管理器,即加载所有插件包,并生成插件executor实例
     */
    public static void init() {
        try {
            tryDeleteOldVersionPlugins();
            PluginClasspathLoader.setClassOnly(false);
            PluginClasspathLoader.setPluginPackageDownloader(new AgentPluginDownloader());
            // 会加载.plugin文件夹下的所有文件(不区分版本)
            PluginClasspathLoader.loadPlugin(PLUGIN_PATH);
        } catch (Exception e) {
            LOGGER.error("load plugin failed!", e);
        }
    }

    public static void tryDeleteOldVersionPlugins() {
        File pluginDir = new File(PLUGIN_PATH);
        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            return;
        }

        List<PluginInfo> pluginInfos = getPluginInfosFromServer();
        LOGGER.info("server pluginInfos [{}]", pluginInfos);
        if (CollectionUtils.isEmpty(pluginInfos)) {
            return;
        }

        for (PluginInfo pluginInfo : pluginInfos) {
            String pluginKey = pluginInfo.getKey();
            if (StringUtils.isBlank(pluginKey)) {
                continue;
            }
            String[] pluginFiles = pluginDir.list(new PrefixFileFilter(pluginKey));
            if (ArrayUtils.isEmpty(pluginFiles)) {
                continue;
            }

            for (String pluginFile : pluginFiles) {
                // pluginInfo.getName()带有版本号信息
                if (!StringUtils.startsWith(pluginFile, pluginInfo.getName())) {
                    LOGGER.info("pluginFile [{}] need update to [{}]", pluginFile, pluginInfo.getName());
                    FileUtils.deleteQuietly(new File(FilenameUtils.concat(PLUGIN_PATH, pluginFile)));
                }
            }
        }
    }

    private static List<PluginInfo> getPluginInfosFromServer() {
        ObjectResponse<PluginInfos> response;
        try {
            response = new HttpClient(Context.getAgentArgs().getServer())
                    .path("/agent/v1/plugin/infos")
                    .retry(3)
                    .get(new ObjectResponse<PluginInfos>().getClass());
        } catch (Exception e) {
            LOGGER.error("plugin install exception!", e);
            return null;
        }
        if (null == response) {
            LOGGER.error("plugin info return null!");
            return null;
        }
        return null == response.getData() ? Collections.<PluginInfo>emptyList() : response.getData().getData();
    }

    /**
     * 通过任务参数类型获取对应的执行器
     *
     * @param paramTypeName 任务输入参数的Class名称
     * @return plugin executor instance
     */
    public static PluginExecutorWrapper getPluginExecutorByParameterType(String paramTypeName) {
        if (StringUtils.isBlank(paramTypeName)) {
            return null;
        }
        return PluginClasspathLoader.getPluginExecutorByParamTypeName(paramTypeName);
    }

    public static class AgentPluginDownloader implements IPluginPackageDownloader {


        @Override
        public String tryToDownloadPluginPackage(String paramTypeName) throws PluginClassLoadException {
            try {

                PluginInfo pluginInfo = getPluginInfoFromServer(paramTypeName);
                if (null == pluginInfo) {
                    return null;
                }
                if (!createDir(PLUGIN_PATH)) {
                    return null;
                }
                String pluginFileName = PLUGIN_PATH + File.separator + pluginInfo.getName() + ".zip";

                if (pluginInfo.getDownloadUrl().startsWith("ftp://")) {
                    new FtpDownloader()
                            .from(pluginInfo.getDownloadUrl())
                            .to(pluginFileName)
                            .username(pluginInfo.getUser())
                            .password(pluginInfo.getPassword())
                            .download();
                } else {
                    new HttpDownloader()
                            .from(pluginInfo.getDownloadUrl())
                            .to(pluginFileName)
                            .download();
                }
                return pluginFileName;
            } catch (Exception e) {
                LOGGER.error("download plugin failed!", e);
                return null;
            }
        }

        private static PluginInfo getPluginInfoFromServer(String paramTypeName) {
            ObjectResponse<PluginInfo> response;
            try {
                response = new HttpClient(Context.getAgentArgs().getServer())
                        .path("/agent/v1/plugin/info")
                        .jsonBody(Context.pluginInfoRequest(paramTypeName))
                        .timeout(DOWNLOAD_TIMEOUT)
                        .post(new ObjectResponse<PluginInfo>().getClass());
            } catch (Exception e) {
                LOGGER.error("plugin install exception!", e);
                return null;
            }
            if (null == response) {
                LOGGER.error("plugin info return null!");
                return null;
            }
            return response.getData();
        }

        private static boolean createDir(String destDirName) {
            File dir = new File(destDirName);
            if (dir.exists()) {
                LOGGER.info("plugin dir exist!");
                return true;
            }
            if (dir.mkdirs()) {
                LOGGER.info("create plugin dir succ");
                return true;
            } else {
                LOGGER.error("create plugin dir failed!");
                return false;
            }
        }
    }
}
