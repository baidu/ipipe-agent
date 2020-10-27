package com.baidu.agile.agent.common.ftp;

import com.baidu.agile.agent.AgentException;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FtpDownloader.class, FTPClient.class, FileOutputStream.class})
public class FtpDownloaderTest {

    @Before
    public void before() {
        PowerMockito.mockStatic(FTPClient.class);
        PowerMockito.mockStatic(FileOutputStream.class);
    }

    @Test(expected = AgentException.class)
    public void testDownloadLogFtpException() throws Exception {
        FTPClient ftpClient = Mockito.mock(FTPClient.class);
        PowerMockito.whenNew(FTPClient.class).withAnyArguments().thenReturn(ftpClient);
        Mockito.when(ftpClient.login(Mockito.anyString(), Mockito.anyString())).thenThrow(IOException.class);
        new FtpDownloader()
                .username("u1").password("p1")
                .from("/f1").to("/d1")
                .download();
    }

    @Test(expected = AgentException.class)
    public void testDownloadLogFtpFail() throws Exception {
        FTPClient ftpClient = Mockito.mock(FTPClient.class);
        PowerMockito.whenNew(FTPClient.class).withAnyArguments().thenReturn(ftpClient);
        Mockito.when(ftpClient.getReplyCode()).thenReturn(-1);
        new FtpDownloader()
                .username("u1").password("p1")
                .from("/f1").to("/d1")
                .download();
    }

    @Test(expected = AgentException.class)
    public void testDownloadRetrieveFail() throws Exception {
        FTPClient ftpClient = Mockito.mock(FTPClient.class);
        PowerMockito.whenNew(FTPClient.class).withAnyArguments().thenReturn(ftpClient);
        Mockito.when(ftpClient.getReplyCode()).thenReturn(200);
        Mockito.when(ftpClient.retrieveFile(Mockito.anyString(), Mockito.any(OutputStream.class))).thenReturn(false);
        new FtpDownloader()
                .username("u1").password("p1")
                .from("/f1").to("/d1")
                .download();
    }

    @Test
    public void testDownload() throws Exception {
        FTPClient ftpClient = Mockito.mock(FTPClient.class);
        PowerMockito.whenNew(FTPClient.class).withAnyArguments().thenReturn(ftpClient);
        Mockito.when(ftpClient.getReplyCode()).thenReturn(200);
        Mockito.when(ftpClient.retrieveFile(Mockito.anyString(), Mockito.any(OutputStream.class))).thenReturn(true);
        FileOutputStream outputStream = Mockito.mock(FileOutputStream.class);
        PowerMockito.whenNew(FileOutputStream.class).withAnyArguments().thenReturn(outputStream);
        new FtpDownloader()
                .username("u1").password("p1")
                .from("/f1").to("/d1")
                .download();
        Mockito.verify(ftpClient).logout();
    }
}
