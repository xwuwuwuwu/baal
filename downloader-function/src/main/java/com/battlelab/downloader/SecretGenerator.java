package com.battlelab.downloader;

import com.battlelab.Constant;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SecretGenerator {

    private static final int CURRENT_VERSION = 1;

    @FunctionName("secret")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "v1/secret")
                    HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        String tag = request.getQueryParameters().get("tag");
        if (tag == null || tag.isEmpty() || tag.length() > Constant.MAX_TAG_LENGTH) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a tag on the query string or in the request body").build();
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(SecretHelper.makeSecret(tag, CURRENT_VERSION)).build();
    }

    @FunctionName("doc")
    public HttpResponseMessage makeDoc(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "v1/doc")
                    HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws IOException {
        String tag = request.getQueryParameters().get("tag");
        if (tag == null || tag.isEmpty() || tag.length() > Constant.MAX_TAG_LENGTH) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a tag on the query string or in the request body").build();
        }

        String secret = SecretHelper.makeSecret(tag, CURRENT_VERSION);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        ZipEntry secretEntry = new ZipEntry("secret.txt");
        zipOutputStream.putNextEntry(secretEntry);
        zipOutputStream.write(String.format("tag=%S\nsecret=%s\n",tag,secret).getBytes());
        ZipEntry rEntry = new ZipEntry("readme.md");
        zipOutputStream.putNextEntry(rEntry);
        InputStream resourceAsStream = getClass().getResourceAsStream("/doc.md");
        byte[] buffer = new byte[2048];
        int readLen = 0;
        while ((readLen = resourceAsStream.read(buffer))!= -1){
            zipOutputStream.write(buffer,0,readLen);
        }
        resourceAsStream.close();
        zipOutputStream.close();
        byte[] bytes = outputStream.toByteArray();
        outputStream.close();

        return request.createResponseBuilder(HttpStatus.OK)
                .body(bytes).build();
    }

}