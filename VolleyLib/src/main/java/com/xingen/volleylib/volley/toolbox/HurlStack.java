/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xingen.volleylib.volley.toolbox;


import com.xingen.volleylib.request.SingleFileRequest;
import com.xingen.volleylib.volley.AuthFailureError;
import com.xingen.volleylib.volley.Request;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * An {@link HttpStack} based on {@link HttpURLConnection}.
 * <p>
 * 用途：
 * 用HttpURLConnection作为联网通讯类。
 * <p>
 * 1. 发送请求中的header和body
 * 2. 获取响应的数据，包含header。
 */
public class HurlStack implements HttpStack {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * An interface for transforming URLs before use.
     * 在被使用前，用于转换URL
     */
    public interface UrlRewriter {
        /**
         * Returns a URL to use instead of the provided one, or null to indicate
         * this URL should not be used at all.
         */
        String rewriteUrl(String originalUrl);
    }

    private final UrlRewriter mUrlRewriter;
    private final SSLSocketFactory mSslSocketFactory;

    public HurlStack() {
        this(null);
    }

    /**
     * @param urlRewriter Rewriter to use for request URLs
     */
    public HurlStack(UrlRewriter urlRewriter) {
        this(urlRewriter, null);
    }

    /**
     * @param urlRewriter      Rewriter to use for request URLs
     * @param sslSocketFactory SSL factory to use for HTTPS connections
     *                         <p>
     *                         Rewriter用于操作URL
     *                         <p>
     *                         SSLSocketFactory用于Https连接。
     */
    public HurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        mUrlRewriter = urlRewriter;
        mSslSocketFactory = sslSocketFactory;
    }

    /**
     * 执行HttpURLConnection，返回HttpResponse
     *
     * @param request           the request to perform
     * @param additionalHeaders additional headers to be sent together with
     *                          {@link Request#getHeaders()}
     * @return
     * @throws IOException
     * @throws AuthFailureError
     */
    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws IOException, AuthFailureError {
        String url = request.getUrl();
        //创建一个Map来，装载Http中标头
        HashMap<String, String> map = new HashMap<String, String>();
        //添加本次请求中标头
        map.putAll(request.getHeaders());
        //若是数据超过缓存时间，但没有过期，则将上次的缓存header添加上。
        map.putAll(additionalHeaders);
        //对Url进行转换
        if (mUrlRewriter != null) {//默认情况,UrlRewriter 为空，
            String rewritten = mUrlRewriter.rewriteUrl(url);
            if (rewritten == null) {
                throw new IOException("URL blocked by rewriter: " + url);
            }
            url = rewritten;
        }
        //创建一个HttpUrlConnection或者其子类，进行网络连接。
        URL parsedUrl = new URL(url);
        HttpURLConnection connection = openConnection(parsedUrl, request);
        //添加Http的标头
        for (String headerName : map.keySet()) {
            connection.addRequestProperty(headerName, map.get(headerName));
        }
        //根据volley中请求，来设置HttpUrlConnection的连接方式，和传递的内容
        setConnectionParametersForRequest(connection, request);
        // Initialize HttpResponse with data from the HttpURLConnection.
        //初始化 HttpResponse，用 HttpURLConnection的数据
        ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
        int responseCode = connection.getResponseCode();
        if (responseCode == -1) {
            // -1 is returned by getResponseCode() if the response code could not be retrieved.
            // Signal to the caller that something was wrong with the connection.
            throw new IOException("Could not retrieve response code from HttpUrlConnection.");
        }

        StatusLine responseStatus = new BasicStatusLine(protocolVersion,
                connection.getResponseCode(), connection.getResponseMessage());

        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        //设置服务器响应的内容，内容类型，编码类型，长度
        response.setEntity(entityFromConnection(connection));
        //添加服务器响应的标头
        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
                response.addHeader(h);
            }
        }
        return response;
    }

    /**
     * Initializes an {@link HttpEntity} from the given {@link HttpURLConnection}.
     *
     * @param connection
     * @return an HttpEntity populated with data from <code>connection</code>.
     * <p>
     * <p>
     * 初始化一个HttpEntity从指定的HttpURLConnection中。
     * 返回一个带有服务器返回结果的HttpEntity
     */
    private static HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        //设置返回内容
        entity.setContent(inputStream);
        //设置返回内容的长度
        entity.setContentLength(connection.getContentLength());
        //设置返回内容的编码格式
        entity.setContentEncoding(connection.getContentEncoding());
        //设置返回内容的数据类型
        entity.setContentType(connection.getContentType());
        return entity;
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     * 通过URL开启一个客户端与url指向资源的间的网络通道。
     */
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Opens an {@link HttpURLConnection} with parameters.
     *
     * @param url
     * @return an open connection
     * @throws IOException 根据url中带有的协议，来开启一个带有参数的HttpURLConnection，或者HttpsURLConnection
     */
    private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = request.getTimeoutMs();
        //设置连接时间
        connection.setConnectTimeout(timeoutMs);
        //设置读取时间
        connection.setReadTimeout(timeoutMs);
        //不设置http缓存
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        // 若是HTTPS协议，则使用HttpsURLConnection进行连接，且添加自定义的SSLSocketFactory
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
        }
        return connection;
    }

    /**
     * 根据请求中方式，来设置HttpURLConnection中传递的内容，和方式
     *
     * @param connection
     * @param request
     * @throws IOException
     * @throws AuthFailureError
     */
    @SuppressWarnings("deprecation")
    /* package */ static void setConnectionParametersForRequest(HttpURLConnection connection,
                                                                Request<?> request) throws IOException, AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.DEPRECATED_GET_OR_POST:
                // This is the deprecated way that needs to be handled for backwards compatibility.
                // If the request's post body is null, then the assumption is that the request is
                // GET.  Otherwise, it is assumed that the request is a POST.
                byte[] postBody = request.getPostBody();
                if (postBody != null) {
                    // Prepare output. There is no need to set Content-Length explicitly,
                    // since this is handled by HttpURLConnection using the size of the prepared
                    // output stream.
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");
                    connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getPostBodyContentType());
                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.write(postBody);
                    out.close();
                }
                break;
            case Request.Method.GET:
                // Not necessary to set the request method because connection defaults to GET but
                // being explicit here.
                connection.setRequestMethod("GET");
                break;
            case Request.Method.DELETE:
                connection.setRequestMethod("DELETE");
                break;
            case Request.Method.POST:
                connection.setRequestMethod("POST");
                addBodyIfExists(connection, request);
                break;
            case Request.Method.PUT:
                connection.setRequestMethod("PUT");
                addBodyIfExists(connection, request);
                break;
            case Request.Method.HEAD:
                connection.setRequestMethod("HEAD");
                break;
            case Request.Method.OPTIONS:
                connection.setRequestMethod("OPTIONS");
                break;
            case Request.Method.TRACE:
                connection.setRequestMethod("TRACE");
                break;
            case Request.Method.PATCH:
                addBodyIfExists(connection, request);
                connection.setRequestMethod("PATCH");
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    /**
     * 若是请求中存在Body(post传递的参数),则写入body到流中。
     *
     * @param connection
     * @param request
     * @throws IOException
     * @throws AuthFailureError
     */
    private static void addBodyIfExists(HttpURLConnection connection, Request<?> request)
            throws IOException, AuthFailureError {
        //全部body转成byte数组，若是文件或者超大的byte数组会导致内存占用巨大。
        if (request instanceof SingleFileRequest) {
            addFileBody(connection, (SingleFileRequest) request);
        } else {
            byte[] body = request.getBody();
            if (body != null) {
                //设置post请求方法，允许写入客户端传递的参数
                connection.setDoOutput(true);
                //设置标头的Content-Type属性
                connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
                //数据操作流
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                //写入post传递的参数
                out.write(body);
                out.close();
            }
        }
    }

    /**
     *
     * 添加文件，避免内存巨大
     *
     * @param connection
     * @param request
     * @throws IOException
     * @throws AuthFailureError
     */
    private static void addFileBody(HttpURLConnection connection, SingleFileRequest request) throws IOException, AuthFailureError {
        String filePath = request.getFilePath();
        if (filePath == null) {
            throw new IOException("File文件路径为空");
        }
        File file = new File(filePath);
        if (file == null || !file.exists()) {
            throw new IOException("File 文件不存在");
        }
        //设置post请求方法，允许写入客户端传递的参数
        connection.setDoOutput(true);
        //设置标头的Content-Type属性
        connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        FileInputStream fileInputStream = new FileInputStream(file);
        long total = 0;
        int read ;
        outputStream.writeUTF(request.getContentHeader());
        byte[] buffer = new byte[SingleFileRequest.READ_SIZE];
        while ((read = fileInputStream.read(buffer)) != -1) {
            if (request.isCanceled()){
                fileInputStream.close();
                outputStream.close();
                throw  new IOException("SingleRequest 被取消");
            }
            outputStream.write(buffer, 0, read);
            outputStream.flush();
            total += read;
            int progress = (int) (total * 100 / file.length());
            request.deliverProgress(progress);
        }
        outputStream.writeUTF(request.getContentFoot());
        outputStream.flush();
        fileInputStream.close();
        outputStream.close();
    }
}
