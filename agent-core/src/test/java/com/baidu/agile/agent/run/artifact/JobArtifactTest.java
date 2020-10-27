package com.baidu.agile.agent.run.artifact;

import com.baidu.agile.agent.log.SaAsyncLogger;
import com.baidu.agile.server.job.bean.artifact.Artifact;
import com.baidu.agile.server.job.bean.artifact.PatternType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Arrays;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JobArtifact.class, File.class})
public class JobArtifactTest {

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(File.class);
    }

    @Test
    public void testUploadAbsolutePath() throws Exception {
        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.isAbsolute()).thenReturn(true);

        DirFileScanner scanner = Mockito.mock(DirFileScanner.class);
        PowerMockito.whenNew(DirFileScanner.class).withAnyArguments().thenReturn(scanner);
        Mockito.when(scanner.scan(Mockito.anyString())).thenReturn(Arrays.asList("f1"));

        Artifact artifact = new Artifact();
        artifact.setPatternType(PatternType.DIR);

        FileUploader fileUploader = new FileUploader() {
            @Override
            public void upload(File file, String toFile) {
            }
        };

        int filesCount = new JobArtifact("u1", "ws", new SaAsyncLogger("uuid", true))
                .artifact(artifact)
                .fileUploader(fileUploader)
                .toDir("d1")
                .upload();

        Assert.assertEquals(1, filesCount);
    }

    @Test
    public void testUploadRelativePath() throws Exception {
        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.isAbsolute()).thenReturn(false);

        DirFileScanner scanner = Mockito.mock(DirFileScanner.class);
        PowerMockito.whenNew(DirFileScanner.class).withAnyArguments().thenReturn(scanner);
        Mockito.when(scanner.scan(Mockito.anyString())).thenReturn(Arrays.asList("f1"));

        Artifact artifact = new Artifact();
        artifact.setPatternType(PatternType.DIR);

        FileUploader fileUploader = new FileUploader() {
            @Override
            public void upload(File file, String toFile) {
            }
        };

        int filesCount = new JobArtifact("u1", "ws", new SaAsyncLogger("uuid", true))
                .artifact(artifact)
                .fileUploader(fileUploader)
                .toDir("d1")
                .upload();

        Assert.assertEquals(1, filesCount);
    }

    @Test
    public void testUploadSingleFile() throws Exception {

        File file = Mockito.mock(File.class);
        PowerMockito.whenNew(File.class).withAnyArguments().thenReturn(file);
        Mockito.when(file.isAbsolute()).thenReturn(false);
        Mockito.when(file.exists()).thenReturn(true);
        Mockito.when(file.isFile()).thenReturn(true);

        DirFileScanner scanner = Mockito.mock(DirFileScanner.class);
        PowerMockito.whenNew(DirFileScanner.class).withAnyArguments().thenReturn(scanner);
        Mockito.when(scanner.scan(Mockito.anyString())).thenReturn(Arrays.asList("f1"));

        Artifact artifact = new Artifact();
        artifact.setFilePattern("/sss/sss");
        artifact.setPatternType(PatternType.DIR);
        new SaAsyncLogger("uuid", true);
        FileUploader fileUploader = new FileUploader() {
            @Override
            public void upload(File file, String toFile) {
            }
        };

        int filesCount = new JobArtifact("u1", "ws", new SaAsyncLogger("uuid", true))
                .artifact(artifact)
                .fileUploader(fileUploader)
                .toDir("d1")
                .upload();

        Assert.assertEquals(1, filesCount);
    }

}
