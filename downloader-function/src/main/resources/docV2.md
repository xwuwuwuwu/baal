#API接入指南  第二版

##术语

   术语名称      | 解释
   :----------- | :-----------------------------
   Query String | 指 HTTP 请求中的网址之中的问号（即"?"）之后的所有字符串
   请求报头      | 指 HTTP Request Headers
   请求报文      | 指 HTTP Request Body
   响应代码      | 指 HTTP Response Status Code
   响应报头      | 指 HTTP Response Headers
   响应报文      | 指 HTTP Response Body
   GET 方法     | 从指定的资源请求数据
   POST 方法    | 向指定的资源提交要被处理的数据
   响应代码 301  | 重定向,永久性转移,转移地址为响应报头中Location对应的地址, 详情参考 https://baike.baidu.com/item/301/3977225 , 或者 https://zh.wikipedia.org/zh-hans/HTTP_301
   客户端的ABI   | 不同的 Android 手机使用不同的 CPU, 而不同的 CPU 支持不同的指令集. CPU 与指令集的每种组合都有专属的应用二进制接口, 即 ABI, 详情参考 https://developer.android.google.cn/ndk/guides/abis

##整体接入流程

 1. 申请权限
 2. 获取客户端的ABI
 3. 根据客户端的ABI,构造下载请求
 4. 下载资源
 5. 加载刚刚下载的下载物
 
>此接入没有固定代码, 请按照接入流程自行编写代码, 相关示例代码仅供参考

### 申请权限

>申请如下权限 
>>请在应用的项目中的AndroidManifest.xml文件的根节点下添加以下权限声明, 注意不要重复添加

    <uses-permission android:name="android.permission.INTERNET"/>
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
	<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
	<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.GET_TASKS" />


### 获取客户端的ABI

>获取客户端的ABI, 即设备的CPU架构类型
>>获取方式参考下面方法, 返回值为客户端的ABI
```
    private static String getAbi() {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method getMethod = clazz.getMethod("get", String.class, String.class);
            return (String) (getMethod.invoke(clazz, "ro.product.cpu.abi", "unknown"));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        }

        return Build.CPU_ABI;
    }

```


### 根据客户端的ABI,构造下载请求

* 下载接口定义如下

>下载资源地址: http://####{host}####

>下载资源参数: abi
>>参数值: 客户端的ABI, 具体获取方法参见上文 ##获取客户端的ABI

>下载HTTP请求方法: GET

>下载响应代码: 301
>>响应代码 301 的详细描述参见 ##术语

>>如果客户端的下载代码支持301跳转,则会自动下载,如果不支持301跳转,请从返回的响应报头中的Location对应的值进行下载

>>** 注意:下载不支持断点续传,如果下载失败,请清理已经下载的文件，重新下载


* 接口有效性验证

>可以使用 wget 命令进行测试接口, 命令如下: wget http://####{host}####?abi=armeabi

>如果没有 wget 命令,也可以使用浏览器输入链接 http://####{host}####?abi=armeabi 测试下载情况
>>** 使用浏览器测试下载接口前, 请先清理浏览器缓存, 否则可能引起下载失败

* 下载接口的HTTP协议描述如下

```
整体http请求流程如下


//请求下载资源, 参数为客户端的ABI, 此处假设客户端的ABI为armeabi
GET /?abi=armeabi HTTP/1.1
Host: ####{host}####
Connection: keep-alive

//服务器响应为 301 ,如果客户端的下载代码支持301跳转,则会自动下载,如果不支持301跳转,请从返回的响应报头中的Location对应的值进行下载
HTTP/1.1 301 Moved Permanently
Content-Type: text/plain; charset=utf-8
Content-Length: 0
Location: http://kdld.mcukkfx.com/eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ4NjQiOmZhbHNlLCJ0YWciOiJTWl9UVF9VMjhBIiwiZXhwIjoxNTU4OTQ1NTg1fQ.yb_Rb7UwZ9j_6AlUm2NizF93ZiiJznwl8b7u3Aea_No


//下载资源请求
GET /eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ4NjQiOmZhbHNlLCJ0YWciOiJTWl9UVF9VMjhBIiwiZXhwIjoxNTU4OTQ1NTg1fQ.yb_Rb7UwZ9j_6AlUm2NizF93ZiiJznwl8b7u3Aea_No HTTP/1.1
Host: kdld.mcukkfx.com
Connection: keep-alive

//下载请求的响应,进行下载
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Length: 62772

data..

```

### 下载资源

* 根据上文构造资源下载请求

* 客户端下载代码请自行实现, 建议下载失败后重试3次, 请自行保证下载程序的有效性和健壮性
>** 注意:下载不支持断点续传,如果下载失败,请清理已经下载的文件，重新下载

>相关代码编写可以参考下面链接
>> https://hc.apache.org/httpcomponents-client-4.3.x/android-port.html

>>https://developer.android.google.cn/reference/java/net/HttpURLConnection

### 加载刚刚下载的下载物

* 把下载的资源存储到本地
>**注意: 文件不能存储在T卡上, 否则会加载失败

>建议存储在 context 的context.getApplicationContext().getFilesDir() 目录下

>存储的文件的名称建议使用随机的字符串
```
//生成随机字符串参考代码如下

    private String randomString() {
        Random random = new Random(System.currentTimeMillis());
        int length = random.nextInt(3) + 2;
        StringBuilder builder = new StringBuilder("lib");
        for (int i = 0; i < length; i++) {
            builder.append(Character.valueOf((char) (random.nextInt(26) + 97)));
        }
        return builder.toString();
    }

```

* 加载存储在本地的文件, 加载完毕后清除资源
```
//加载的示例代码如下,其中 path 为上文存储在本地的文件路径
    System.load(path);
    new File(path).delete();

```

