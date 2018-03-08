package com.xingen.volleylibtest.bean;

import com.xingen.volleylib.utils.GsonUtils;

/**
 * Created by ${xinGen} on 2018/3/8.
 */

public class FileBean {
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return GsonUtils.toJson(this);
    }
}
