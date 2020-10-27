package com.baidu.agile.agent.plugin;

import com.baidu.agile.agent.log.IJobMultiAsyncLogger;
import com.baidu.agile.server.job.bean.JobBaseParameter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

public class AgentProxyTest {

    @InjectMocks
    private AgentProxy agentProxy = new AgentProxy("uuid", Mockito.mock(JobBaseParameter.class));

    @Mock
    private IJobMultiAsyncLogger jobMultiAsyncLogger;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void log() {
        agentProxy.log("uuid", "data");
        Mockito.verify(jobMultiAsyncLogger).log(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void startLogger() {
        agentProxy.startLogger();
        Mockito.verify(jobMultiAsyncLogger).start();

    }

    @Test
    public void stopLogger() {
        agentProxy.stopLogger();
        Mockito.verify(jobMultiAsyncLogger).end();
    }
}