package com.baidu.agile.agent.common.http;

import com.baidu.agile.agent.hook.cov.CovHttpClientUtil;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClients.class})
@PowerMockIgnore("javax.net.ssl.*")
public class CovHttpClientUtilTest {

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(HttpClients.class);
    }

    @Test
    public void testPostOk() throws Exception {
        String url = "http://test";
        Map<String, String> params = new HashMap<String, String>();
        params.put("p1", "v1");
        Map<String, FileBody> fileParam = new HashMap<String, FileBody>();
        fileParam.put("f1", new FileBody(new File("mock")));

        HttpClientBuilder builder = Mockito.spy(HttpClientBuilder.create());

        PowerMockito.doReturn(builder).when(HttpClients.class, "custom");

        CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

        Mockito.doReturn(httpClient).when(builder).build();

        CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);

        StatusLine statusLine = Mockito.mock(StatusLine.class);
        Mockito.doReturn(200).when(statusLine).getStatusCode();

        Mockito.when(response.getStatusLine()).thenReturn(statusLine);
        Mockito.when(response.getEntity()).thenReturn(new StringEntity("{}"));

        Mockito.doReturn(response).when(httpClient).execute(Mockito.any(HttpRequestBase.class));

        Assert.assertNotNull(CovHttpClientUtil.postWithFileBody(url, params, fileParam, 1));
    }
}