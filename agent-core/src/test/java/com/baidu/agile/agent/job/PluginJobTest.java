package com.baidu.agile.agent.job;

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.job.bean.JobBaseParameter;
import com.baidu.agile.server.job.bean.JobStatus;
import com.baidu.agile.server.job.bean.concrete.ShellJobOutput;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;
import com.baidu.agile.server.utils.PluginClasspathLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URLClassLoader;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PluginJob.class, PluginClasspathLoader.class})
public class PluginJobTest {

    @Test
    public void testResult() throws Exception {
        Context.setServerContext(new Context.ServerContext());
        PluginJob job = new PluginJob();
        AgentJob agentJob = new AgentJob<JobBaseParameter>();
        agentJob.setJobUuid("j1");
        agentJob.setJobParameter(new ShellJobParameter());
        job.setAgentJob(agentJob);

        URLClassLoader classLoader = PowerMockito.mock(URLClassLoader.class);
        PowerMockito.mockStatic(PluginClasspathLoader.class);
        PowerMockito.when(PluginClasspathLoader.getClassLoader()).thenReturn(classLoader);
        PowerMockito.doReturn(ShellJobOutput.class).when(classLoader).loadClass(Mockito.anyString());

        job.end(JobStatus.SUCC);

        Mockito.verify(classLoader).loadClass(Mockito.anyString());
    }

}
