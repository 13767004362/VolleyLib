package com.xingen.volleylib.utils;

/**
 * Created by ${xinGen} on 2018/3/7.
 */

public class FileUtils {
    /**
     * 从路径中获取文件名
     *
     * @param filePath
     * @return
     */
    public static String getFileName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1, filePath.length());
    }
}
