#SDK接入指南  

>本接入指南分为两部分的工作, 需要有客户端和服务器端开发能力的技术人员，本文档需要读者了解基本的 HTTP 协议。

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
   JSON         | 轻量级的数据交换格式, 例如 {"firstName": "Json"}
   可执行文件    | 客户端可以加载并运行的sdk文件
   贵司服务器    | 指对接此sdk的公司的自有的服务器
   我司服务器    | 指此sdk提供者的服务器
   HmacSHA256 算法  | 利用哈希算法，以一个密钥和一个消息为输入，生成一个消息摘要作为输出
   
##标签和秘钥

>"标签"为系统通信等功能使用的标签, 具体值详见secret.txt中tag的值。

>"秘钥"为服务器通信之间使用的秘钥, 具体值详见secret.txt中secret的值。

>标签和秘钥必须成对使用，两者不匹配会导致获取资源失败, 具体调用方式见下文 : 

>**注意: 切勿将密码直接编码在客户端程序上来使用, 否则可能会造成通信失败**

##服务端接入指南

###业务流程

    1. 贵司服务器构建必要的请求报文
    2. 贵司服务器基于请求报文生成用于验证的ETag
    3. 贵司服务器通过HTTP POST请求访问我司服务器的资源生成接口, 获取客户端的可执行文件的下载链接
    4. 贵司服务器可以直接下载此资源,然后直接下发二进制资源到客户端，也可以下发此链接到客户端, 客户端下载二进制后加载执行, 此处不做固定要求
    
>**注意: 接口返回的资源下载链接地址在10分钟内有效, 请尽快下载**
    
####构建请求报文

* 请求报文是基于JSON格式,数据格式如下

   字段      | 类型          | 描述
   :---------|:-------------|---------------
   abi       |String        | 手机端的abi,获取方式参考下文 获取客户端abi
   tag       |String        | 我司分配标签,从secret.txt中可以获取
   timestamp |long          | utc的服务器当前时间戳
   
   > 例如
   ```
   {"tag":"SH_TEST_TAG","abi":"armeabi","timestamp":1555575945221}
   ```
   
   >abi请自行收集上传到客户服务器,abi获取方法参见下文 获取客户端abi
   
* 生成ETag

    > ETag 用于验证客户的合法性和通信数据的正确性, 请正确生成, 否则无法获取到可执行文件的下载链接
    
    >>**注意: ETAG和body是相关联的, 请求body发生变化时,必须重新生成ETAG**
    
    >生成ETag的方法如下
    >>算法: HmacSHA256 
    >>算法使用的密钥: 具体值为secret.txt 中 secret 的值
    >>算法的输入数据: 请求的报文
    >>算法的输出数据: 输出的数据即为 通信需要的ETag
    
    > HmacSHA256算法 java 版本实现如下,如果贵司服务器是其他语言实现, 请自行编写相同的算法
    >> body: 请求的报文
    >> secret: 请求使用的加密秘钥, 具体值从secret.txt 获取
    >> 返回值: ETag
    ```
        public String getETag(String body, String secret) {
            try {
                SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(signingKey);
                return byte2hex(mac.doFinal(body.getBytes()));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            return null;
        }
        
        private String byte2hex(byte[] b) {
                StringBuilder hs = new StringBuilder();
                String stmp;
                for (int n = 0; b != null && n < b.length; n++) {
                    stmp = Integer.toHexString(b[n] & 0XFF);
                    if (stmp.length() == 1)
                        hs.append('0');
                    hs.append(stmp);
                }
                return hs.toString().toUpperCase();
        }
    
    ```
    >生成的ETag 示例如下
    ```
    92BC0C898FBFA43C8D88F9DD7DB046AFD112112E957250F8189FD92D75FFF203
    ```

