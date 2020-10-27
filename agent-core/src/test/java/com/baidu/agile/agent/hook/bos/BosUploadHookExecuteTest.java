package com.baidu.agile.agent.hook.bos;

import com.baidu.agile.agent.hook.HookExecuteFactory;
import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import com.baidu.agile.server.job.bean.hook.bos.BosUploadHook;
import com.baidu.agile.server.job.bean.hook.bos.BosUploadHookResponse;
import com.baidubce.services.bos.BosClient;
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

import java.io.File;
import java.util.UUID;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BosUploadHookExecute.class, BosClient.class, File.class})
public class BosUploadHookExecuteTest {
    @Mock
    private IJobLogger jobLogger;

    private BosUploadHook hook;

    @Mock
    private BosClient bosClient;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        hook = new BosUploadHook();
        hook.setHookId(1L);
        hook.setBucketName("b1");
        hook.setLocalFile("f1");
        hook.setObjectKey("k1");
        hook.setAccessKeyId("k1");
        hook.setAllowFail(false);
        hook.setEndPoint("");
        hook.setSecretAccessKey("s1");
        hook.setSessionToken("t1");

        PowerMockito.mockStatic(BosClient.class);
        PowerMockito.mockStatic(File.class);
    }

    @Test
    public void testTriggerHookWithErrorFile() throws Exception {
        hook.setEndPoint("http://bj");
        String uuid = UUID.randomUUID().toString();

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(false);
        Mockito.when(file.isFile()).thenReturn(false);

        PowerMockito.whenNew(BosClient.class).withAnyArguments().thenReturn(bosClient);

        ShellHookResponse shellHookResponse = HookExecuteFactory.execute(hook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), hook.getHookId());
        Assert.assertFalse(((BosUploadHookResponse) shellHookResponse).isSuccess());
    }

    @Test
    public void testTriggerHookWithException() throws Exception {
        String uuid = UUID.randomUUID().toString();

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.isFile()).thenReturn(true);

        PowerMockito.whenNew(BosClient.class).withAnyArguments().thenReturn(bosClient);
        Mockito.doThrow(Exception.class).when(bosClient).putObject(
                Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class));

        hook.setAllowFail(true);
        ShellHookResponse shellHookResponse = HookExecuteFactory.execute(hook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), hook.getHookId());
        Assert.assertFalse(((BosUploadHookResponse) shellHookResponse).isSuccess());
    }

    @Test
    public void testTriggerHookSuccess() throws Exception {
        String uuid = UUID.randomUUID().toString();

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.isAbsolute()).thenReturn(true);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.isFile()).thenReturn(true);
        Mockito.when(file.length()).thenReturn(1L);

        PowerMockito.whenNew(BosClient.class).withAnyArguments().thenReturn(bosClient);

        ShellHookResponse shellHookResponse = HookExecuteFactory.execute(hook, uuid, "", jobLogger);
        Assert.assertEquals(shellHookResponse.getHookId(), hook.getHookId());
        Assert.assertTrue(((BosUploadHookResponse) shellHookResponse).isSuccess());
        Assert.assertEquals(1, ((BosUploadHookResponse) shellHookResponse).getFileSizeBytes());
    }

}
