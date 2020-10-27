package com.baidu.agile.agent.common.http;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpClient.class})
public class HttpClientTest {

    @Mock
    private HttpURLConnection connection;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        URL url = PowerMockito.mock(URL.class);
        PowerMockito.whenNew(URL.class).withAnyArguments().thenReturn(url);
        Mockito.when(url.openConnection()).thenReturn(this.connection);
    }

    @Test
    public void testPostOk() throws Exception {
        PowerMockito.doReturn(200).when(connection).getResponseCode();
        PowerMockito.doReturn(new ByteArrayOutputStream()).when(connection).getOutputStream();
        PowerMockito.doReturn(new ByteArrayInputStream("ok".getBytes())).when(connection).getInputStream();
        PowerMockito.doReturn(new ByteArrayInputStream("".getBytes())).when(connection).getErrorStream();
        String response = new HttpClient("http://localhost")
                .path("/api")
                .param("p1", "1")
                .header("h1", "1")
                .timeout(100)
                .retry(1)
                .body("", "{}")
                .encoding("UTF-8")
                .post(String.class);
        Assert.assertEquals("ok", response);
    }

    @Test
    public void testGetNull() throws Exception {
        PowerMockito.doReturn(401).when(connection).getResponseCode();
        PowerMockito.doReturn(new ByteArrayOutputStream()).when(connection).getOutputStream();
        PowerMockito.doReturn(new ByteArrayInputStream("ok".getBytes())).when(connection).getInputStream();
        HttpClientTest response = new HttpClient("http://localhost")
                .path("/api")
                .param(null)
                .header(null)
                .jsonBody(Arrays.asList(""))
                .timeout(100)
                .encoding(null)
                .retry(3)
                .get(HttpClientTest.class);
        Assert.assertNull(response);
    }

    @Test
    public void testGetObject() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("h1", "1");

        PowerMockito.doReturn(401).when(connection).getResponseCode();
        PowerMockito.doReturn(new ByteArrayOutputStream()).when(connection).getOutputStream();
        PowerMockito.doReturn(new ByteArrayInputStream("{}".getBytes())).when(connection).getInputStream();
        HttpClientTest response = new HttpClient("http://localhost")
                .path("/api")
                .param(map)
                .header(map)
                .timeout(100)
                .body(new ByteArrayInputStream(new byte[]{}))
                .get(HttpClientTest.class);
        Assert.assertNotNull(response);
    }

    @Test
    public void testGetObjectException() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("h1", "1");

        PowerMockito.doReturn(401).when(connection).getResponseCode();
        PowerMockito.doReturn(new ByteArrayOutputStream()).when(connection).getOutputStream();
        PowerMockito.doThrow(new RuntimeException()).when(connection).getInputStream();
        HttpClientTest response = new HttpClient("http://localhost")
                .path("/api")
                .param(map)
                .header(map)
                .timeout(100)
                .body(new ByteArrayInputStream(new byte[]{}))
                .get(HttpClientTest.class);
        Assert.assertNull(response);
    }

    @Test
    public void testPostObject() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("h1", "1");

        PowerMockito.doReturn(401).when(connection).getResponseCode();
        PowerMockito.doReturn(new ByteArrayOutputStream()).when(connection).getOutputStream();
        PowerMockito.doReturn(new ByteArrayInputStream("{}".getBytes())).when(connection).getInputStream();

        InputStream inputStreamBody = Mockito.spy(new ByteArrayInputStream(new byte[]{}));
        Mockito.doThrow(new IOException()).when(inputStreamBody).close();

        HttpClientTest response = new HttpClient("http://localhost")
                .path("/api")
                .param(map)
                .header(map)
                .timeout(100)
                .body(inputStreamBody)
                .post(HttpClientTest.class);
        Assert.assertNotNull(response);
    }
}