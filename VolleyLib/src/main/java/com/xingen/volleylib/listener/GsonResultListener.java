package com.xingen.volleylib.listener;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.$Gson$Types;
import com.xingen.volleylib.volley.NetworkResponse;
import com.xingen.volleylib.volley.ParseError;
import com.xingen.volleylib.volley.Response;
import com.xingen.volleylib.volley.VolleyError;
import com.xingen.volleylib.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by ${xinGen} on 2018/1/29.
 */

public abstract class GsonResultListener<T> implements Response.Listener<T>, Response.ErrorListener {
    private Type type;
    private Gson gson;
    public GsonResultListener() {
        this.gson = new Gson();
        this.type = getSuperclassTypeParameter(this.getClass());
    }
    public Response<T> parseResponse(NetworkResponse response) {
        try {
            String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            T t = gson.fromJson(json, type);
            return Response.success(t, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JsonSyntaxException e) {
            return Response.error(new ParseError(e));
        }
    }
    @Override
    public void onResponse(T response) {
        this.success(response);
    }
    @Override
    public void onErrorResponse(VolleyError error) {
        this.error(error);
    }

    public static Type getSuperclassTypeParameter(Class<?> subclass) {
        //得到带有泛型的类
        Type superclass = subclass.getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new RuntimeException("Missing type parameter.");
        }
        //取出当前类的泛型
        ParameterizedType parameter = (ParameterizedType) superclass;
        return $Gson$Types.canonicalize(parameter.getActualTypeArguments()[0]);
    }

    /**
     * 解析结果
     * @param t

     */
   public  abstract  void success(T t);

    /**
     * 异常结果
     * @param volleyError
     */
   public  abstract void error(VolleyError volleyError);
}
