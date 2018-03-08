package com.xingen.volleylibtest;

import android.app.Application;

import com.xingen.volleylib.VolleyClient;

/**
 * Created by ${xinGen} on 2018/3/7.
 */

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VolleyClient.getInstance().init(this);
    }
}
