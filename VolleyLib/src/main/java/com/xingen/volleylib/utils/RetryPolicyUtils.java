package com.xingen.volleylib.utils;

import com.xingen.volleylib.volley.DefaultRetryPolicy;
import com.xingen.volleylib.volley.RetryPolicy;

/**
 * Created by ${xinGen} on 2018/3/5.
 *
 * 重试次数对象的工具类
 */

public class RetryPolicyUtils {
    private static final  int MAX_TIME_OUT=50*1000;
    public static RetryPolicy createDefaultPolicy(){
        return createPolicy(MAX_TIME_OUT, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    }
    public static RetryPolicy createPolicy(int initialTimeoutMs, int maxNumRetries, float backoffMultiplier){
        return new DefaultRetryPolicy(initialTimeoutMs,maxNumRetries,backoffMultiplier);
    }
}
