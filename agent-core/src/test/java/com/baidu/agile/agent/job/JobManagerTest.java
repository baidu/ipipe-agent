package com.baidu.agile.agent.job;

import com.baidu.agile.agent.Args;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.AgentResponse;
import com.baidu.agile.server.agent.bean.job.AgentJob;
import com.baidu.agile.server.common.ObjectResponse;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {JobManager.class})
public class JobManagerTest {

    @Test
    public void testNotice4NotExistJob() throws Exception {

        Context.ServerContext serverContext = new Context.ServerContext();
        serverContext.setSecureKey("sk");
        Context.setServerContext(serverContext);

        Args args = new Args();
        args.setServer("testserver");
        Field agentArgsField = Context.class.getDeclaredField("agentArgs");
        agentArgsField.setAccessible(true);
        agentArgsField.set(null, args);

        ObjectResponse<AgentResponse> response = new ObjectResponse<AgentResponse>();
        response.setStatus(101);
        response.setMessage("no msg");

        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        PowerMockito.whenNew(HttpClient.class).withAnyArguments().thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.path(anyString())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.timeout(anyInt())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.retry(anyInt())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.jsonBody(anyMap())).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.post(Mockito.any(Class.class))).thenReturn(null);
        JobManager.notice4NotExistJob("asdf");

        PowerMockito.when(mockHttpClient.post(Mockito.any(Class.class))).thenReturn(response);
        JobManager.notice4NotExistJob("asdf");

        Mockito.verify(mockHttpClient, Mockito.times(2))
                .post(Mockito.any(Class.class));
    }

    @Test
    public void testNotice4NotExistJobException() {
        JobManager.notice4NotExistJob("asdf");
        HttpClient mockHttpClient = Mockito.mock(HttpClient.class);
        Mockito.verify(mockHttpClient, Mockito.times(0))
                .post(Mockito.any(Class.class));
    }

    @Test
    public void testKillJob() {
        Job job = Mockito.spy(new ShellJob());
        AgentJob<ShellJobParameter> agentJob = new AgentJob<ShellJobParameter>();
        agentJob.setJobUuid("u1");
        job.setAgentJob(agentJob);
        JobManager.registerJob(job);
        Mockito.doNothing().when(job).stop(Mockito.anyString());
        JobManager.killJob("u1");
        Mockito.verify(job).stop(Mockito.anyString());
    }

}
