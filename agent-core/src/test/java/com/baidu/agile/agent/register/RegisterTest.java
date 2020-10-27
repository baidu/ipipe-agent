package com.baidu.agile.agent.register;

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.AgentStatus;
import com.baidu.agile.agent.Args;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.Agent;
import com.baidu.agile.server.agent.bean.AgentResponse;
import com.baidu.agile.server.common.ObjectResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Register.class, AgentStatus.class})
public class RegisterTest {

    private HttpClient httpClient = Mockito.spy(new HttpClient(""));

    @Before
    public void before() throws Exception {
        Context.init(null, new Args());
        PowerMockito.mockStatic(AgentStatus.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(httpClient);
    }

    @Test(expected = AgentException.class)
    public void testRegisterException() throws Exception {
        Mockito.when(httpClient.post(Mockito.any(Class.class))).thenReturn(null);
        Register.register();
    }

    @Test
    public void testRegister() throws Exception {
        ObjectResponse<AgentResponse> response = new ObjectResponse<AgentResponse>();
        response.setStatus(ObjectResponse.STATUS_OK);
        AgentResponse agentResponse = new AgentResponse();
        agentResponse.setAgent(new Agent());
        response.setData(agentResponse);
        Mockito.when(httpClient.post(Mockito.any(Class.class))).thenReturn(response);
        Register.register();
        PowerMockito.verifyStatic();
        Context.labels();
    }

}
