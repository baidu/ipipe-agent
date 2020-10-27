package com.baidu.agile.agent.execute;

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.job.JobManager;
import com.baidu.agile.agent.log.IJobAsyncLogger;
import com.baidu.agile.agent.os.OS;
import com.baidu.agile.agent.process.ProcessTree;
import com.baidu.agile.agent.run.artifact.JobArtifact;
import com.baidu.agile.agent.wrapper.ShellWrapperUtil;
import com.baidu.agile.server.job.bean.JobStatus;
import com.baidu.agile.server.job.bean.artifact.Artifact;
import com.baidu.agile.server.job.bean.artifact.PatternType;
import com.baidu.agile.server.job.bean.concrete.ScriptType;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;
import com.baidu.agile.server.job.bean.hook.ShellHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellCoverageHook;
import com.baidu.agile.server.job.bean.hook.cov.ShellInvalidateGitSshHook;
import com.baidu.agile.server.job.bean.report.Report;
import com.baidu.agile.server.job.bean.report.ReportType;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {FileUtils.class, ShellProcessBuilder.class, ShellWrapperUtil.class, JobManager.class,
        JobArtifact.class, OS.class, ProcessTree.class})
public class ShellProcessBuilderTest {

    @InjectMocks
    private ShellProcessBuilder shellProcessBuilder = Mockito.spy(new ShellProcessBuilder("uuid", null,
            "command", new HashMap<String, String>(), "", null));
    @Mock
    private IJobAsyncLogger jobAsyncLogger;
    private ProcessBuilder processBuilder = PowerMockito.mock(ProcessBuilder.class);
    private Process process = PowerMockito.mock(Process.class);

    @Before
    public void setup() throws Exception {
        Context.ServerContext serverContext = new Context.ServerContext();
        Context.setServerContext(serverContext);

        PowerMockito.whenNew(ProcessBuilder.class).withNoArguments().thenReturn(processBuilder);
        PowerMockito.mockStatic(FileUtils.class);
        PowerMockito.mockStatic(ShellWrapperUtil.class);
        PowerMockito.mockStatic(JobManager.class);
    }

