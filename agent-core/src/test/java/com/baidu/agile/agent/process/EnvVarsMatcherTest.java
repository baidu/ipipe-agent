package com.baidu.agile.agent.process;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class EnvVarsMatcherTest {

    private EnvVarsMatcher matcher;

    @Before
    public void before() {
        Map<String, String> envVars = new HashMap<String, String>();
        envVars.put("a", "1");
        envVars.put("b", "2");
        matcher = new EnvVarsMatcher(envVars);
    }

    @Test
    public void testMatchTrue() throws Exception {
        Map<String, String> envVars = new HashMap<String, String>();
        envVars.put("a", "1");
        envVars.put("b", "2");
        envVars.put("c", "3");

        Process process = Mockito.mock(Process.class);
        Mockito.doReturn(envVars).when(process).getEnvVars();

        Assert.assertTrue(matcher.match(process));
    }

    @Test
    public void testMatchFalse() throws Exception {
        Map<String, String> envVars = new HashMap<String, String>();
        envVars.put("a", "1");

        Process process = Mockito.mock(Process.class);
        Mockito.doReturn(envVars).when(process).getEnvVars();

        Assert.assertFalse(matcher.match(process));
    }
}
