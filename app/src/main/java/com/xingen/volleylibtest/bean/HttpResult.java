package com.xingen.volleylibtest.bean;

import com.google.gson.Gson;

/**
 * Created by ${xinGen} on 2018/1/20.
 */

public class HttpResult<T> {
    public int code;
    public T data;
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