    @Test
    public void testStartSuccessRetainWorkspaceConditionAll() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(0);
        shellProcessBuilder.setRetainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition.ALL);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(2));
    }

    @Test
    public void testStartFailRetainWorkspaceConditionAll() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(1);
        shellProcessBuilder.setRetainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition.JOB_FAIL);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(2));
    }

    @Test
    public void testStartSuccessRetainWorkspaceConditionNull() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(0);
        shellProcessBuilder.setRetainWorkspaceCondition(null);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(4));
    }

    @Test
    public void testStartFailRetainWorkspaceConditionNull() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(1);
        shellProcessBuilder.setRetainWorkspaceCondition(null);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(4));
    }

    @Test
    public void testStartSuccessRetainWorkspaceConditionJobFail() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(0);
        shellProcessBuilder.setRetainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition.JOB_FAIL);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(4));
    }

    @Test
    public void testStartFailRetainRetainWorkspaceConditionJobFail() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(1);
        shellProcessBuilder.setRetainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition.JOB_FAIL);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(2));
    }

    @Test
    public void testReportFail() throws Exception {
        prepareForReportTest(false, 0);

        final Object lock = new Object();

        PowerMockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                synchronized (lock) {
                    lock.notifyAll();
                }
                return null;
            }
        }).when(JobManager.class);
        JobManager.endJob(Mockito.anyString(), Mockito.eq(JobStatus.FAILED));

        shellProcessBuilder.start();
        synchronized (lock) {
            lock.wait(1000);
        }

        PowerMockito.verifyStatic();
        JobManager.endJob(Mockito.anyString(), Mockito.eq(JobStatus.FAILED));
    }

    @Test
    public void testReportForceSucc() throws Exception {
        prepareForReportTest(true, 0);

        PowerMockito.mockStatic(JobManager.class);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        PowerMockito.verifyStatic();
        JobManager.endJob(Mockito.anyString(), Mockito.eq(JobStatus.SUCC));
    }

    @Test
    public void testReportSucc() throws Exception {
        prepareForReportTest(false, 1);

        PowerMockito.mockStatic(JobManager.class);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        PowerMockito.verifyStatic();
        JobManager.endJob(Mockito.anyString(), Mockito.eq(JobStatus.SUCC));
    }

    private void prepareForReportTest(Boolean allowMissing, int filesCount) throws Exception {
        List<Report> reports = new ArrayList<Report>();
        Report report = new Report();
        report.setReportType(ReportType.HTML);
        report.setReportName("r1");
        Artifact artifact = new Artifact();
        artifact.setPatternType(PatternType.ANT);
        artifact.setFilePattern("not.file");
        report.setArtifact(artifact);
        report.setAllowMissing(allowMissing);
        reports.add(report);
        shellProcessBuilder.reports(reports);

        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(0);
        Mockito.doNothing().when(shellProcessBuilder).reportStart(Mockito.anyString());
        JobArtifact jobArtifact = Mockito.spy(new JobArtifact("uuid"));
        PowerMockito.whenNew(JobArtifact.class).withAnyArguments().thenReturn(jobArtifact);
        Mockito.doReturn(filesCount).when(jobArtifact).upload();
        Mockito.doNothing().when(shellProcessBuilder).reportFinish(
                Mockito.anyString(), Mockito.anyString(), Mockito.any(ReportType.class));
    }

    @Test
    public void testClearFile() throws Exception {
        shellProcessBuilder.clearFile();
        PowerMockito.verifyStatic();
        FileUtils.forceDelete(Mockito.any(File.class));
    }

    @Test
    public void testCommandSh() throws Exception {
        ShellProcessBuilder shellProcessBuilder = new ShellProcessBuilder("uuid", ScriptType.SH,
                "command", new HashMap<String, String>(), "", "");
        shellProcessBuilder = shellProcessBuilder.retainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition
                .ALL);
        Assert.assertEquals("sh", shellProcessBuilder.command()[0]);
    }

    @Test
    public void testCommandBash() throws Exception {
        ShellProcessBuilder shellProcessBuilder = new ShellProcessBuilder("uuid", ScriptType.BASH,
                "command", new HashMap<String, String>(), "", "");
        shellProcessBuilder = shellProcessBuilder.retainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition
                .ALL);
        Assert.assertEquals("bash", shellProcessBuilder.command()[0]);
    }

    @Test
    public void testCommandBat() throws Exception {
        ShellProcessBuilder shellProcessBuilder = new ShellProcessBuilder("uuid", ScriptType.BAT,
                "command", new HashMap<String, String>(), "", "");
        shellProcessBuilder = shellProcessBuilder.retainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition
                .ALL);
        Assert.assertEquals("cmd", shellProcessBuilder.command()[0]);
    }

    @Test
    public void testCoverageHook() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(0);
        shellProcessBuilder.setRetainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition.JOB_FAIL);
        ArrayList<ShellHook> hooks = new ArrayList<ShellHook>();
        hooks.add(new ShellCoverageHook());
        shellProcessBuilder.hook(hooks);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(4));
    }

    @Test
    public void testNotCoverageHook() throws Exception {
        PowerMockito.when(processBuilder.environment()).thenReturn(new HashMap<String, String>());
        PowerMockito.when(processBuilder.start()).thenReturn(process);
        PowerMockito.when(process.getOutputStream()).thenReturn(new OutputStream() {
            @Override
            public void write(int b) {
                // do noting
            }
        });

        PowerMockito.when(process.waitFor()).thenReturn(0);
        shellProcessBuilder.setRetainWorkspaceCondition(ShellJobParameter.RetainWorkspaceCondition.JOB_FAIL);
        ArrayList<ShellHook> hooks = new ArrayList<ShellHook>();
        hooks.add(new ShellInvalidateGitSshHook());
        shellProcessBuilder.hook(hooks);
        shellProcessBuilder.start();
        // 等待线程执行完成
        Thread.sleep(1000);
        Mockito.verify(FileUtils.class, Mockito.times(4));
    }

    @Test
    public void testKillDescendantFail() throws Exception {
        prepareForReportTest(false, 1);
        PowerMockito.mockStatic(ProcessTree.class);
        PowerMockito.when(ProcessTree.getTree()).thenThrow(new RuntimeException());
        PowerMockito.mockStatic(JobManager.class);
        shellProcessBuilder.killDescendant(true).start();
        // 等待线程执行完成
        Thread.sleep(1000);
        PowerMockito.verifyStatic();
        JobManager.endJob(Mockito.anyString(), Mockito.eq(JobStatus.SUCC));
    }

    @Test(expected = IOException.class)
    public void testStartException4CreateWorkspace() throws Exception {
        PowerMockito.doThrow(new IOException())
                .when(FileUtils.class, "forceMkdir", Mockito.any(File.class));
        PowerMockito.when(jobAsyncLogger, "log", Mockito.anyString()).thenAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                return null;
            }
        });
        shellProcessBuilder.start();
    }
}
