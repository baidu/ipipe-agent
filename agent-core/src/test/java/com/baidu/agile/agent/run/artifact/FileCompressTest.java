package com.baidu.agile.agent.run.artifact;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.util.UUID;

public class FileCompressTest {

    @Test
    public void testCompress() throws Exception {
        String tmpDir = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID();
        FileUtils.write(new File(tmpDir + File.separator + "test"), "test", Charset.defaultCharset());
        FileCompress.compress(tmpDir, tmpDir + ".zip", "");
        Assert.assertTrue(new File(tmpDir + ".zip").exists());
        FileUtils.forceDelete(new File(tmpDir));
        FileUtils.forceDelete(new File(tmpDir + ".zip"));
    }

}
