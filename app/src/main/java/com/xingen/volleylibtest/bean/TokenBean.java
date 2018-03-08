package com.xingen.volleylibtest.bean;

import com.google.gson.Gson;

/**
 * Created by ${xinGen} on 2018/1/10.
 */

public class TokenBean {
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return  new Gson().toJson(this);
    }
}
