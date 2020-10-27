package com.baidu.agile.agent.job;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.job.bean.JobStatus;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobManager.class})
public class JobTest {

    /**
     * initial
     */
    @Before
    public void setup() {
        PowerMockito.mockStatic(JobManager.class);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testEnd() {

        ShellJob shellJob = new ShellJob();
        AgentJob agentJob = new AgentJob();
        agentJob.setJobUuid("99999999999999999");
        shellJob.setAgentJob(agentJob);
        shellJob.end(JobStatus.DISPATCHED);
    }
}
