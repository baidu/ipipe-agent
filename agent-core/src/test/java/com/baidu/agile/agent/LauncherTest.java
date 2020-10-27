package com.baidu.agile.agent;

import com.baidu.agile.agent.common.ftp.FtpDownloader;
import com.baidu.agile.agent.common.http.HttpDownloader;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.heart.HeartThread;
import com.baidu.agile.agent.log.SaAsyncLogger;
import com.baidu.agile.agent.os.PosixAPI;
import com.baidu.agile.agent.plugin.PluginManager;
import com.baidu.agile.agent.register.Register;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Launcher.class, FtpDownloader.class, HttpDownloader.class, File.class, FileUtils.class,
        Register.class, Thread.class, Runtime.class, Launcher.JarUpgrade.class, HeartThread.class,
        PluginManager.class, SaAsyncLogger.class, System.class, PosixAPI.class})
public class LauncherTest {

    private SystemCmdInfoFactory.AgentUpdateInfo updateInfo = new SystemCmdInfoFactory.AgentUpdateInfo();

    @Before
    public void before() throws Exception {
        PowerMockito.whenNew(SaAsyncLogger.class).withAnyArguments().thenReturn(Mockito.mock(SaAsyncLogger.class));
        Context.init(new String[] {}, new Args());

        Context.ServerContext serverContext = new Context.ServerContext();
        Context.setServerContext(serverContext);
        serverContext.setAgentUpdateInfo(updateInfo);
        updateInfo.setDownloadUrl("http://test");
        updateInfo.setUser("getprod");
        updateInfo.setPassword("getprod");

        PowerMockito.mockStatic(FtpDownloader.class);
        PowerMockito.mockStatic(HttpDownloader.class);
        PowerMockito.mockStatic(File.class);
        PowerMockito.mockStatic(FileUtils.class);
        PowerMockito.mockStatic(System.class);
        PowerMockito.mockStatic(PosixAPI.class);

        PowerMockito.mockStatic(Register.class);
        PowerMockito.mockStatic(HeartThread.class);
        PowerMockito.mockStatic(PluginManager.class);
        PowerMockito.mockStatic(Launcher.JarUpgrade.class);
    }

    @Test
    public void testStart() throws Exception {
        Launcher.INSTANCE.start();
        PowerMockito.verifyStatic();
        Runtime.getRuntime();
    }

    @Test
    public void testStartError() throws Exception {
        PowerMockito.doThrow(new Throwable()).when(Register.class);
        Launcher.INSTANCE.start();
        PowerMockito.verifyStatic();
        System.exit(Mockito.eq(1));
    }

    @Test
    public void testJarUpgradeByFtp() throws Exception {
        updateInfo.setDownloadUrl("ftp://test");
        FtpDownloader downloader = Mockito.spy(new FtpDownloader());
        PowerMockito.whenNew(FtpDownloader.class).withAnyArguments().thenReturn(downloader);
        Mockito.doNothing().when(downloader).download();
        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.renameTo(Mockito.any(File.class))).thenReturn(true);
        PowerMockito.mock(FileUtils.class, new DoesNothing());

        PowerMockito.when(PosixAPI.jnr()).thenThrow(Exception.class);

