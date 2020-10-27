/**
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.agile.agent.common.ftp;

import com.baidu.agile.agent.AgentException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

public class FtpDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FtpDownloader.class);

    String localFile;

    String ftpFile;

    String username;

    String password;

    public FtpDownloader from(String ftpFile) {
        this.ftpFile = ftpFile;
        return this;
    }

    public FtpDownloader to(String localFile) {
        this.localFile = localFile;
        return this;
    }

    public FtpDownloader username(String username) {
        this.username = username;
        return this;
    }

    public FtpDownloader password(String password) {
        this.password = password;
        return this;
    }

    public void download() {

        FTPClient client = new FTPClient();
        // 1M缓存，加速下载
        client.setBufferSize(1 * 1024 * 1024);
        try {

            URI uri = new URI(ftpFile);

            String host = uri.getHost();

            int port = uri.getPort();

            String remoteFile = uri.getPath();

            if (port > 0) {
                client.connect(host, port);
            } else {
                client.connect(host);
            }

            client.login(username, password);
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
            client.configure(conf);

            int reply = client.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                client.disconnect();
                throw new AgentException("FTP server refused connection.");
            }

            client.setRemoteVerificationEnabled(false);
            client.enterLocalPassiveMode();

            OutputStream fos = new FileOutputStream(localFile);
            if (!client.retrieveFile(remoteFile, fos)) {
                throw new AgentException("retrieve file fail.");
            }
            fos.close();

            client.noop();
            client.logout();
        } catch (AgentException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentException("ftp download exception.", e);
        } finally {
            if (client.isConnected()) {
                try {
                    client.disconnect();
                } catch (IOException ioe) {
                    LOGGER.error("ftp client disconnect exception.", ioe);
                }
            }
        }
    }
}
