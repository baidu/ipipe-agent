package com.baidu.agile.agent.upgrade;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.common.http.HttpDownloader;
import com.baidu.agile.agent.context.Context;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Launcher.class, UpgraderExecutor.class, Runtime.class, Watcher.class})
public class UpgraderExecutorTest {

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        UpgraderTest.init();
    }

    @Test
    public void testPrepareSuccess() throws Exception {
        HttpDownloader downloader = Mockito.spy(new HttpDownloader());
        PowerMockito.whenNew(HttpDownloader.class).withAnyArguments().thenReturn(downloader);
        Mockito.doNothing().when(downloader).download();
        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.canExecute()).thenReturn(false);
        Mockito.when(file.renameTo(Mockito.any(File.class))).thenReturn(true);
        PowerMockito.mock(FileUtils.class, new DoesNothing());
        boolean result = UpgraderExecutor.prepare(Context.getServerContext().getAgentUpdateInfo());
        Assert.assertTrue(result);
    }

    @Test
    public void testPrepareFail4JavaNotExecutable() throws Exception {
        HttpDownloader downloader = Mockito.spy(new HttpDownloader());
        PowerMockito.whenNew(HttpDownloader.class).withAnyArguments().thenReturn(downloader);
        Mockito.doNothing().when(downloader).download();
        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.canExecute()).thenThrow(new RuntimeException());
        Mockito.when(file.renameTo(Mockito.any(File.class))).thenReturn(true);
        PowerMockito.mock(FileUtils.class, new DoesNothing());
        boolean result = UpgraderExecutor.prepare(Context.getServerContext().getAgentUpdateInfo());
        Assert.assertFalse(result);

    }

    @Test
    public void testGetCmds() throws Exception {
        String[] cmds = UpgraderExecutor.getCmds();
        Assert.assertTrue(ArrayUtils.isNotEmpty(cmds));

    }

    @Test
    public void testExecuteUpgradeSuccess() throws Exception {
        PowerMockito.mockStatic(Watcher.class);
        String[] args = {"echo hello"};
        Process process = PowerMockito.mock(Process.class);
        PowerMockito.mockStatic(Runtime.class);
        PowerMockito.when(Runtime.getRuntime().exec(args)).thenReturn(process);
        UpgradeStatus upgradeStatus = new UpgradeStatus();
        PowerMockito.when(Watcher.watch(process)).thenReturn(upgradeStatus);
        UpgradeStatus result = UpgraderExecutor.executeUpgrade(args);
        Assert.assertEquals(upgradeStatus, result);
    }

    @Test
    public void testExecuteUpgradeSuccess4Supervise() throws Exception {
        PowerMockito.mockStatic(Launcher.class);
        String[] args = {"echo hello"};
        Process process = PowerMockito.mock(Process.class);
        PowerMockito.when(Launcher.isSuperviseRun()).thenReturn(true);
        UpgradeStatus result = UpgraderExecutor.executeUpgrade(args);
        Assert.assertTrue(result.isOnline());
        Assert.assertTrue(result.isNewestVersion());
    }

    @Test
    public void testExecuteUpgradeFail() throws Exception {
        PowerMockito.mockStatic(Watcher.class);
        String[] args = {"echo hello"};
        Process process = PowerMockito.mock(Process.class);
        PowerMockito.mockStatic(Runtime.class);
        PowerMockito.when(Runtime.getRuntime().exec(args)).thenReturn(process);
        UpgradeStatus upgradeStatus = new UpgradeStatus();
        PowerMockito.when(Watcher.watch(process)).thenThrow(new RuntimeException());
        UpgradeStatus result = UpgraderExecutor.executeUpgrade(args);
        Assert.assertEquals(result, null);
    }

}
