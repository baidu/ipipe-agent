package com.baidu.agile.agent.upgrade;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.server.common.CommonResponse;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Watcher.class})
public class WatcherTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        UpgraderTest.init();

    }

    @Test
    public void testWatch4ProcessSuccess() throws Exception {
        Process process = PowerMockito.mock(Process.class);
        PowerMockito.when(process.exitValue()).thenThrow(new IllegalThreadStateException());
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.param(anyString(), anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(anyInt())).thenReturn(mockHttpClient);
        CommonResponse response = new CommonResponse();
        response.setMessage("{\"online\":true,\"newestVersion\":true}");
        PowerMockito.when(mockHttpClient.get(Mockito.any(Class.class))).thenReturn(response);
        UpgradeStatus result = Watcher.watch(process);
        Assert.assertTrue(result.isOnline());
        Assert.assertTrue(result.isNewestVersion());
    }

    @Test
    public void testWatch4ProcessFail() throws Exception {
        Process process = PowerMockito.mock(Process.class);
        UpgradeStatus result = Watcher.watch(process);
        Assert.assertEquals(result, null);
    }

    @Test
    public void testCheckVersion4Success() throws Exception {

        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.param(anyString(), anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(anyInt())).thenReturn(mockHttpClient);
        CommonResponse response = new CommonResponse();
        response.setMessage("{\"online\":true,\"newestVersion\":true}");
        PowerMockito.when(mockHttpClient.get(Mockito.any(Class.class))).thenReturn(response);
        UpgradeStatus result = Watcher.checkVersion();
        Assert.assertTrue(result.isOnline());
        Assert.assertTrue(result.isNewestVersion());
    }

    @Test
    public void testCheckVersion4NullResponse() throws Exception {

        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.param(anyString(), anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(anyInt())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.get(Mockito.any(Class.class))).thenReturn(null);
        UpgradeStatus result = Watcher.checkVersion();
        Assert.assertEquals(result, null);
    }

    @Test
    public void testCheckVersion4Exception() throws Exception {

        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.param(anyString(), anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(anyInt())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.get(Mockito.any(Class.class))).thenThrow(new RuntimeException());
        UpgradeStatus result = Watcher.checkVersion();
        Assert.assertEquals(result, null);
    }

    @Test
    public void testExemptUpgrade4BadResponse() throws Exception {

        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.param(anyString(), anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(anyInt())).thenReturn(mockHttpClient);
        CommonResponse response = new CommonResponse();
        response.setStatus(CommonResponse.STATUS_NO_PERMISSION);
        response.setMessage("true");
        PowerMockito.when(mockHttpClient.get(Mockito.any(Class.class))).thenReturn(response);
        UpgradeStatus result = Watcher.checkVersion();
        Assert.assertEquals(result, null);
    }

    @Test
    public void testWatch4ProcessException() throws Exception {
        UpgradeStatus result = Watcher.watch(null);
        Assert.assertEquals(result, null);
    }

}
