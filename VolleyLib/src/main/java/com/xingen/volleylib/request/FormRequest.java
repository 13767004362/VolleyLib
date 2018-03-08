package com.xingen.volleylib.request;

import android.text.TextUtils;

import com.xingen.volleylib.listener.GsonResultListener;
import com.xingen.volleylib.utils.RetryPolicyUtils;
import com.xingen.volleylib.volley.AuthFailureError;
import com.xingen.volleylib.volley.NetworkResponse;
import com.xingen.volleylib.volley.Request;
import com.xingen.volleylib.volley.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ${xinGen} on 2018/1/29.
 *
 *  Form表单上传文本，Gson解析json的请求
 *
 *  内容格式：application/x-www-form-urlencoded
 */
public class FormRequest<T> extends Request<T> {
    private final GsonResultListener<T> resultListener;
    private Map<String, String> body;
    private Map<String, String> headers;
    public FormRequest(String url, Map<String, String> body, GsonResultListener<T> resultListener) {
        this(Method.POST, url, body, resultListener);
    }
    public FormRequest(int method, String url, Map<String, String> body, GsonResultListener<T> resultListener) {
        super(method, url, resultListener);
        this.headers = new HashMap<>();
        this.body = body;
        this.resultListener = resultListener;
        this.setRetryPolicy(RetryPolicyUtils.createDefaultPolicy());
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        return this.resultListener.parseResponse(response);
    }

    @Override
    protected void deliverResponse(T response) {
        this.resultListener.onResponse(response);
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> setHeader(String key, String content) {
        if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(content)) {
            headers.put(key, content);
        }
        return headers;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return this.body;
    }

}
