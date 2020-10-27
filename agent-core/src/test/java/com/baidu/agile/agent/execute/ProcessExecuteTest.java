package com.baidu.agile.agent.execute;

import com.baidu.agile.agent.context.Context;
import com.baidu.agile.agent.register.ReRegisterThread;
import com.baidu.agile.agent.run.artifact.AntFileScanner;
import com.baidu.agile.agent.run.artifact.DirFileScanner;
import com.baidu.agile.agent.run.artifact.FileHttpUploader;
import com.baidu.agile.agent.run.artifact.FileScanner;
import com.baidu.agile.agent.run.artifact.JobArtifact;
import com.baidu.agile.server.job.bean.artifact.Artifact;
import com.baidu.agile.server.job.bean.artifact.PatternType;
import com.baidu.agile.server.job.bean.concrete.ScriptType;
import com.baidu.agile.server.job.bean.concrete.ShellJobParameter;
import com.baidu.agile.server.job.bean.report.Report;
import com.baidu.agile.server.job.bean.report.ReportType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * 进程执行
 */
public class ProcessExecuteTest {

    private String uuid = UUID.randomUUID().toString();

    // @Test
    public void testExecuteCommand() throws IOException {
        System.out.println("begin run:" + new Date());
        final Process process = Runtime.getRuntime().exec("sh /Users/baidu/Code/Git/agent/tmp.sh");

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    System.out.println("begin sleep:" + new Date());
//                    Thread.sleep(30 * 1000);
//                } catch (Exception e) {
//
//                } finally {
//                    System.out.println("begin destory:" + new Date());
//                    process.destroy();
//                }
//
//            }
//        }).start();

        try {
            System.out.println("begin wait:" + new Date());
            int returnVal = process.waitFor();
            System.out.println("end wait:" + new Date());
            System.out.println(returnVal);
        } catch (Exception e) {
            e.printStackTrace();
        }

//
//        String command2 = "cd .\nmvn clean install";
//        Process process2 = ProcessExecute.executeCommand(uuid, command2);
//
//        String command3 = "python -V";
//        Process process3 = ProcessExecute.executeCommand(uuid, command3);
    }

    // @Test
    public void testKillProcess() throws IOException {
        String ws = "/Users/baidu/Code/Git/agent/workspace";
//
//        Process process0 = Runtime.getRuntime().exec("sh -xe a.sh", new String[] {"abc=123"}, new File(ws));
//        try {
//            System.out.println("========00000000");
//            System.out.println(IOUtils.toString(process0.getInputStream()));
//            System.out.println(process0.waitFor());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
        ProcessBuilder pb = new ProcessBuilder("sh", "-xe", "a.sh")
                .directory(new File(ws));
        pb.environment().clear();
        pb.environment().put("abc", "123");
        Process process1 = pb.start();

        try {
            System.out.println("========1111111");
            System.out.println(IOUtils.toString(process1.getInputStream()));
            System.out.println(process1.waitFor());
        } catch (Exception e) {
            e.printStackTrace();
        }


//        Process process2 = new ProcessBuilder().command("sh", "-xe", "a.sh").directory(new File(ws)).redirectErrorStream(true).start();
//        try {
//            System.out.println("========222222");
//            System.out.println(IOUtils.toString(process2.getInputStream()));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    // @Test
    public void test() {
        //
        String uuid = "test1";
        ShellProcessBuilder process = new ShellProcessBuilder(uuid, ScriptType.SH, "", null, null,
                null);

        process.reportStart(uuid);

        Report report = new Report();
        report.setReportName("r1");
        report.setReportType(ReportType.HTML);
        Artifact artifact = new Artifact();
        artifact.setPatternType(PatternType.DIR);
        artifact.setFilePattern("");
        artifact.setToDir("");
        report.setArtifact(artifact);

        int fileCount = new JobArtifact(uuid)
                .fileUploader(new FileHttpUploader(Context.getServerContext().getReportServerUrl()))
                .artifact(report.getArtifact())
                .toDir(report.getReportName())
                .upload();

        System.out.println(fileCount);

        process.reportFinish(uuid, report.getReportName(), report.getReportType());
    }

}
