package com.baidu.agile.agent.job;

import com.baidu.agile.agent.Args;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.execute.ShellProcessBuilder;
import com.baidu.agile.agent.process.ProcessTree;
import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.job.bean.JobStatus;
import com.baidu.agile.server.job.bean.concrete.ScriptType;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ShellJob.class, ProcessTree.class, Context.class})
public class ShellJobTest {
    @InjectMocks
    @Spy
    ShellJob shellJob = new ShellJob();

    @Spy
    private AgentJob agentJob = new AgentJob();

    @Mock
    private Process process;

    @Spy
    private ShellProcessBuilder shellProcessBuilder =
            new ShellProcessBuilder("", ScriptType.SH, "", null, null, null);

    @Before
    public void before() {
        agentJob.setJobParameter(new ShellJobParameter());
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(ProcessTree.class);
        PowerMockito.mockStatic(Context.class);
    }

    @Test
    public void testStart() throws Exception {
        PowerMockito.when(Context.getServerContext()).thenReturn(new Context.ServerContext());
        PowerMockito.when(Context.getAgentArgs()).thenReturn(new Args());

        HttpClient httpClient = Mockito.spy(new HttpClient(""));
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
        Mockito.when(httpClient.post(Mockito.any(Class.class))).thenReturn(null);

        PowerMockito.whenNew(ShellProcessBuilder.class).withAnyArguments().thenReturn(shellProcessBuilder);
        Mockito.doReturn(process).when(shellProcessBuilder).start();

        shellJob.start();
        Mockito.verify(shellProcessBuilder).start();
    }

    @Test
    public void testStop() {
        Mockito.doNothing().when(shellJob).end(Mockito.any(JobStatus.class));
        ProcessTree processTree = Mockito.mock(ProcessTree.class);
        PowerMockito.when(ProcessTree.getTree()).thenReturn(processTree);

        PowerMockito.when(Context.getAgentArgs()).thenReturn(new Args());

        shellJob.stop();
        Mockito.verify(process).destroy();
    }

    @Test
    public void testWs() {
        Assert.assertEquals("/b", shellJob.ws("u", ShellJobParameter.WsDirType.FIXED, "/a", "/b"));
        Assert.assertEquals("/a/u", shellJob.ws("u", ShellJobParameter.WsDirType.JOB_UUID, "/a", "/b"));
    }
}
