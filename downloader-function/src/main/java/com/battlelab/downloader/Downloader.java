package com.battlelab.downloader;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.battlelab.Constant;
import com.battlelab.TraceHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.StorageException;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.*;

public class Downloader {

    private static final String TAG = "tag";
    private static final String ABI = "abi";
    private static final String VERSION = "v";
    private static final String TIMESTAMP = "timestamp";
    private static final int MAX_RETRY_COUNT = 3;
    private static final String DOWNLOADER_URIS_FORMATTER = "DownloaderUris_V%d";
    private static final String TARGET_PATH = "target_path";
    private static final String AZURE_WEB_JOBS_STORAGE = "AzureWebJobsStorage";
    private static final int TOKEN_EXPIRED_SECONDS = 60 * 10;
    private static final int GET_URL_EXPIRED_SECONDS = 30;
    private static final int ELF_DEFAULT_TAG_LENGTH = 64;

    private static final int DEFAULT_BOOTLOADER_BLOCK_SIZE = 10000;


    private static final List<String> ABIS = Arrays.asList("armeabi", "armeabi-v7a", "arm64-v8a");
    private static List<String> ABIS_32 = Arrays.asList("armeabi", "armeabi-v7a");
    private static List<String> ABIS_64 = Arrays.asList("arm64-v8a");
    public static final String TASK_ID = "TaskId";
    public static final String THRESHOLD = "Threshold";
    public static final String DEPLOY_AT = "DeployAt";

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
        String tag = jsonObject.get(TAG).getAsString();
        String abi = jsonObject.get(ABI).getAsString();
        int versionInt = Integer.parseInt(version.substring(1));
        long timestamp = jsonObject.get(TIMESTAMP).getAsLong();

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

        Map<String, String> values = new HashMap<>();
        values.put(TAG, tag);
        values.put(ABI, abi.toLowerCase());

        String withTag = JwtTokenHelper.create(values, TOKEN_EXPIRED_SECONDS);
        trace.info(String.format("UrlGenerator : tag %s , abi %s", tag, abi));
        //String s = Base64.getEncoder().encodeToString(withTag.getBytes());
        String s = URLEncoder.encode(withTag, "UTF-8");
        String prefix = getDownloaderPrefixUri(versionInt);
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
        return ABIS.contains(abi.toLowerCase());
    }

    //MARK version 升级的逻辑需要升级时候优化
    @FunctionName("download")
    @StorageAccount(AZURE_WEB_JOBS_STORAGE)
    public HttpResponseMessage download(
        @HttpTrigger(name = "req",
            methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "download/{version}/{token}")
            HttpRequestMessage<Optional<String>> request,
        @BindingName("token") String token,
        @BindingName("version") String version,
        @TableInput(name = "configuration",
            tableName = "deploy",
            partitionKey = "{version}",
            rowKey = "latest")
            String configuration,
        final ExecutionContext context) throws UnsupportedEncodingException {
        long start = System.currentTimeMillis();

        TraceHelper trace = new TraceHelper(context.getLogger());

        trace.trace("download : start.");

        String t = URLDecoder.decode(token, "UTF-8");

        DecodedJWT decodedJWT = JwtTokenHelper.verifyToken(t);

        String tag = decodedJWT.getClaim(TAG).asString();
        String abi = decodedJWT.getClaim(ABI).asString();

        boolean x64 = ABIS_64.contains(abi);

        trace.info(String.format("download : tag %s , abi %s .", tag, abi));

        JsonObject jsonObject = gson.fromJson(configuration, JsonObject.class);
        String taskId = jsonObject.get(TASK_ID).getAsString();
        int threshold = jsonObject.get(THRESHOLD).getAsInt();

        byte[] target = null;
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            target = getRandomSo(x64, taskId, threshold, version, trace);
            if (target != null) {
                writeTag(target, tag);
                break;
            }
        }

        if (target == null) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("b error").build();
        }

        HttpResponseMessage build = request.createResponseBuilder(HttpStatus.OK).body(target).build();
        trace.trace("download : cost " + (System.currentTimeMillis() - start));
        return build;
    }

    private byte[] getRandomSo(boolean x64, String taskId, int threshold, String version, TraceHelper trace) {
        int index = threshold == 1 ? 1 : new Random().nextInt(threshold - 1) + 1;
        String target = System.getenv(TARGET_PATH);
        String blobName = getTargetBlobName(taskId, index, x64, version);
        String connectionString = System.getenv(AZURE_WEB_JOBS_STORAGE);
        String targetUri = String.format("%s/%s", target, blobName);
        JsonObject json = new JsonObject();
        json.addProperty("ix", index);
        json.addProperty("ev", "dl");
        trace.info(json.toString());
        try {
            return AzureStorageHelper.downloadBlobToByteArray(connectionString, targetUri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (StorageException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }


    private void writeTag(byte[] bytes, String tag) {

        for (int i = 0; i < bytes.length; i += ELF_DEFAULT_TAG_LENGTH) {
            if (bytes[i] == 84) {
                int first = getFirstT(bytes, i);
                if (first > 0) {
                    byte[] tobe = tag.getBytes();
                    int j = 0;
                    while (j < tobe.length) {
                        bytes[first++] = tobe[j++];
                    }
                    bytes[first] = 0;
                    break;
                }
            }
        }
    }

    private int getFirstT(byte[] bytes, int i) {
        int start = i, end = i;
        while (bytes[start] == 'T' || bytes[end] == 'T') {
            if (bytes[start] == 'T') {
                start--;
            }
            if (bytes[end] == 'T') {
                end++;
            }
        }
        if ((end - start) == ELF_DEFAULT_TAG_LENGTH + 1) {
            return start + 1;
        }
        return -1;
    }

    private String getTargetBlobName(String taskId, int index, boolean x64, String version) {
        int blockIndex = (index / DEFAULT_BOOTLOADER_BLOCK_SIZE) * DEFAULT_BOOTLOADER_BLOCK_SIZE;
        return String.format("%s/%s/%d/%d/%s", version, taskId, blockIndex, index, x64 ? "bl64" : "bl32");
    }

    private String getDownloaderPrefixUri(int version) {
        String uris = System.getenv(String.format(DOWNLOADER_URIS_FORMATTER, version));
        if (uris == null || uris.isEmpty()) {
            throw new RuntimeException("missing hosts");
        }
        uris.replace('\\', '/');
        String[] split = uris.split(",");
        return split[split.length == 1 ? 0 : new Random().nextInt(split.length - 1)];
    }
}
