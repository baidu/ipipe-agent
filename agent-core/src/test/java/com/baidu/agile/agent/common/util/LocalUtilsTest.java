package com.baidu.agile.agent.common.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LocalUtils.class})
public class LocalUtilsTest {

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetSystemEnv() throws Exception {
        Assert.assertTrue(LocalUtils.getSystemEnv().size() > 0);
    }

    @Test
    public void testGetSystemProps() throws Exception {
        Assert.assertTrue(LocalUtils.getSystemProps().get("user.dir").length() > 0);
    }

    @Test
    public void testGetHostIp() throws Exception {
        Assert.assertNotNull(LocalUtils.getHostIp());
    }

    @Test
    public void testGetHostIpException() throws Exception {
        PowerMockito.mockStatic(InetAddress.class);
        PowerMockito.when(InetAddress.getLocalHost()).thenThrow(UnknownHostException.class);
        Assert.assertEquals(new UnknownHostException().getMessage(), LocalUtils.getHostIp());
    }

    @Test
    public void testGetHostName() throws Exception {
        Assert.assertNotNull(LocalUtils.getHostName());
    }

    @Test
    public void testGetHostNameException() throws Exception {
        PowerMockito.mockStatic(InetAddress.class);
        PowerMockito.when(InetAddress.getLocalHost()).thenThrow(UnknownHostException.class);
        Assert.assertEquals(new UnknownHostException().getMessage(), LocalUtils.getHostName());
    }

}