package com.baidu.agile.agent.context;

import com.baidu.agile.agent.Args;
import com.baidu.agile.agent.log.SaAsyncLogger;
import com.baidu.agile.server.agent.bean.Agent;
import com.baidu.agile.server.agent.bean.AgentResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class})
public class ContextTest {

    private SaAsyncLogger logger = Mockito.mock(SaAsyncLogger.class);

    @Before
    public void before() throws Exception {
        PowerMockito.whenNew(SaAsyncLogger.class).withAnyArguments().thenReturn(logger);
        Context.init(null, new Args());
    }

    @Test
    public void testLabels() throws Exception {
        Assert.assertEquals(Context.VERSION, Context.labels().get("agentVersion"));
    }

    @Test
    public void testLog() throws Exception {
        Context.log("", new RuntimeException());
        Mockito.verify(logger).log(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testFlush() throws Exception {
        Context.flush();
        Mockito.verify(logger).flush();
    }

    @Test
    public void testSetResponse() throws Exception {
        AgentResponse response = new AgentResponse();
        response.setAgent(new Agent());
        Context.setContext(response);
        Assert.assertNotNull(Context.getServerContext());
    }
}