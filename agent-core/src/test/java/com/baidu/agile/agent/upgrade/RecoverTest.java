package com.baidu.agile.agent.upgrade;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.heart.HeartThread;
import com.baidu.agile.agent.register.Register;
import com.baidu.agile.server.common.ObjectResponse;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Register.class, HeartThread.class, Launcher.class, Recover.class})
public class RecoverTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        UpgraderTest.init();
    }

    @Test
    public void testRestartSuccess() throws Exception {
        PowerMockito.mockStatic(Register.class);
        PowerMockito.mockStatic(HeartThread.class);
        PowerMockito.mockStatic(Launcher.class);
        HttpClient mockHttpClient = PowerMockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(Mockito.anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.param(Mockito.anyString(), Mockito.anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(Mockito.anyInt())).thenReturn(mockHttpClient);
        ObjectResponse objectResponse = new ObjectResponse();
        objectResponse.setMessage("true");
        PowerMockito.when(mockHttpClient.post(Mockito.any(Class.class))).thenReturn(objectResponse);
        Recover.restart();
        PowerMockito.verifyStatic(Mockito.times(1));
        Register.register();
    }

    @Test
    public void testRestartFail() throws Exception {
        PowerMockito.mockStatic(Register.class);
        PowerMockito.mockStatic(HeartThread.class);
        PowerMockito.mockStatic(Launcher.class);
        HttpClient mockHttpClient = PowerMockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(Mockito.anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.param(Mockito.anyString(), Mockito.anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(Mockito.anyInt())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.post(Mockito.any(Class.class))).thenReturn(null);
        Recover.restart();
        PowerMockito.verifyStatic(Mockito.times(0));
        Register.register();
    }

}
