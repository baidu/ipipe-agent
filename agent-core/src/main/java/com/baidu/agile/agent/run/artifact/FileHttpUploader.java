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

package com.baidu.agile.agent.run.artifact;

import com.baidu.agile.agent.AgentException;
import com.baidu.agile.agent.common.http.HttpClient;
import com.baidu.agile.agent.common.response.Response;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FileHttpUploader implements FileUploader {

    private String url;

    public FileHttpUploader(String url) {
        this.url = url;
    }

    @Override
    public void upload(File file, String toFile) {
        if (!file.exists() || !file.isFile()) {
            return;
        }
        try {
            Response response = new HttpClient(url)
                    .path("/report/upload")
                    .param("file", toFile)
                    .timeout(60 * 1000) // 上传文件，暂定1分钟超时
                    .header("Content-Type", "application/json")
                    .body(new FileInputStream(file))
                    .post(Response.class);
            if (null == response) {
                throw new AgentException("upload file response null!");
            }
            if (!response.isSuccess()) {
                throw new AgentException("upload file error:" + response.getError());
            }
        } catch (FileNotFoundException e) {
            // file.exists(), normally unreachable, so do nothing
        }

    }

}
