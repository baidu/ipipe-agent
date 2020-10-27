package com.baidu.agile.agent.common.http;

import com.baidu.agile.agent.AgentException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpDownloader.class, HttpClient.class})
public class HttpDownloaderTest {

    @Before
    public void before() {
        PowerMockito.mockStatic(HttpClient.class);
    }

    @Test(expected = AgentException.class)
    public void testDownloadException() throws Exception {
        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.doThrow(Exception.class).when(httpClient).get(Mockito.any(Class.class));
        new HttpDownloader()
                .from("/f1").to("/d1")
                .download();
    }

    @Test(expected = AgentException.class)
    public void testDownloadFail() throws Exception {
        HttpClient httpClient = Mockito.spy(new HttpClient("http://test"));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.doReturn(null).when(httpClient).get(Mockito.any(Class.class));
        new HttpDownloader()
                .from("/f1").to("/d1")
                .download();
    }
}