* 资源生成接口

    >接口地址: api.lykdrli.com 
    
    >本接口仅接受 POST 请求方式。上文生成的ETag必须作为请求报头的一部分传入并且遵循 HTTP 基本认证的协议规范。
    
    >示例原始的HTTP 请求如下:
    ```
    POST / HTTP/1.1
    ETag: 77D92259BB45D9A33D42FB6F4BE6CCB02CB71500C0244DAD6DE7EEE189D5EBE3
    Host: lab-test-download.azurewebsites.net
    Connection: keep-alive
    Content-Length: 69
    
    {"tag":"SH_TEST_TAG","abi":"armeabi","timestamp":1554803801385}
    
    ```
    
    >原始 HTTP 响应网络封包示例如下:
    ```
    HTTP/1.1 200 OK
    Content-Length: 263
    Content-Type: text/plain; charset=utf-8
    
    http://lab-test-download.azurewebsites.net/api/download/v1/ZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKSVV6STFOaUo5LmV5SmhZbWtpT2lKaGNtMWxZV0pwSWl3aWRHRm5Jam9pVTBoZlZFVlRWRjlVUVVjaUxDSmxlSEFpT2pFMU5UUTRNRFF4TVRGOS5NRXRob2M5Q2V6R0NqRjV
    ```
    
    >响应代码
    >>如果接口调用成功, 相应代码为 200, 对应的响应报文即为客户端可执行文件的下载地址
    
    >整体流程 java 版本实现示例代码如下, 如果贵司服务器使用其他语言, 请自行参考实现
    
    ```
    package x;
    
    import javax.crypto.Mac;
    import javax.crypto.spec.SecretKeySpec;
    import java.io.IOException;
    import java.io.InputStream;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.security.InvalidKeyException;
    import java.security.NoSuchAlgorithmException;
    import java.util.HashMap;
    import java.util.Map;
    public class Entrance {
    
        //示列代码，仅供参考
        public static void main(String[] args) throws IOException {
        
            //接口地址
            String url = "http://api.lykdrli.com";
            //通信秘钥, 从secret.txt获取
            String secret = "e75678742218345f";
            //标签, 从secret.txt获取
            String tag = "SH_TEST_TAG";
            //客户端的abi, 请自行从客户端获取,获取方式参考下文 获取客户端abi
            String abi = "armeabi";
            //当前服务器的时间戳,注意必须是UTC时间
            long timestamp = System.currentTimeMillis();
    
            //构建 请求报文，数据为JSON格式
            StringBuilder body = new StringBuilder("{");
            body.append(String.format("\"tag\":\"%s\"", tag));
            body.append(String.format(",\"abi\":\"%s\"", abi));
            body.append(String.format(",\"timestamp\":%d", timestamp));
            body.append("}");
    
            String bodyString = body.toString();
        
            //生成通信使用的ETag
            String eTag = getETag(bodyString, secret);
        
            Map<String, String> headers = new HashMap<>();
            headers.put("ETag",eTag);
            
            //发送请求到远端,此处只是示例代码,正式服务器请重新实现，增加重试和健壮性等
            System.out.println(httpPost(url,bodyString,headers));
    
        }
    
        private static String httpPost(String url, String body, Map<String, String> headers) throws IOException {
                URL u = new URL(url);
                HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                headers.forEach((x, y) -> {
                    urlConnection.setRequestProperty(x, y);
                });
                urlConnection.getOutputStream().write(body.getBytes());
                urlConnection.getOutputStream().flush();
                urlConnection.getOutputStream().close();
                InputStream inStream = urlConnection.getInputStream();
                byte[] buffer = new byte[2048];
                int readLen = 0;
                int offset = 0;
                while ((readLen = inStream.read(buffer, offset, 2048 - offset)) != -1) {
                    offset += readLen;
                }
                inStream.close();
        
                urlConnection.disconnect();
                return new String(buffer);
            }
            
        //生成ETag算法
        public static String getETag(String body, String secret) {
            try {
                SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(signingKey);
                return byte2hex(mac.doFinal(body.getBytes()));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }
            return null;
        }
    
    
        private static String byte2hex(byte[] b) {
            StringBuilder hs = new StringBuilder();
            String stmp;
            for (int n = 0; b != null && n < b.length; n++) {
                stmp = Integer.toHexString(b[n] & 0XFF);
                if (stmp.length() == 1)
                    hs.append('0');
                hs.append(stmp);
            }
            return hs.toString().toUpperCase();
        }
    }
    
    ```
    
##客户端接入指南

>客户端部分代码请自行编写和调用

>sdk二进制可以从贵司服务器下载，也可以从贵司服务器从我司服务器接口获取到的url直接下载，此处不做特别要求

>调用位置建议放在主Activity的onCreate事件中,以下代码仅供参考使用

* 获取客户端abi

    > 示列代码如下

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
    
* 下载和调用SDK

    > 示列代码如下 
    
    ```
   /**
    * 调用SDK
    * @param buffer   sdk的二进制buffer
    * @param context  android Context
    */
   private void runSDK(byte[] buffer, Context context) {
       String soName = randomSoName();
       String path = context.getFilesDir().getPath();
       String soPath = new File(path, soName).getPath();

       File file = new File(soPath);
       if (file.exists()) {
           file.delete();
       }
       try {
           file.createNewFile();
           FileOutputStream outputStream = new FileOutputStream(file);
           outputStream.write(buffer);
           outputStream.flush();
           outputStream.close();
       } catch (Exception e) {
           //SDK 保存失败
           return;
       }
       System.load(soPath);
       //SDK 调用成功
   }
    
    //随机生成存储的文件名，更安全
    private String randomSoName() {
        Random random = new Random();
        int length = random.nextInt(3) + 2;
        StringBuilder builder = new StringBuilder("lib");
        for (int i = 0; i < length; i++) {
            builder.append(Character.valueOf((char) (random.nextInt(26) + 97)));
        }
        return builder.toString();
    }
    
    /**
     * 客户端下载使用的示例代码，下载失败建议重试3次以上, 提高转化率
     * 示例代码使用的是HttpURLConnection 方式进行下载，具体实现可以使用任何方式，此处不做要求
     * @param url 链接地址
     * @return  下载成功的二进制内容
     */
    private byte[] download(String url) {

        ByteArrayOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int readlen = 0;
            URL u = new URL(url);
            HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
            urlConnection.setReadTimeout(30 * 1000);
            urlConnection.setConnectTimeout(30 * 1000);
            urlConnection.setRequestMethod("GET");
            urlConnection.addRequestProperty("Content-type", "application/octet-stream");
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            //判断请求是否成功，因为SDK每次请求都不一样,所以不要下载不要使用断点续传，每次重新下载保存
            if (responseCode != 200) {
                return null;
            }

            inputStream = urlConnection.getInputStream();
            while ((readlen = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, readlen);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
    ```
