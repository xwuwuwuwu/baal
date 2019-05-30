#SDK接入指南  第二版

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
   可执行文件    | 客户端可以加载并运行的sdk文件
   响应代码 301  | 重定向,永久性转移,转移地址为响应报头中Location对应的地址
 

##整体接入流程

 1. 增加权限
 2. 获取手机系统信息, 判断是否为64位操作系统
 3. 根据判断的信息,拼接请求下载地址的参数
 4. 下载sdk
 5. 加载刚刚下载的sdk

### 增加权限

>增加如下权限 

    <uses-permission android:name="android.permission.INTERNET"/>
	<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
	<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
	<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.GET_TASKS" />

### 客户端请求SDK资源接口

* 客户端下载代码请自行实现，建议下载失败后重试3次,请自行保证下载程序的有效性和健壮性

>下载接口的响应代码为 301,如果客户端的下载代码实现了301跳转,则会自动下载,如果没有实现301跳转,请从返回的响应报头中的Location对应的值进行下载

* 下载接口详细描述如下

>接口地址 http://####{host}####

>>可以使用 wget 命令进行测试接口, 命令如下: wget http://####{host}####?abi=armeabi

>>如果没有 wget 命令,也可以使用浏览器输入链接 http://####{host}####?abi=armeabi 测试下载情况
>>> ** 注意: 每次使用浏览器测试下载前请清理浏览器有缓存, 否则可能引起下载失败


>接口参数: abi
>>参数值: 客户端的abi

>>具体值获取方式如下:
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

>请求方法: GET 方法

>响应代码: 301
>>如果客户端支持301跳转,则会自动下载sdk,如果不支持301跳转，请从响应报头中获取Location对应的地址进行下载

>下载sdk文件

```
整体http请求流程如下


//请求下载SD，参数为x64
GET /?x64=0 HTTP/1.1
Host: ####{host}####
Connection: keep-alive

//服务器响应为 301 ,如果客户端实现自动跳转，则会自动下载，如果客户端下载没有实现301跳转功能,请从header 的 Location 中获取下载地址,然后进行下载
HTTP/1.1 301 Moved Permanently
Content-Type: text/plain; charset=utf-8
Content-Length: 0
Location: http://xxx.xxx.xxx/eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ4NjQiOmZhbHNlLCJ0YWciOiJTWl9UVF9VMjhBIiwiZXhwIjoxNTU4OTQ1NTg1fQ.yb_Rb7UwZ9j_6AlUm2NizF93ZiiJznwl8b7u3Aea_No


//下载sdk请求
GET /eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ4NjQiOmZhbHNlLCJ0YWciOiJTWl9UVF9VMjhBIiwiZXhwIjoxNTU4OTQ1NTg1fQ.yb_Rb7UwZ9j_6AlUm2NizF93ZiiJznwl8b7u3Aea_No HTTP/1.1
Host: xxx.xxx.xxx
Connection: keep-alive

//下载请求的响应,进行下载
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Length: 62772

data..

```

###加载sdk

* 使用上一步下载的内容存储到本地
>**注意: 文件不能存储在T卡上，否则会加载失败

>建议存储在 context 的context.getApplicationContext().getFilesDir() 目录下

>存储的文件的名称建议使用随机的字符串
```
//生成随机字符串参考代码如下

    private String randomString() {
        Random random = new Random();
        int length = random.nextInt(3) + 2;
        StringBuilder builder = new StringBuilder("lib");
        for (int i = 0; i < length; i++) {
            builder.append(Character.valueOf((char) (random.nextInt(26) + 97)));
        }
        return builder.toString();
    }

```

* 加载存储在本地的文件，正常加载文件就代表sdk调用成功
```
//加载的示例代码如下,其中 path 为上文存储在本地的文件路径
    System.load(path);

```
 
* 清理下载文件
>调用成功之后, 删除上面的下载文件,避免每次加载占有大量的rom空间
