package com.baidu.agile.agent;

import com.baidu.agile.agent.common.ftp.FtpDownloader;
import com.baidu.agile.agent.common.http.HttpDownloader;
import com.baidu.agile.agent.heart.HeartThread;
import com.baidu.agile.agent.log.SaAsyncLogger;
import com.baidu.agile.agent.os.PosixAPI;
import com.baidu.agile.agent.plugin.PluginManager;
import com.baidu.agile.agent.register.Register;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Launcher.class, FtpDownloader.class, HttpDownloader.class, File.class, FileUtils.class,
        Register.class, Thread.class, Runtime.class, Launcher.JarUpgrade.class, HeartThread.class,
        PluginManager.class, SaAsyncLogger.class, System.class, PosixAPI.class})
public class AgentEventTest {

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(System.class);
    }

    @Test
    public void testRejectEvent() throws Exception {
        AgentEvent.REJECT.process();
        PowerMockito.verifyStatic();
        System.exit(Mockito.anyInt());
    }

}
