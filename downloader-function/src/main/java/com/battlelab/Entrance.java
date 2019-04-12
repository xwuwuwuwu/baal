package com.battlelab;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Entrance {

    //示列代码，仅供参考
    public static void main(String[] args) throws IOException {
//        String url = "http://lab-test-download.azurewebsites.net/api/UrlGenerator";
        String url = "http://127.0.0.1:7071/UrlGenerator";
        String secret = "e75678742218345f";
        String tag = "SH_TEST_TAG";
        String abi = "armeabi";
        long timestamp = System.currentTimeMillis();


        StringBuilder body = new StringBuilder("{");
        body.append(String.format("\"tag\":\"%s\"", tag));
        body.append(String.format(",\"abi\":\"%s\"", abi));
        body.append(String.format(",\"timestamp\":%d", timestamp));
        body.append(String.format(",\"v\":1", tag));
        body.append("}");

        String bodyString = body.toString();

        System.out.println(bodyString);

        String eTag = getETag(bodyString, secret);

        System.out.println("etag " + eTag);

        Map<String, String> headers = new HashMap<>();
        headers.put("ETag",eTag);

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
        inStream.read(buffer);
        inStream.close();

        urlConnection.disconnect();
        return new String(buffer);
    }

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
