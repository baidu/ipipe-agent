package com.baidu.agile.agent.hook.cov;

import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.agent.run.artifact.FileCompress;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.cov.ShellCoverageHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellCoverageHookResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ShellCoverageHookExecute.class, CovHttpClientUtil.class, File.class, FileCompress.class})
public class ShellCoverageHookExecuteTest {
    @Mock
    private IJobLogger jobLogger;

    @Before
    public void before() {
        PowerMockito.mockStatic(CovHttpClientUtil.class);
        PowerMockito.mockStatic(File.class);
        PowerMockito.mockStatic(FileCompress.class);
    }

    @Test
    public void testTriggerHookException() throws Exception {
        ShellCoverageHookExecute shellCovHookExecute = new ShellCoverageHookExecute();
        ShellCoverageHook hook = new ShellCoverageHook();
        hook.setParams(new HashMap<String, String>());
        hook.setCovFile("cov.xml");
        hook.setCommitTimeSecond("123");
        hook.setRequestUrl("www.baidu.com");
        hook.setHookId(1L);
        String uuid = UUID.randomUUID().toString();

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);

        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenThrow(Exception.class);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(hook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), hook.getHookId());
        Assert.assertFalse(((ShellCoverageHookResponse) shellHookResponse).isSuccess());
    }

    @Test
    public void testTriggerHook4Response200() throws Exception {
        ShellCoverageHookExecute shellCovHookExecute = new ShellCoverageHookExecute();
        ShellCoverageHook hook = new ShellCoverageHook();
        hook.setCovFile("cov.xml");
        hook.setReportDir("r1/");
        hook.setCommitTimeSecond("123");
        hook.setRequestUrl("www.baidu.com");
        hook.setHookId(1L);
        String uuid = UUID.randomUUID().toString();

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.isDirectory()).thenReturn(true);

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(200);
        response.setResult("{}");
        Mockito.when(CovHttpClientUtil.postWithFileBody(Mockito.anyString(), Mockito.any(Map.class),
                Mockito.any(Map.class), Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(hook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), hook.getHookId());
        Assert.assertTrue(((ShellCoverageHookResponse) shellHookResponse).isSuccess());
    }

    @Test
    public void testTriggerHook4Response200WithErrorCompress() throws Exception {
        ShellCoverageHookExecute shellCovHookExecute = new ShellCoverageHookExecute();
        ShellCoverageHook hook = new ShellCoverageHook();
        hook.setCovFile("cov.xml");
        hook.setReportDir("r1/");
        hook.setCommitTimeSecond("123");
        hook.setRequestUrl("www.baidu.com");
        hook.setHookId(1L);
        String uuid = UUID.randomUUID().toString();

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.isDirectory()).thenReturn(true);
        PowerMockito.when(FileCompress.class, "compress", Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
                .thenThrow(Exception.class);

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(200);
        response.setResult("{}");
        Mockito.when(CovHttpClientUtil.postWithFileBody(Mockito.anyString(), Mockito.any(Map.class),
                Mockito.any(Map.class), Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(hook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), hook.getHookId());
        Assert.assertTrue(((ShellCoverageHookResponse) shellHookResponse).isSuccess());
    }

    @Test
    public void testTriggerHook4Response500() throws Exception {
        ShellCoverageHookExecute shellCovHookExecute = new ShellCoverageHookExecute();
        ShellCoverageHook hook = new ShellCoverageHook();
        hook.setCovFile("cov.xml");
        hook.setCommitTimeSecond("123");
        hook.setRequestUrl("www.baidu.com");
        hook.setHookId(1L);
        String uuid = UUID.randomUUID().toString();

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);

        CovResponseResult response = new CovResponseResult();
        response.setStatusCode(500);
        Mockito.when(CovHttpClientUtil.post(Mockito.anyString(), Mockito.any(Map.class), Mockito.any(Map.class),
                Mockito.anyInt())).thenReturn(response);
        ShellHookResponse shellHookResponse = shellCovHookExecute.triggerHook(hook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), hook.getHookId());
        Assert.assertFalse(((ShellCoverageHookResponse) shellHookResponse).isSuccess());
    }

    @Test
    public void testTriggerHook4Null() throws Exception {
        ShellCoverageHookExecute shellCovHookExecute = new ShellCoverageHookExecute();
        Assert.assertNull(shellCovHookExecute.triggerHook(null, "u1", "", jobLogger));
    }
}
