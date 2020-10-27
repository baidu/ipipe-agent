package com.baidu.agile.agent.hook.cov;

import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.cov.ShellCovHook;
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

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CovHttpClientUtil.class})
public class ShellCovHookExecuteTest {

    @Mock
    private IJobLogger jobLogger;

    /**
     * initial
     */
    @Before
    public void setup() {
        PowerMockito.mockStatic(CovHttpClientUtil.class);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testTriggerHook4Response200() throws IOException {

        ShellCovHookExecute shellCovHookExecute = new ShellCovHookExecute();
        ShellCovHook shellCovHook = new ShellCovHook();
        shellCovHook.setCommitTime(new Date().getTime());
        shellCovHook.setRequestUrl("www.baidu.com");
        shellCovHook.setHookId(9999L);
        String uuid = UUID.randomUUID().toString();

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(200);
        response.setResult("{}");
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(shellCovHook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), shellCovHook.getHookId());

        response.setStatusCode(200);
        response.setResult("Asdf");
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenReturn(response);
        shellCovHookExecute.triggerHook(shellCovHook, uuid, "", jobLogger);
    }

    @Test
    public void testTriggerHook4Response400() throws IOException {
        ShellCovHookExecute shellCovHookExecute = new ShellCovHookExecute();
        ShellCovHook shellCovHook = new ShellCovHook();
        shellCovHook.setCommitTime(new Date().getTime());
        shellCovHook.setRequestUrl("www.baidu.com");
        String uuid = UUID.randomUUID().toString();

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(400);
        response.setResult("{\"error\":\"UNKOWN_ERROR\"}");
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse1 = shellCovHookExecute.triggerHook(shellCovHook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse1, null);

        response.setStatusCode(400);
        response.setResult("{\"error\"\"UNKOWN_ERROR\"}");
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(shellCovHook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse, null);
    }

    @Test
    public void testTriggerHook4Response500() throws IOException {
        ShellCovHookExecute shellCovHookExecute = new ShellCovHookExecute();
        ShellCovHook shellCovHook = new ShellCovHook();
        shellCovHook.setCommitTime(new Date().getTime());
        shellCovHook.setRequestUrl("www.baidu.com");
        String uuid = UUID.randomUUID().toString();

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(500);
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(shellCovHook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse, null);
    }

    @Test
    public void testTriggerHook4ResponseX() throws IOException {

        ShellCovHookExecute shellCovHookExecute = new ShellCovHookExecute();
        ShellCovHook shellCovHook = new ShellCovHook();
        shellCovHook.setCommitTime(new Date().getTime());
        shellCovHook.setRequestUrl("www.baidu.com");
        String uuid = UUID.randomUUID().toString();

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(300);
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(shellCovHook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse, null);
    }

    @Test
    public void testTriggerHook4ResponseException() throws IOException {

        ShellCovHookExecute shellCovHookExecute = new ShellCovHookExecute();
        ShellCovHook shellCovHook = new ShellCovHook();
        shellCovHook.setCommitTime(new Date().getTime());
        shellCovHook.setRequestUrl("www.baidu.com");
        String uuid = UUID.randomUUID().toString();

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(300);
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenThrow(new IOException());
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(shellCovHook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse, null);
    }

}
