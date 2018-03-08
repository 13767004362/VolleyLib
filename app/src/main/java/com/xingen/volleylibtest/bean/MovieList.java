package com.xingen.volleylibtest.bean;

import com.google.gson.Gson;

import java.util.List;

/**
 * Created by ${xinGen} on 2018/3/7.
 */

public class MovieList<T> {
    public List<T> getSubjects() {
        return subjects;
    }

    private List<T> subjects;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
