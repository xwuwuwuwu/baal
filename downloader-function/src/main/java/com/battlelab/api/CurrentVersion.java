package com.battlelab.api;

import com.battlelab.Constant;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

public class CurrentVersion {

    @FunctionName("version")
    @StorageAccount(Constant.Downloader.AZURE_WEB_JOBS_STORAGE)
    public HttpResponseMessage version(
        @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "version")
            HttpRequestMessage<Optional<String>> request) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("/build.cfg");

        Properties properties = new Properties();
        properties.load(resourceAsStream);
        resourceAsStream.close();
        StringBuilder body = new StringBuilder();
        properties.forEach((x, y) -> {
            body.append(String.format("%s = %s \n", x, y));
        });

        return request.createResponseBuilder(HttpStatus.OK).body(body.toString()).build();
    }
}
