package com.battlelab.api.v1;

import com.battlelab.Constant;
import com.battlelab.TraceHelper;
import com.battlelab.api.DownloadUriPrifixHelper;
import com.battlelab.api.JwtTokenHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UrlGenerator implements DownloadUriPrifixHelper {

    private static final int GET_URL_EXPIRED_SECONDS = 30;

    private static Gson gson = new Gson();

    @FunctionName("UrlGenerator")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req",
            methods = {HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "{version}/UrlGenerator")
            HttpRequestMessage<Optional<String>> request,
        @BindingName("version") String version,
        final ExecutionContext context) throws UnsupportedEncodingException {
        long start = System.currentTimeMillis();

        TraceHelper trace = new TraceHelper(context.getLogger());

        trace.trace("UrlGenerator : starts.");

        Optional<String> body = request.getBody();

        if (!body.isPresent()) {
            trace.trace("UrlGenerator : no request body.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("no body.").build();
        }

        String eTag = request.getHeaders().get("etag");

        if (eTag == null || eTag.isEmpty()) {
            trace.trace("UrlGenerator : no ETag.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("no ETag.").build();
        }

        String bodyString = body.get();

        JsonObject jsonObject = gson.fromJson(bodyString, JsonObject.class);
        String tag = jsonObject.get(Constant.Downloader.TAG).getAsString();
        String abi = jsonObject.get(Constant.Downloader.ABI).getAsString();
        int versionInt = Integer.parseInt(version.substring(1));
        long timestamp = jsonObject.get(Constant.DeployTask.TIMESTAMP).getAsLong();

        if (tag == null || tag.isEmpty() || tag.length() > Constant.MAX_TAG_LENGTH) {
            trace.trace("UrlGenerator : no tag.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Please pass a tag on the query string or in the request body").build();
        }

        if (abi == null || !validAbi(abi)) {
            trace.trace("UrlGenerator : no abi.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Please pass a abi on the query string or in the request body").build();
        }

        if ((System.currentTimeMillis() - timestamp) > GET_URL_EXPIRED_SECONDS * 1000) {
            trace.trace("UrlGenerator : timeout.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("timeout.").build();
        }

        String eTag1 = Sha256Helper.getETag(bodyString, SecretHelper.makeSecret(tag, versionInt));

        if (!eTag.equals(eTag1)) {
            trace.trace("UrlGenerator : wrong ETag.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("wrong ETag.").build();
        }

        Map<String, Serializable> values = new HashMap<>();
        values.put(Constant.Downloader.TAG, tag);
        boolean x64 = Constant.Downloader.ABIS_64.contains(abi.toLowerCase());
        values.put(Constant.Downloader.IS_X64, x64);

        String withTag = JwtTokenHelper.create(values, Constant.Downloader.TOKEN_EXPIRED_SECONDS);
        trace.info(String.format("UrlGenerator : tag %s , abi %s", tag, abi));
        //String s = Base64.getEncoder().encodeToString(withTag.getBytes());
        String s = URLEncoder.encode(withTag, "UTF-8");
        String prefix = getPrifix(versionInt);
        String formatter = prefix.endsWith("/") ? "http://%s%s" : "http://%s/%s";

        HttpResponseMessage response = request.createResponseBuilder(HttpStatus.OK)
            .body(String.format(formatter, prefix, s)).build();
        trace.trace("UrlGenerator : cost " + (System.currentTimeMillis() - start));
        return response;
    }

    private boolean validAbi(String abi) {
        if (abi.isEmpty()) {
            return false;
        }
        return Constant.Downloader.ABIS.contains(abi.toLowerCase());
    }

}
