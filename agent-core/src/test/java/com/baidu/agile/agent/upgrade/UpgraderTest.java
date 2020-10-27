package com.baidu.agile.agent.upgrade;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.baidu.agile.agent.Args;
import com.baidu.agile.agent.Launcher;
import com.baidu.agile.agent.context.Context;
import com.baidu.agile.server.agent.bean.SystemCmdInfoFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Launcher.class, UpgraderExecutor.class, System.class, Recover.class})
public class UpgraderTest {

    @InjectMocks
    private Upgrader upgrader;

    @Mock
    private UpgraderExecutor upgraderExecutor;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        init();
    }

    @Test
    public void testExecute4UpgradeFail() throws Exception {
        PowerMockito.mockStatic(Launcher.class);
        PowerMockito.mockStatic(UpgraderExecutor.class);
        PowerMockito.mockStatic(Recover.class);
        Upgrader.execute();
        PowerMockito.verifyStatic(Mockito.times(1));
        Recover.restart();
    }

    @Test
    public void testExecute4NotNewestVersion() throws Exception {
        PowerMockito.mockStatic(Launcher.class);
        PowerMockito.mockStatic(UpgraderExecutor.class);
        PowerMockito.mockStatic(Recover.class);
        PowerMockito.when(UpgraderExecutor.prepare(Mockito.any(SystemCmdInfoFactory.AgentUpdateInfo.class)))
                .thenReturn(true);
        UpgradeStatus upgradeStatus = new UpgradeStatus(true, false);
        PowerMockito.when(UpgraderExecutor.executeUpgrade(Mockito.any(String[].class))).thenReturn(upgradeStatus);
        Upgrader.execute();
        PowerMockito.verifyStatic(Mockito.times(0));
        Runtime.getRuntime();

    }

    @Test
    public void testExecute4TryUpgradeException() throws Exception {
        PowerMockito.mockStatic(Launcher.class);
        PowerMockito.mockStatic(UpgraderExecutor.class);
        PowerMockito.mockStatic(Recover.class);
        PowerMockito.when(UpgraderExecutor.prepare(Mockito.any(SystemCmdInfoFactory.AgentUpdateInfo.class)))
                .thenThrow(new RuntimeException());
        Upgrader.execute();
        PowerMockito.verifyStatic(Mockito.times(1));
        Recover.restart();
    }

    @Test
    public void testExecute4UpgradeError() throws Exception {
        PowerMockito.mockStatic(Launcher.class);
        PowerMockito.mockStatic(UpgraderExecutor.class);
        PowerMockito.mockStatic(Recover.class);
        PowerMockito.when(UpgraderExecutor.prepare(Mockito.any(SystemCmdInfoFactory.AgentUpdateInfo.class)))
                .thenReturn(true);
        UpgradeStatus upgradeStatus = PowerMockito.mock(UpgradeStatus.class);
        PowerMockito.when(UpgraderExecutor.executeUpgrade(Mockito.any(String[].class))).thenReturn(upgradeStatus);
        PowerMockito.when(upgradeStatus.isOnline()).thenReturn(true);
        PowerMockito.when(upgradeStatus.isNewestVersion()).thenThrow(new RuntimeException());
        Upgrader.execute();
    }

    public static void init() {
        Context.ServerContext serverContext = new Context.ServerContext();
        //        serverContext.setSecureKey("sk");
        SystemCmdInfoFactory.AgentUpdateInfo agentUpdateInfo = new SystemCmdInfoFactory.AgentUpdateInfo();
        agentUpdateInfo.setDownloadUrl("http://test");
        serverContext.setAgentUpdateInfo(agentUpdateInfo);
        Context.setServerContext(serverContext);
        Args agentArgs = new Args();
        agentArgs.setServer("test-server");
        String[] args = new String[1];
        Context.init(args, agentArgs);
    }

}
