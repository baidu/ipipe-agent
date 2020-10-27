package com.baidu.agile.agent.plugin;

import com.baidu.agile.agent.Args;
import com.baidu.agile.agent.common.ftp.FtpDownloader;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.common.http.HttpDownloader;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.plugin.PluginInfo;
import com.baidu.agile.server.agent.bean.plugin.PluginInfos;
import com.baidu.agile.server.common.ObjectResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Collections;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginManager.class, Context.class, FtpDownloader.class, HttpDownloader.class})
public class PluginManagerTest {

    public static final String PLUGIN_PATH = System.getProperty("user.dir") + File.separator + ".plugin/";

    @Before
    public void before() throws Exception {
        Args args = new Args();
        PowerMockito.mockStatic(Context.class);
        PowerMockito.when(Context.getAgentArgs()).thenReturn(args);

        PowerMockito.mockStatic(FtpDownloader.class);
        PowerMockito.mockStatic(HttpDownloader.class);

        File file = new File(PLUGIN_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        File pluginFile = new File(FilenameUtils.concat(PLUGIN_PATH, "plugin-output-process-1.0.1.zip"));
        if (!pluginFile.exists()) {
            pluginFile.createNewFile();
        }
    }

    @After
    public void tearDown() {
        File file = new File(PLUGIN_PATH);
        FileUtils.deleteQuietly(file);
    }

    @Test
    public void testDownloadPluginPackageException() throws Exception {
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setDownloadUrl("ftp://test");
        ObjectResponse<PluginInfo> response = new ObjectResponse<PluginInfo>(pluginInfo);
        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.doReturn(response).when(httpClient).post(Mockito.any(Class.class));

        FtpDownloader downloader = Mockito.spy(new FtpDownloader());
        PowerMockito.whenNew(FtpDownloader.class).withAnyArguments().thenReturn(downloader);
        Mockito.doThrow(Exception.class).when(downloader).download();

        Assert.assertNull(new PluginManager.AgentPluginDownloader().tryToDownloadPluginPackage("p1"));
    }

    @Test
    public void testDownloadPluginPackageByFtp() throws Exception {
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setDownloadUrl("ftp://test");
        ObjectResponse<PluginInfo> response = new ObjectResponse<PluginInfo>(pluginInfo);
        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.doReturn(response).when(httpClient).post(Mockito.any(Class.class));

        FtpDownloader downloader = Mockito.spy(new FtpDownloader());
        PowerMockito.whenNew(FtpDownloader.class).withAnyArguments().thenReturn(downloader);
        Mockito.doNothing().when(downloader).download();

        Assert.assertNotNull(new PluginManager.AgentPluginDownloader().tryToDownloadPluginPackage("p1"));
    }

    @Test
    public void testDownloadPluginPackageHttp() throws Exception {
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setDownloadUrl("Http://test");
        ObjectResponse<PluginInfo> response = new ObjectResponse<PluginInfo>(pluginInfo);
        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.doReturn(response).when(httpClient).post(Mockito.any(Class.class));

        HttpDownloader downloader = Mockito.spy(new HttpDownloader());
        PowerMockito.whenNew(HttpDownloader.class).withAnyArguments().thenReturn(downloader);
        Mockito.doNothing().when(downloader).download();

        Assert.assertNotNull(new PluginManager.AgentPluginDownloader().tryToDownloadPluginPackage("p1"));
    }

    @Test
    public void testTryUpdatePlugin() throws Exception {
        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.when(httpClient.path(Mockito.anyString())).thenReturn(httpClient);
        Mockito.when(httpClient.retry(Mockito.anyInt())).thenReturn(httpClient);
        Mockito.when(httpClient.jsonBody(Mockito.anyObject())).thenReturn(httpClient);
        Mockito.when(httpClient.timeout(Mockito.anyInt())).thenReturn(httpClient);

        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setKey("plugin-output-process");
        pluginInfo.setName("plugin-output-process-1.0.2");
        PluginInfos pluginInfos = new PluginInfos(Collections.singletonList(pluginInfo));
        ObjectResponse<PluginInfos> response = new ObjectResponse<PluginInfos>(pluginInfos);
        Mockito.when(httpClient.get(Mockito.any(Class.class))).thenReturn(response);
        PluginManager.tryDeleteOldVersionPlugins();
    }

    @Test
    public void testTryUpdatePluginWhenPluginInfoEmpty() throws Exception {
        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.when(httpClient.path(Mockito.anyString())).thenReturn(httpClient);
        Mockito.when(httpClient.retry(Mockito.anyInt())).thenReturn(httpClient);
        Mockito.when(httpClient.jsonBody(Mockito.anyObject())).thenReturn(httpClient);
        Mockito.when(httpClient.timeout(Mockito.anyInt())).thenReturn(httpClient);

        Mockito.when(httpClient.post(Mockito.any(Class.class))).thenReturn(null);
        PluginManager.tryDeleteOldVersionPlugins();
    }

    @Test
    public void testTryUpdatePluginPathNotFound() throws Exception {
        tearDown();

        ObjectResponse<PluginInfo> response = new ObjectResponse<PluginInfo>();
        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setName("plugin-output-process-1.0.2");
        response.setData(pluginInfo);
        PluginManager.tryDeleteOldVersionPlugins();
    }

    @Test
    public void testTryUpdatePluginEmpty() throws Exception {
        File file = new File(PLUGIN_PATH);
        try {
            FileUtils.cleanDirectory(file);
        } catch (Exception e) {
            // ignore
        }

        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.when(httpClient.path(Mockito.anyString())).thenReturn(httpClient);
        Mockito.when(httpClient.retry(Mockito.anyInt())).thenReturn(httpClient);
        Mockito.when(httpClient.jsonBody(Mockito.anyObject())).thenReturn(httpClient);
        Mockito.when(httpClient.timeout(Mockito.anyInt())).thenReturn(httpClient);

        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setKey("plugin-output-process");
        pluginInfo.setName("plugin-output-process-1.0.2");
        PluginInfos pluginInfos = new PluginInfos(Collections.singletonList(pluginInfo));
        ObjectResponse<PluginInfos> response = new ObjectResponse<PluginInfos>(pluginInfos);
        Mockito.when(httpClient.get(Mockito.any(Class.class))).thenReturn(response);
        PluginManager.tryDeleteOldVersionPlugins();
    }
}
