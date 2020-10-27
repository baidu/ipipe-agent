package com.baidu.agile.agent.hook;

import com.baidu.agile.agent.log.IJobLogger;
import com.baidu.agile.server.job.bean.hook.ShellHookResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class HookExecuteFactoryTest {

    @Mock
    private IJobLogger jobLogger;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecuteNull() throws Exception {
        ShellHookResponse shellHookResponse = HookExecuteFactory.execute(null, null, null, jobLogger);
        Assert.assertNull(shellHookResponse);
    }
}
