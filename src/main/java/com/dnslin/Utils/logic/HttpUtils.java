package com.dnslin.Utils.logic;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Console;
import com.alibaba.fastjson.JSONObject;
import com.dnslin.Utils.result.HttpClientResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 功能描述
 * httpclient 工具类
 *
 * @param
 * @author dnslin
 * @date 10/27
 * @return
 */
@Slf4j
public class HttpUtils {
    // 编码格式。发送编码格式统一用UTF-8
    private static final String ENCODING = "UTF-8";

    // 设置连接超时时间，单位毫秒。
    private static final int CONNECT_TIMEOUT = 6000;

    // 请求获取数据的超时时间(即响应时间)，单位毫秒。
    private static final int SOCKET_TIMEOUT = 6000;

    public static CloseableHttpClient getHttpClient() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            //不进行主机名验证
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(),
                    NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(100);
            return HttpClients.custom()
                    .setSSLSocketFactory(sslConnectionSocketFactory)
                    .setDefaultCookieStore(new BasicCookieStore())
                    .setConnectionManager(cm).build();
        } catch (KeyManagementException e) {
            Console.log("所有处理密钥管理的操作的通用密钥管理异常");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            Console.log("请求特定加密算法但在环境中不可用");
            e.printStackTrace();
        } catch (KeyStoreException e) {
            Console.log("通用的KeyStore异常");
            e.printStackTrace();
        }
        return HttpClients.createDefault();
    }

    /**
     * 发送get请求；不带请求头和请求参数
     *
     * @param url 请求地址
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult doGet(String url) throws IOException {
        return doGet(url, null, null);
    }

    /**
     * 发送get请求；带请求参数
     *
     * @param url    请求地址
     * @param params 请求参数集合
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult doGet(String url, Map<String, String> params) throws IOException {
        return doGet(url, null, params);
    }

    /**
     * 发送get请求；带请求头和请求参数
     *
     * @param url     请求地址
     * @param headers 请求头集合
     * @param params  请求参数集合
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult doGet(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        // 创建httpClient对象
        CloseableHttpClient httpClient = getHttpClient();

        // 创建访问的地址
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(url);
        } catch (URISyntaxException e) {
            Console.log("链接可能有问题！！！");
            e.printStackTrace();
        }
        if (params != null) {
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue());
            }
        }

        // 创建http对象
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            Console.log("链接可能有问题！！！");
            e.printStackTrace();
        }
        /**
         * setConnectTimeout：设置连接超时时间，单位毫秒。
         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
         */
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpGet.setConfig(requestConfig);

        // 设置请求头
        packageHeader(headers, httpGet);

        // 创建httpResponse对象
        CloseableHttpResponse httpResponse = null;

        try {
            // 执行请求并获得响应结果
            return getHttpClientResult(httpResponse, httpClient, httpGet);
        } finally {
            // 释放资源
            release(httpResponse, httpClient);
        }
    }


    /**
     * 发送get请求；不带请求头和请求参数
     *
     * @param url 请求地址
     * @return CloseableHttpResponse
     * @throws Exception
     */
    public static CloseableHttpResponse doGets(String url) throws IOException {
        return doGets(url, null, null);
    }

    /**
     * 发送get请求；带请求参数
     *
     * @param url    请求地址
     * @param params 请求参数集合
     * @return CloseableHttpResponse
     * @throws Exception
     */
    public static CloseableHttpResponse doGets(String url, Map<String, String> params) throws IOException {
        return doGets(url, null, params);
    }

    /**
     * 发送get请求；带请求头和请求参数
     *
     * @param url     请求地址
     * @param headers 请求头集合
     * @param params  请求参数集合
     * @return CloseableHttpResponse
     * @throws Exception
     */
    public static CloseableHttpResponse doGets(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        // 创建httpClient对象
        CloseableHttpClient httpClient = getHttpClient();

        // 创建访问的地址
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(url);
        } catch (URISyntaxException e) {
            Console.log("链接可能有问题！！！");
            e.printStackTrace();
        }
        if (params != null) {
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue());
            }
        }

        // 创建http对象
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            Console.log("链接可能有问题！！！");
            e.printStackTrace();
        }
        /**
         * setConnectTimeout：设置连接超时时间，单位毫秒。
         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
         */
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpGet.setConfig(requestConfig);

        // 设置请求头
        packageHeader(headers, httpGet);

        // 创建httpResponse对象
        CloseableHttpResponse httpResponse = null;

        try {
            // 执行请求并获得响应结果
            return getResponse(httpResponse, httpClient, httpGet);
        } finally {
            // 释放资源
            release(httpResponse, httpClient);
        }
    }

    /**
     * 发送post请求；不带请求头和请求参数
     *
     * @param url 请求地址
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult doPost(String url) throws IOException {
        return doPost(url, null, null);
    }

    /**
     * 发送post请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult doPost(String url, Map<String, String> params) throws IOException {
        return doPost(url, null, params);
    }

    /**
     * 发送post请求；带请求头和请求参数
     *
     * @param url     请求地址
     * @param headers 请求头集合
     * @param params  请求参数集合
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult doPost(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        // 创建httpClient对象
        CloseableHttpClient httpClient = getHttpClient();

        // 创建http对象
        HttpPost httpPost = new HttpPost(url);
        /**
         * setConnectTimeout：设置连接超时时间，单位毫秒。
         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
         */
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpPost.setConfig(requestConfig);
        // 设置请求头
        packageHeader(headers, httpPost);

        // 封装请求参数
        packageParam(params, httpPost);

        // 创建httpResponse对象
        CloseableHttpResponse httpResponse = null;
        try {
            // 执行请求并获得响应结果
            return getHttpClientResult(httpResponse, httpClient, httpPost);
        } finally {
            // 释放资源
            release(httpResponse, httpClient);
        }
    }

    /**
     * 发送post请求；不带请求头和请求参数
     *
     * @param url 请求地址
     * @return response
     * @throws Exception
     */
    public static CloseableHttpResponse doPosts(String url) throws IOException {
        CloseableHttpResponse closeableHttpResponse = doPosts(url, null, null);
        return closeableHttpResponse;
    }

    /**
     * 发送post请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doPosts(String url, Map<String, String> params)throws IOException {
        return doPosts(url, null, params);
    }

    /**
     * 发送post请求；带请求头和请求参数
     *
     * @param url     请求地址
     * @param headers 请求头集合
     * @param params  请求参数集合
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doPosts(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        // 创建httpClient对象
        CloseableHttpClient httpClient = getHttpClient();

        // 创建http对象
        HttpPost httpPost = new HttpPost(url);
        /**
         * setConnectTimeout：设置连接超时时间，单位毫秒。
         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
         */
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpPost.setConfig(requestConfig);
        // 设置请求头
        packageHeader(headers, httpPost);

        // 封装请求参数
        packageParam(params, httpPost);

        // 创建httpResponse对象
        CloseableHttpResponse httpResponse = null;

        try {
            // 执行请求并获得响应结果
            return getResponse(httpResponse, httpClient, httpPost);
        } finally {
            // 释放资源
            release(httpResponse, httpClient);
        }
    }

    /**
     * 发送put请求；不带请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPut(String url) {
        return doPut(url);
    }

    /**
     * 发送put请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPut(String url, Map<String, String> params) throws IOException{
        CloseableHttpClient httpClient = getHttpClient();
        HttpPut httpPut = new HttpPut(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpPut.setConfig(requestConfig);

        packageParam(params, httpPut);

        CloseableHttpResponse httpResponse = null;

        try {
            return getHttpClientResult(httpResponse, httpClient, httpPut);
        } finally {
            release(httpResponse, httpClient);
        }
    }

    /**
     * 发送put请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doPut(String url, Map<String, String> headers, Map<String, String> params) throws IOException{
        CloseableHttpClient httpClient = getHttpClient();
        HttpPut httpPut = new HttpPut(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpPut.setConfig(requestConfig);
        // 设置请求头
        packageHeader(headers, httpPut);
        packageParam(params, httpPut);

        CloseableHttpResponse httpResponse = null;

        try {
            return getHttpClientResult(httpResponse, httpClient, httpPut);
        } finally {
            release(httpResponse, httpClient);
        }
    }


    /**
     * 发送put请求；不带请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doPuts(String url) {
        return doPuts(url);
    }

    /**
     * 发送put请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doPuts(String url, Map<String, String> params) throws IOException{
        CloseableHttpClient httpClient = getHttpClient();
        HttpPut httpPut = new HttpPut(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpPut.setConfig(requestConfig);

        packageParam(params, httpPut);

        CloseableHttpResponse httpResponse = null;

        try {
            return getResponse(httpResponse, httpClient, httpPut);
        } finally {
            release(httpResponse, httpClient);
        }
    }



    /**
     * 发送put请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doPuts(String url, Map<String, String> headers, Map<String, String> params) throws IOException{
        CloseableHttpClient httpClient = getHttpClient();
        HttpPut httpPut = new HttpPut(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpPut.setConfig(requestConfig);
        // 设置请求头
        packageHeader(headers, httpPut);
        packageParam(params, httpPut);

        CloseableHttpResponse httpResponse = null;

        try {
            return getResponse(httpResponse, httpClient, httpPut);
        } finally {
            release(httpResponse, httpClient);
        }
    }

    /**
     * 发送delete请求；不带请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static HttpClientResult doDelete(String url) throws Exception {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpDelete.setConfig(requestConfig);

        CloseableHttpResponse httpResponse = null;
        try {
            return getHttpClientResult(httpResponse, httpClient, httpDelete);
        } finally {
            release(httpResponse, httpClient);
        }
    }

    /**
     * 发送delete请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static HttpClientResult doDelete(String url, Map<String, String> params) throws IOException {
        if (params == null) {
            params = new HashMap<String, String>();
        }

        params.put("_method", "delete");
        return doPost(url, params);
    }


    /**
     * 发送delete请求；不带请求参数
     *
     * @param url 请求地址
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doDeletes(String url) throws IOException{
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpDelete httpDelete = new HttpDelete(url);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpDelete.setConfig(requestConfig);

        CloseableHttpResponse httpResponse = null;
        try {
            return getResponse(httpResponse, httpClient, httpDelete);
        } finally {
            release(httpResponse, httpClient);
        }
    }

    /**
     * 发送delete请求；带请求参数
     *
     * @param url    请求地址
     * @param params 参数集合
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse doDeletes(String url, Map<String, String> params) throws IOException {
        if (params == null) {
            params = new HashMap<String, String>();
        }

        params.put("_method", "delete");
        return doPosts(url, params);
    }


    /**
     * 发送上传文件请求；
     *
     * @param url      请求地址
     * @param params   参数集合
     * @param headers  请求头集合
     * @param filePath 文件路径
     * @param fileType 文件类型名
     * @return
     * @throws Exception
     */
    public static HttpClientResult uploadFile(String url,
                                              Map<String, String> params,
                                              Map<String, String> headers,
                                              String filePath,
                                              String fileType) throws IOException {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "multipart/form-data");
        packageHeader(headers, httpPost);
        //创建multipart/form-data的entity的builder
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //将文件加在http的post请求中
        final File file = new File(filePath);
        try {
            builder.addBinaryBody(
                    fileType,
                    new FileInputStream(file),
                    ContentType.APPLICATION_OCTET_STREAM,
                    file.getName()
            );
        } catch (FileNotFoundException e) {
            Console.log("文件找不到！！！");
            e.printStackTrace();
        }
        if (params != null) {
            final JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            builder.addTextBody(jsonObject.toString(), ContentType.APPLICATION_JSON.toString());
        }
        //生成multipart/form-data的entity
        final HttpEntity multipartEntity = builder.build();
        httpPost.setEntity(multipartEntity);
        CloseableHttpResponse response = null;
        try {
            return getHttpClientResult(response, httpClient, httpPost);
        } finally {
            release(response, httpClient);
        }
    }

    /**
     * 发送上传文件请求；
     *
     * @param url      请求地址
     * @param params   参数集合
     * @param headers  请求头集合
     * @param file      文件
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult uploadFile(String url,
                                              Map<String, String> params,
                                              Map<String, String> headers,
                                              File file
                                              ) throws IOException {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "multipart/form-data");
        packageHeader(headers, httpPost);
        //创建multipart/form-data的entity的builder
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //将文件加在http的post请求中
        String name = FileNameUtil.mainName(file);
        try {
            builder.addBinaryBody(
                    name,
                    new FileInputStream(file),
                    ContentType.APPLICATION_OCTET_STREAM,
                    file.getName()
            );
        } catch (FileNotFoundException e) {
            Console.log("文件找不到！！！");
            e.printStackTrace();
        }
        if (params != null) {
            final JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            builder.addTextBody(jsonObject.toString(), ContentType.APPLICATION_JSON.toString());
        }
        //生成multipart/form-data的entity
        final HttpEntity multipartEntity = builder.build();
        httpPost.setEntity(multipartEntity);
        CloseableHttpResponse response = null;
        try {
            return getHttpClientResult(response, httpClient, httpPost);
        } finally {
            release(response, httpClient);
        }
    }


    /**
     * 发送上传文件请求；
     *
     * @param url      请求地址
     * @param params   参数集合
     * @param headers  请求头集合
     * @param file      文件byte
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult uploadFile(String url,
                                              Map<String, String> params,
                                              Map<String, String> headers,
                                              String type,
                                              byte[] file,
                                              String filename) throws IOException {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "multipart/form-data");
        packageHeader(headers, httpPost);
        //创建multipart/form-data的entity的builder
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //将文件加在http的post请求中
        builder.addBinaryBody(type,file,ContentType.APPLICATION_OCTET_STREAM,filename);
        if (params != null) {
            final JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            builder.addTextBody(jsonObject.toString(), ContentType.APPLICATION_JSON.toString());
        }
        //生成multipart/form-data的entity
        final HttpEntity multipartEntity = builder.build();
        httpPost.setEntity(multipartEntity);
        log.info("请求实体：{}",multipartEntity.toString());
        CloseableHttpResponse response = null;
        try {
            return getHttpClientResult(response, httpClient, httpPost);
        } finally {
            release(response, httpClient);
        }
    }

    /**
     * 发送上传文件请求；
     *
     * @param url      请求地址
     * @param params   参数集合
     * @param headers  请求头集合
     * @param file      文件byte
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult uploadFile(String url,
                                              Map<String, String> params,
                                              Map<String, String> headers,
                                              String type,
                                              byte[] file) throws IOException {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "multipart/form-data");
        packageHeader(headers, httpPost);
        //创建multipart/form-data的entity的builder
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //将文件加在http的post请求中
        builder.addBinaryBody(type,file);
        if (params != null) {
            final JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            builder.addTextBody(jsonObject.toString(), ContentType.APPLICATION_JSON.toString());
        }
        //生成multipart/form-data的entity
        final HttpEntity multipartEntity = builder.build();
        httpPost.setEntity(multipartEntity);
        log.info("请求实体：{}",multipartEntity.toString());
        CloseableHttpResponse response = null;
        try {
            return getHttpClientResult(response, httpClient, httpPost);
        } finally {
            release(response, httpClient);
        }
    }

    /**
     * 发送上传文件请求；
     *
     * @param url      请求地址
     * @param params   参数集合
     * @param headers  请求头集合
     * @param file      文件
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult uploadFiles(String url,
                                              Map<String, String> params,
                                              Map<String, String> headers,
                                              File file
    ) throws IOException {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader("Content-Type", "multipart/form-data");
        packageHeader(headers, httpPut);
        //创建multipart/form-data的entity的builder
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //将文件加在http的post请求中
        String name = FileNameUtil.mainName(file);
        try {
            builder.addBinaryBody(
                    name,
                    new FileInputStream(file),
                    ContentType.APPLICATION_OCTET_STREAM,
                    file.getName()
            );
        } catch (FileNotFoundException e) {
            Console.log("文件找不到！！！");
            e.printStackTrace();
        }
        if (params != null) {
            final JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            builder.addTextBody(jsonObject.toString(), ContentType.APPLICATION_JSON.toString());
        }
        //生成multipart/form-data的entity
        final HttpEntity multipartEntity = builder.build();
        httpPut.setEntity(multipartEntity);
        CloseableHttpResponse response = null;
        try {
            return getHttpClientResult(response, httpClient, httpPut);
        } finally {
            release(response, httpClient);
        }
    }


    /**
     * 发送上传文件请求；
     *
     * @param url      请求地址
     * @param params   参数集合
     * @param headers  请求头集合
     * @param file      文件byte
     * @return HttpClientResult
     * @throws Exception
     */
    public static HttpClientResult uploadFiles(String url,
                                              Map<String, String> params,
                                              Map<String, String> headers,
                                              String type,
                                              byte[] file) throws IOException {
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        final HttpPut httpPut = new HttpPut(url);
        httpPut.setHeader("Content-Type", "multipart/form-data");
        packageHeader(headers, httpPut);
        //创建multipart/form-data的entity的builder
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        //将文件加在http的post请求中
        builder.addBinaryBody(type,file);
        if (params != null) {
            final JSONObject jsonObject = new JSONObject();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
            builder.addTextBody(jsonObject.toString(), ContentType.APPLICATION_JSON.toString());
        }
        //生成multipart/form-data的entity
        final HttpEntity multipartEntity = builder.build();

        httpPut.setEntity(multipartEntity);
        CloseableHttpResponse response = null;
        try {
            return getHttpClientResult(response, httpClient, httpPut);
        } finally {
            release(response, httpClient);
        }
    }




    /**
     * Description: 封装请求头
     *
     * @param params
     * @param httpMethod
     */
    public static void packageHeader(Map<String, String> params, HttpRequestBase httpMethod) {
        // 封装请求头
        if (params != null) {
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                // 设置到请求头到HttpRequestBase对象中
                httpMethod.setHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Description: 封装请求参数
     *
     * @param params
     * @param httpMethod
     * @throws UnsupportedEncodingException
     */
    public static void packageParam(Map<String, String> params, HttpEntityEnclosingRequestBase httpMethod) {
        // 封装请求参数
        if (params != null) {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }

            // 设置到请求的http对象中
            try {
                httpMethod.setEntity(new UrlEncodedFormEntity(nvps, ENCODING));
            } catch (UnsupportedEncodingException e) {
                Console.log("不支持字符编码。。。");
                e.printStackTrace();
            }
        }
    }

    /**
     * Description: 获得响应结果
     *
     * @param httpClient
     * @return
     * @throws Exception
     */
    public static CloseableHttpResponse getResponse(CloseableHttpResponse httpResponse, CloseableHttpClient httpClient, HttpRequestBase httpMethod) {
        try {
            httpResponse = httpClient.execute(httpMethod);
        } catch (IOException e) {
            Console.log("httpClient.execute！！！网络IO异常",e.getMessage());
            e.printStackTrace();
        } finally {

        }
        return httpResponse;
    }

    /**
     * Description: 获得响应结果
     *
     * @param httpResponse
     * @param httpClient
     * @param httpMethod
     * @return
     * @throws Exception
     */
    public static HttpClientResult getHttpClientResult(CloseableHttpResponse httpResponse,
                                                       CloseableHttpClient httpClient, HttpRequestBase httpMethod) {
        // 执行请求
        try {
            httpResponse = httpClient.execute(httpMethod);
        } catch (IOException e) {
            Console.log("httpClient.execute！！！网络IO异常",e.getMessage());
            e.printStackTrace();
        }

        // 获取返回结果
        if (httpResponse != null && httpResponse.getStatusLine() != null) {
            String content = "";
            if (httpResponse.getEntity() != null) {
                try {
                    content = EntityUtils.toString(httpResponse.getEntity(), ENCODING);
                } catch (IOException e) {
                    Console.log("IO流异常");
                    e.printStackTrace();
                }
            }
            return new HttpClientResult(httpResponse.getStatusLine().getStatusCode(), content);
        }
        return new HttpClientResult(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * @return CookieStore
     * @Description: 获取CookieStore 不带请求头和参数
     * @param: url
     * @author DnsLin
     * @date 2021/11/6 13:53
     */
    public static CookieStore getCookieStore(String url) throws IOException {
        return getCookieStore(url, null, null);
    }

    /**
     * @return CookieStore
     * @Description: 获取CookieStore 带请求头
     * @param: url
     * @author DnsLin
     * @date 2021/11/6 13:53
     */
    public static CookieStore getCookieStore(String url, Map<String, String> headers) throws IOException {
        return getCookieStore(url, headers, null);
    }


    /**
     * @return CookieStore
     * @Description: 获取CookieStore 带请求头和参数
     * @param: url
     * @author DnsLin
     * @date 2021/11/6 13:53
     */
    public static CookieStore getCookieStore(String url, Map<String, String> headers, Map<String, String> params) throws IOException {
        CloseableHttpClient httpClient = getHttpClient();
        HttpClientContext context = HttpClientContext.create();
        // 创建访问的地址
        URIBuilder uriBuilder = null;
        try {
            uriBuilder = new URIBuilder(url);
        } catch (URISyntaxException e) {
            Console.log("链接可能有问题！！！");
            e.printStackTrace();
        }
        if (params != null) {
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                uriBuilder.setParameter(entry.getKey(), entry.getValue());
            }
        }
        // 创建http对象
        HttpGet httpGet = null;
        try {
            httpGet = new HttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            Console.log("链接可能有问题！！！");
            e.printStackTrace();
        }
        /**
         * setConnectTimeout：设置连接超时时间，单位毫秒。
         * setConnectionRequestTimeout：设置从connect Manager(连接池)获取Connection
         * 超时时间，单位毫秒。这个属性是新加的属性，因为目前版本是可以共享连接池的。
         * setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。 如果访问一个接口，多少时间内无法返回数据，就直接放弃此次调用。
         */
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(CONNECT_TIMEOUT).setSocketTimeout(SOCKET_TIMEOUT).build();
        httpGet.setConfig(requestConfig);

        // 设置请求头
        packageHeader(headers, httpGet);

        // 创建httpResponse对象
        CloseableHttpResponse httpResponse = null;
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet, context);
            return context.getCookieStore();
        } catch (IOException e) {
            Console.log("获取cookie！！！IO流异常");
            e.printStackTrace();
        } finally {
            // 释放资源
            release(httpResponse, httpClient);
        }
        return context.getCookieStore();
    }

    /**
     * @return List<Cookie>
     * @Description: 获取CookieList
     * @param: url
     * @author DnsLin
     * @date 2021/11/6 13:53
     */
    public static List<Cookie> getCookies(String url) throws IOException {
        return getCookieStore(url).getCookies();
    }

    /**
     * Description: 释放资源
     *
     * @param httpResponse
     * @param httpClient
     * @throws IOException
     */
    public static void release(CloseableHttpResponse httpResponse, CloseableHttpClient httpClient) throws IOException {
        // 释放资源
        if (httpResponse != null) {
            httpResponse.close();
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }

}
