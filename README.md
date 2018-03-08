
**简介**：

>VolleyLib框架是一个集合`Volley 网络通讯库`, `Gson解析库` ，`org.apache.http部分源码`的定制化网络框架 , 具备文件上传、文件下载、Form表单、Json数据协议、Gson 解析Json生成泛型实体等功能。


在实际开发中，网络请求的body是文本或者文件。在Android开发中，通常处理文本，图片，文件等多种数据。

根据这些需求，定制化了以下几种Request :

- **CircleBitmapImageRequest** ：圆形图片的Request , 默认添加网络图片的有效期和过期时间。

- **FormRequest**： 类似web端的form表单Request，支持内容格式`application/x-www-form-urlencoded`,Gson解析生成对应的泛型实体类。

- **GsonRequest**：Json数据协议的Request，支持内容格式`application/json` , Gson解析生成对应的泛型实体类。

- **DownloadRequest** :  文件下载的Request , 不走内存操作，直接将response输出到文件中。 

-  **SingleFileRequest**：文件上传的Request , 不走内存操作，直接将文件写入。


---


### **使用介绍**：

**1. 在项目Module中libs下拷贝`VolleyLib.jar`**

**2. 在Application子类中初始化VolleyClient的操作**。
```
public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VolleyClient.getInstance().init(this);
    }
}
```
**3. 接下来 , 介绍各种Request的使用**

**3.1 使用CircleBitmapImageRequest**：

```
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
```
支持，默认图片，异常图片设置，主线程中，回调监听器上返回圆形Bitmap。

**3.2 使用FormRequest**： 
```
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
```
支持Header , Body 设置，Gson解析生成泛型实体类。

**3.3 使用GsonRequest**:

```
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
```
支持传递JsonObject和实体对象的post参数，支持设置Header,Gson解析生成泛型实体类。

**3.4 使用SingleFileRequest**:
```
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
```
支持Body中传递文件路径或者文件对象 , 进度监听器回调进度， Gson解析实体类 , 生成带有图片路径的Bean对象。


同时,也支持指定name和filePath。需注意，这里name并非文件名。
```
public <T> SingleFileRequest sendSingleFileRequest(String tag, String url, String name, String filePath, FileProgressListener fileProgressListener, GsonResultListener<T> resultListener) {
    
}
```

**3.5 使用DownlaodRequest**:
```
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
```

指定下载的Url和存储文件的路径, 进度监听器回调进度，结果监听器回调异常和成功的信息。


Volley中修改的部分源码
---

**1. HurlStack类**：

在这里添加，针对文件上传的操作，避免直接生成byte数组，占用巨大内存。

```
 /**
     *
     * 添加文件，避免内存巨大
     *
     * @param connection
     * @param request
     * @throws IOException
     * @throws AuthFailureError
     */
    private static void addFileBody(HttpURLConnection connection, SingleFileRequest request) throws IOException, AuthFailureError {
        String filePath = request.getFilePath();
        if (filePath == null) {
            throw new IOException("File文件路径为空");
        }
        File file = new File(filePath);
        if (file == null || !file.exists()) {
            throw new IOException("File 文件不存在");
        }
        //设置post请求方法，允许写入客户端传递的参数
        connection.setDoOutput(true);
        //设置标头的Content-Type属性
        connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        FileInputStream fileInputStream = new FileInputStream(file);
        long total = 0;
        int read ;
        outputStream.writeUTF(request.getContentHeader());
        byte[] buffer = new byte[SingleFileRequest.READ_SIZE];
        while ((read = fileInputStream.read(buffer)) != -1) {
            if (request.isCanceled()){
                fileInputStream.close();
                outputStream.close();
                throw  new IOException("SingleRequest 被取消");
            }
            outputStream.write(buffer, 0, read);
            outputStream.flush();
            total += read;
            int progress = (int) (total * 100 / file.length());
            request.deliverProgress(progress);
        }
        outputStream.writeUTF(request.getContentFoot());
        outputStream.flush();
        fileInputStream.close();
        outputStream.close();
    }
```

**2. BasicNetwork类**：

这里避免使用`PoolingByteArrayOutputStream`将文件直接生成`byte数组`，导致内存溢出。采用`FileOutputStream `生成指定路径的文件。
```
  private static NetworkResponse writeFileStreamIfExist(DownloadRequest request, Map<String, String> responseHeaders, HttpResponse httpResponse) throws IOException {
        Log.i("DownloadRequest" ,"下载");
        File file=new File(request.getFilePath());
        if (file!=null&&file.exists()){
            file.delete();
        }
        InputStream inputStream=httpResponse.getEntity().getContent();
        FileOutputStream outputStream=new FileOutputStream(file);
        long fileLength = httpResponse.getEntity().getContentLength();
        byte[] buffer = new byte[4096];
        int count;
        long total = 0;
        while ((count = inputStream.read(buffer)) != -1) {
            if (request.isCanceled()){
                outputStream.close();
                inputStream.close();
                file.deleteOnExit();
                throw  new IOException("DownloadRequest cancel 被取消");
            }
            outputStream.write(buffer, 0, count);
            total += count;
            if (fileLength > 0) {
                int progress = (int) ((float) total * 100 / fileLength);
                request.deliverProgress(progress);
            }
        }
        outputStream.flush();
        inputStream.close();
        outputStream.close();
        return new NetworkResponse(httpResponse.getStatusLine().getStatusCode(), new byte[0], responseHeaders, false);
    }
```
---

**代码资源**：

- **Volley库** : https://github.com/google/volley

- **Gson库**： https://github.com/google/gson


