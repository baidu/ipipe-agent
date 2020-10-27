package com.baidu.agile.agent.run;

import com.baidu.agile.agent.AgentStatus;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.job.bean.concrete.KillAgentParameter;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobScheduler.class, Thread.class, Context.class, AgentStatus.class})
public class JobSchedulerTest {

    @InjectMocks
    private JobScheduler jobScheduler;

    @Mock
    private JobGetter jobGetter;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Context.class);
    }

    @Test
    public void testSchedule() throws Exception {
        Mockito.doThrow(Exception.class).when(jobGetter).getJobs();
        PowerMockito.mockStatic(Thread.class);
        jobScheduler.schedule();
        PowerMockito.verifyStatic();
        Thread.sleep(Mockito.anyLong());
    }

    @Test
    public void testKillAgent() throws Exception {
        jobScheduler = PowerMockito.spy(jobScheduler);
        List<AgentJob<?>> jobs = new ArrayList<AgentJob<?>>();
        KillAgentParameter killAgentParameter = new KillAgentParameter();
        killAgentParameter.setTargetAgentUuid("testUuid");
        AgentJob<KillAgentParameter> job = new AgentJob<KillAgentParameter>();
        job.setJobParameter(killAgentParameter);
        jobs.add(job);
        PowerMockito.doReturn(true).when(jobScheduler, "confirm", jobs);
        Mockito.when(jobGetter.getJobs()).thenReturn(jobs);
        PowerMockito.mockStatic(AgentStatus.class);
        jobScheduler.schedule();
    }

    @Test
    public void testShellJob() throws Exception {
        jobScheduler = PowerMockito.spy(jobScheduler);
        List<AgentJob<?>> jobs = new ArrayList<AgentJob<?>>();
        ShellJobParameter shellJobParameter = new ShellJobParameter();
        AgentJob<ShellJobParameter> job = new AgentJob<ShellJobParameter>();
        job.setJobParameter(shellJobParameter);
        jobs.add(job);
        jobs.add(new AgentJob());
        PowerMockito.doReturn(true).when(jobScheduler, "confirm", jobs);
        Mockito.when(jobGetter.getJobs()).thenReturn(jobs);
        PowerMockito.mockStatic(AgentStatus.class);
        jobScheduler.schedule();
    }

}
