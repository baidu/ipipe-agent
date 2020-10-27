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

package com.baidu.agile.agent.hook.cov;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by niuwanpeng on 17/12/20.
 */
public class CovHttpClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(CovHttpClientUtil.class);

    private static PoolingHttpClientConnectionManager cm;
    private static String UTF_8 = "UTF-8";
    // 设置连接超时时间(单位毫秒)
    private static final int CONNECTION_TIME_OUT = 7000;
    // 设置读数据超时时间(单位毫秒)
    private static final int SOCKET_TIME_OUT = 30000;
    private static final Integer defaultRetryCount = 3;

    public static final String HTML_CONTENT_TYPE = "text/html;charset=UTF-8";
    public static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
    public static final String TEXT_CONTENT_TYPE = "text/plain";
    public static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";

    private static void init() {
        if (cm == null) {
            cm = new PoolingHttpClientConnectionManager();
            // 整个连接池最大连接数
            cm.setMaxTotal(50);
            // 每路由最大连接数，默认值是2
            cm.setDefaultMaxPerRoute(5);
        }
    }

    /**
     * 通过连接池获取HttpClient
     *
     * @param requestRetryHandler 重试次数处理器
     *
     * @return
     */
    private static CloseableHttpClient getHttpClient(HttpRequestRetryHandler requestRetryHandler) {
        init();
        return HttpClients.custom().setRetryHandler(requestRetryHandler).setConnectionManager(cm).build();
    }

    public static CovResponseResult postWithFileBody(String url, Map<String, String> params,
                                         Map<String, FileBody> fileParam, int socketTimeout) throws IOException {
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        for (Map.Entry<String, FileBody> entry : fileParam.entrySet()) {
            entityBuilder.addPart(entry.getKey(), entry.getValue());
        }
        return post(url, entityBuilder, params, socketTimeout);
    }

    /**
     * 携带文件的post请求
     * @param url              请求的url
     * @param params           传输参数
     * @param fileParam        文件流参数
     * @param socketTimeout    超时时间
     * @return
     * @throws IOException
     */
    public static CovResponseResult post(String url, Map<String, String> params,
                                         Map<String, File> fileParam, int socketTimeout) throws IOException {
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        for (Map.Entry<String, File> entry : fileParam.entrySet()) {
            entityBuilder.addPart(entry.getKey(), new FileBody(entry.getValue()));
        }
        return post(url, entityBuilder, params, socketTimeout);
    }

    private static CovResponseResult post(String url, MultipartEntityBuilder entityBuilderWithFiles,
                                          Map<String, String> params, int socketTimeout) throws IOException {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
                continue;
            }
            try {
                entityBuilderWithFiles.addTextBody(
                        key, entry.getValue(), ContentType.create("text/plain", Charset.forName(UTF_8)));
            } catch (Exception e) {
                LOGGER.error("Deal entity:{} is fail!", key, e);
            }
        }
        HttpEntity reqEntity = entityBuilderWithFiles.build();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(reqEntity);
        return getResult(httpPost, socketTimeout, 1);
    }

    /**
     * 处理Http请求
     *
     * @param request
     * @param socketTimeout 超时时间(单位毫秒)
     * @param retryCount    重试次数
     *
     * @return
     */
    private static CovResponseResult getResult(HttpRequestBase request, int socketTimeout, Integer retryCount)
            throws IOException {
        // 设置超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIME_OUT)
                .setSocketTimeout(socketTimeout).build();
        request.setConfig(requestConfig);

        // 设置重试次数
        HttpRequestRetryHandler requestRetryHandler;
        if (retryCount != null && retryCount > 0) {
            requestRetryHandler = new RetryHandlerImpl(retryCount);
        } else {
            requestRetryHandler = new RetryHandlerImpl(defaultRetryCount);
        }

        CloseableHttpClient httpClient = getHttpClient(requestRetryHandler);

        CloseableHttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        CovResponseResult covResponseResultForUrl = new CovResponseResult();
        covResponseResultForUrl.setStatusCode(response.getStatusLine().getStatusCode());
        if (entity != null) {
            String result = EntityUtils.toString(entity);
            response.close();
            covResponseResultForUrl.setResult(result);
            return covResponseResultForUrl;
        }

        return covResponseResultForUrl;
    }

    public static class RetryHandlerImpl extends DefaultHttpRequestRetryHandler {
        private static Set<Class<? extends IOException>> nonRetriableClasses;

        static {
            nonRetriableClasses = new HashSet<Class<? extends IOException>>();
        }

        RetryHandlerImpl(Integer retryCount) {
            super(retryCount, true, nonRetriableClasses);
        }
    }
}