        Launcher.JarUpgrade upgrade = new Launcher.JarUpgrade(updateInfo);
        upgrade.setWaitMinute(0L);
        upgrade.upgrade();
        Mockito.verify(downloader).download();
    }

    @Test
    public void testJarUpgradeByHttp() throws Exception {
        updateInfo.setDownloadUrl("http://test");
        HttpDownloader downloader = Mockito.spy(new HttpDownloader());
        PowerMockito.whenNew(HttpDownloader.class).withAnyArguments().thenReturn(downloader);
        Mockito.doNothing().when(downloader).download();
        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.renameTo(Mockito.any(File.class))).thenReturn(true);
        PowerMockito.mock(FileUtils.class, new DoesNothing());
        Launcher.JarUpgrade upgrade = new Launcher.JarUpgrade(updateInfo);
        upgrade.setWaitMinute(0L);
        upgrade.upgrade();
        Mockito.verify(downloader).download();
    }

    @Test
    public void testKilledHook() throws Exception {
        PowerMockito.mockStatic(Register.class);
        Launcher.KILLED_HOOK_THREAD.run();
        PowerMockito.verifyStatic();
        Register.unRegister();
    }

    @Test
    public void testUpgradeHook() throws Exception {
        Context.getServerContext().setSecureKey("sk1");
        PowerMockito.mockStatic(Register.class);
        PowerMockito.mockStatic(Runtime.class);

        PowerMockito.whenNew(Launcher.JarUpgrade.class).withAnyArguments().thenReturn(Mockito.mock(
                Launcher.JarUpgrade.class));
        Launcher.UPGRADE_HOOK_THREAD.run();
        PowerMockito.verifyStatic();
        Runtime.getRuntime();

        PowerMockito.mockStatic(Launcher.class);
        PowerMockito.when(Launcher.isSuperviseRun()).thenReturn(true);
        Launcher.UPGRADE_HOOK_THREAD.run();
        PowerMockito.verifyStatic();
        Launcher.isSuperviseRun();
    }

    @Test
    public void testUpgrade() throws Exception {
        Context.getServerContext().setSecureKey("sk1");
        PowerMockito.mockStatic(Runtime.class);
        PowerMockito.mockStatic(System.class);

        Launcher.INSTANCE.upgrade();

        PowerMockito.verifyStatic();
        System.exit(0);
    }



    @Test
    public void testSetExecutable4Java() throws Exception {

        Context.getServerContext().setSecureKey("sk1");
        PowerMockito.mockStatic(Register.class);
        PowerMockito.mockStatic(Runtime.class);

        PowerMockito.whenNew(Launcher.JarUpgrade.class).withAnyArguments().thenReturn(Mockito.mock(
                Launcher.JarUpgrade.class));

        File mockFile = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(mockFile);

        Launcher.UPGRADE_HOOK_THREAD.run();
        Mockito.verify(mockFile, Mockito.times(1)).canExecute();

    }

    @Test
    public void testSetExecutable4JavaException() throws Exception {

        Context.getServerContext().setSecureKey("sk1");
        PowerMockito.mockStatic(Register.class);
        PowerMockito.mockStatic(Runtime.class);

        PowerMockito.whenNew(Launcher.JarUpgrade.class).withAnyArguments().thenReturn(Mockito.mock(
                Launcher.JarUpgrade.class));

        File mockFile = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(mockFile);
        PowerMockito.when(mockFile.canExecute()).thenThrow(new RuntimeException());

        Launcher.UPGRADE_HOOK_THREAD.run();
        Mockito.verify(mockFile, Mockito.times(1)).canExecute();
    }

    @Test
    public void testUpgrade4Exception() throws Exception {
        Context.getServerContext().setSecureKey("sk1");

        PowerMockito.mockStatic(Launcher.class);
        PowerMockito.whenNew(Launcher.JarUpgrade.class).withAnyArguments().thenReturn(Mockito.mock(
                Launcher.JarUpgrade.class));
        PowerMockito.when(Launcher.class, "execUpgradeCmd", Mockito.anyObject()).thenThrow(new RuntimeException());

        Launcher.UPGRADE_HOOK_THREAD.run();
        PowerMockito.verifyStatic();
        Runtime.getRuntime();
    }

    @Test
    public void testRestart() throws Exception {
        Context.ServerContext serverContext = new Context.ServerContext();
        serverContext.setSecureKey("sk");
        Context.setServerContext(serverContext);
        Launcher.INSTANCE.unregisterAndStop();
        PowerMockito.verifyStatic(Mockito.times(1));
        Register.unRegister();
    }


}
