package com.xingen.volleylibtest;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.xingen.volleylib.VolleyClient;
import com.xingen.volleylib.listener.BitmapResultListener;
import com.xingen.volleylib.listener.DownloadListener;
import com.xingen.volleylib.listener.FileProgressListener;
import com.xingen.volleylib.listener.GsonResultListener;
import com.xingen.volleylib.volley.VolleyError;
import com.xingen.volleylibtest.bean.FileBean;
import com.xingen.volleylibtest.bean.HttpResult;
import com.xingen.volleylibtest.bean.Movie;
import com.xingen.volleylibtest.bean.MovieList;
import com.xingen.volleylibtest.bean.TokenBean;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        testImageRequest();
    }

    private void initView() {
        findViewById(R.id.net_download_file).setOnClickListener(this);
        findViewById(R.id.net_upload_file).setOnClickListener(this);
        findViewById(R.id.net_form_text).setOnClickListener(this);
        findViewById(R.id.net_json_text).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.net_download_file:
                testDownloadFile();
                break;
            case R.id.net_upload_file:
                testUploadFile();
                break;
            case R.id.net_form_text:
                testFormRequest();
                break;
            case R.id.net_json_text:
                testJsonRequest();
                break;
        }
    }

    /**
     * 测试DownloadRequest,实现文件下载
     */
    private void testDownloadFile() {
        String url2 = "http://yun.aiwan.hk/1441972507.apk";
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "BaiHeWang.apk";
        VolleyClient.getInstance().startDownload(TAG, url2, filePath, new FileProgressListener() {
            @Override
            public void progress(int progress) {
                Log.i("DownloadRequest", " 下载进度 " + progress);
            }
        }, new DownloadListener() {
            @Override
            public void downloadFinish(String url, String filePath) {
                Log.i("DownloadRequest", " 文件下载完成,路径是 " + filePath);
            }

            @Override
            public void downloadError(String s, VolleyError volleyError) {
                Log.i("DownloadRequest", " 文件下载异常 " + s + " 异常是 " + volleyError.toString());
            }
        });
    }

    /**
     * 测试CircleBitmapImageRequest , 实现圆形图片
     */
    private void testImageRequest() {
        final ImageView networkImageView = findViewById(R.id.net_iv);
        VolleyClient.getInstance().loadCircleNetImage("https://www.baidu.com/img/bd_logo1.png", networkImageView,
                new BitmapResultListener(this, R.mipmap.ic_launcher, R.mipmap.ic_launcher) {
                    @Override
                    public void success(Bitmap bitmap) {
                        networkImageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void error(VolleyError volleyError, Bitmap bitmap) {
                        networkImageView.setImageBitmap(bitmap);
                    }
                });
    }

    /**
     * 测试FormRequest，实现Form表单
     */
    private void testFormRequest() {
        Map<String, String> body = new HashMap<>();
        body.put("appId", "2");
        body.put("loginName", "testAdmin");
        body.put("userPass", "123456");
        VolleyClient.getInstance().sendFormRequest(TAG, "http://yanfayi.cn:8889/user/login", body,
                new GsonResultListener<HttpResult<TokenBean>>() {
                    @Override
                    public void success(HttpResult<TokenBean> tokenBeanHttpResult) {
                        Log.i("FormRequest", tokenBeanHttpResult.toString());
                    }

                    @Override
                    public void error(VolleyError volleyError) {
                        Log.i("FormRequest", " 发生异常： " + volleyError.toString());
                    }
                });
    }

    /**
     * 测试GsonRequest，实现json数据协议，Gson解析
     */
    private void testJsonRequest() {
        String url = "https://api.douban.com/v2/movie/search";
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject();
            jsonObject.put("q", "张艺谋");
        } catch (Exception e) {
            e.printStackTrace();
        }
        VolleyClient.getInstance().sendGsonRequest(TAG, url, jsonObject, new GsonResultListener<MovieList<Movie>>() {
            @Override
            public void success(MovieList<Movie> movieMovieList) {
                Log.i("JsonRequest", "响应结果 " + movieMovieList.toString() + " " + movieMovieList.getSubjects().get(0).getTitle());
            }

            @Override
            public void error(VolleyError volleyError) {
                Log.i("JsonRequest", "异常结果" + volleyError.toString());
            }
        });
    }

    /**
     * 测试SingleFileRequest,实现文件上传
     */
    private void testUploadFile() {
        //手机上的文件，这里，本地模拟器上存在的文件
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "BaiHeWang.apk";
        //网络的url,这里，本地Eclipse中tomcat服务器上的地址
        String url = "http://192.168.1.8:8080/SSMProject/file/fileUpload";
        VolleyClient.getInstance().sendSingleFileRequest(TAG, url, filePath,
                new FileProgressListener() {
                    @Override
                    public void progress(int progress) {
                        Log.i("SingleFileRequest", "上传进度 " + progress);
                    }
                }, new GsonResultListener<FileBean>() {
                    @Override
                    public void success(FileBean fileBean) {
                        Log.i("SingleFileRequest", " 上传返回路径：" + fileBean.toString());
                    }

                    @Override
                    public void error(VolleyError volleyError) {
                        Log.i("SingleFileRequest", " 上传失败 " + volleyError.toString());
                    }
                });
    }
}
