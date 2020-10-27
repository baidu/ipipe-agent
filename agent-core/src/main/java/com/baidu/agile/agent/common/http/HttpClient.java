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

package com.baidu.agile.agent.common.http;

import com.baidu.agile.agent.context.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClient.class.getName());

    public static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    protected String endpoint = StringUtils.EMPTY;

    protected String path = StringUtils.EMPTY;

    protected int retry = 1;

    /**
     * read timeout, 0 as infinate time
     */
    protected int timeout = 5000;

    protected String encoding = "UTF-8";

    protected Map<String, String> params = new HashMap<String, String>();

    protected Map<String, String> headers = new HashMap<String, String>();

    protected String body = StringUtils.EMPTY;

    protected InputStream inputStreamBody;

    protected String outFile;

    private boolean uploadLog = false;

    public HttpClient(String endpoint) {
        super();
        this.endpoint = endpoint;
    }

    public HttpClient path(String path) {
        this.path = path;
        return this;
    }

    public HttpClient param(String key, String value) {
        params.put(key, value);
        return this;
    }

    public HttpClient param(Map<String, String> map) {
        if (MapUtils.isNotEmpty(map)) {
            params.putAll(map);
        }
        return this;
    }

    public HttpClient header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public HttpClient header(Map<String, String> map) {
        if (MapUtils.isNotEmpty(map)) {
            headers.putAll(map);
        }
        return this;
    }

    public HttpClient body(String contentType, String body) {
        if (StringUtils.isNotBlank(contentType)) {
            header("Content-Type", contentType);
        }
        this.body = body;
        return this;
    }

    public HttpClient body(InputStream inputStreamBody) {
        this.inputStreamBody = inputStreamBody;
        return this;
    }

    public HttpClient jsonBody(String body) {
        body("application/json;charset=UTF-8", body);
        return this;
    }

    public HttpClient jsonBody(Object object) {
        try {
            body("application/json;charset=UTF-8", MAPPER.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            LOG.error("JsonProcessingException when serialize json body!", e);
            throw new RuntimeException("JsonProcessingException when serialize json body!");
        }
        return this;
    }

    public HttpClient retry(int retry) {
        this.retry = retry > 1 ? retry : 1;
        return this;
    }

    public HttpClient timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public HttpClient outFile(String outFile) {
        this.outFile = outFile;
        return this;
    }

    public HttpClient uploadLog(boolean uploadLog) {
        this.uploadLog = uploadLog;
        return this;
    }

    /**
     * set encoding for response
     *
     * @param encoding
     * @return
     */
    public HttpClient encoding(String encoding) {
        if (StringUtils.isNotBlank(encoding)) {
            this.encoding = encoding;
        }
        return this;
    }

    public <T> T get(Class<T> returnType) {
        return requestRetryTimes("GET", returnType);
    }

    public <T> T post(Class<T> returnType) {
        return requestRetryTimes("POST", returnType);
    }


    private <T> T requestRetryTimes(String method, Class<T> returnType) {
        for (int i = 0; i < retry; i++) {
            T t = request(method, returnType);
            if (null != t) {
                return t;
            }
        }
        return null;
    }

    protected <T> T request(String method, Class<T> returnType) {
        HttpURLConnection conn = null;
        OutputStream out = null;
        try {
            URL url = new URL(endpoint + path + urlParamString());
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod(method);

            if (MapUtils.isNotEmpty(headers)) {
                for (Entry<String, String> header : headers.entrySet()) {
                    conn.addRequestProperty(header.getKey(), header.getValue());
                }
            }
            if (StringUtils.isNotBlank(body)) {
                conn.setDoOutput(true);
                out = conn.getOutputStream();
                out.write(body.getBytes(encoding));
                out.flush();
            } else if (inputStreamBody != null) {
                conn.setDoOutput(true);
                out = conn.getOutputStream();
                IOUtils.copy(inputStreamBody, out);
                try {
                    inputStreamBody.close();
                } catch (Exception e) {
                    LOG.error("close inputStreamBody exception!", e);
                }
                out.flush();
            }
            LOG.info(method + " " + url.toString() + ":" + conn.getResponseCode());
            if (this.uploadLog) {
                Context.log(method + " " + url.toString() + ":" + conn.getResponseCode());
            }

            InputStream in = conn.getInputStream();
            if (returnType == String.class) {
                return (T) IOUtils.toString(in, encoding);
            } else if (returnType == File.class) {
                if (StringUtils.isEmpty(outFile)) {
                    return null;
                }
                File file = new File(outFile);
                FileUtils.copyToFile(in, file);
                return (T) file;
            }
            return MAPPER.readValue(new InputStreamReader(in, encoding), returnType);
        } catch (Exception e) {
            LOG.error("http client exception:", e);
            if (this.uploadLog) {
                Context.log("http client exception: " + e.getMessage(), e);
            }
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            }
            if (null != conn) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            }
        }
        return null;
    }

    protected String urlParamString() throws UnsupportedEncodingException {
        StringBuffer paramsSb = new StringBuffer("?");
        if (MapUtils.isNotEmpty(params)) {
            for (Entry<String, String> param : params.entrySet()) {
                if (StringUtils.isNotBlank(param.getKey())) {
                    paramsSb
                            .append(param.getKey())
                            .append('=')
                            .append(null == param.getValue() ? null : URLEncoder.encode(param.getValue(), encoding))
                            .append('&');
                }
            }
        }

        if (paramsSb.length() > 0) {
            return paramsSb.deleteCharAt(paramsSb.length() - 1).toString();
        }
        return StringUtils.EMPTY;
    }
}
