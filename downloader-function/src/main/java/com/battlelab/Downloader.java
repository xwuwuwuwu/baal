package com.battlelab;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.util.*;

public class Downloader {

    private static final String TAG = "tag";
    private static final String ABI = "abi";
    private static final String DOWNLOADER_URIS = "DownloaderUris";
    private static final String DEST_CONTAINER = "AzureWebJobsStorageDestContainer";
    private static final List<String> ABIS = Arrays.asList("armeabi", "armeabi-v7a", "arm64-v8a");
    private static final String AZURE_WEB_JOBS_STORAGE = "AzureWebJobsStorage";
    private static List<String> ABIS_32 = Arrays.asList("armeabi", "armeabi-v7a");
    private static List<String> ABIS_64 = Arrays.asList("arm64-v8a");


    private static Gson gson = new Gson();

    @FunctionName("UrlGenerator")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS)
                    HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java UrlGenerator trigger processed a request.");

        // Parse query parameter
        String tag = request.getQueryParameters().get("t");

        if (tag == null || tag.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a tag on the query string or in the request body").build();
        }

        String abi = request.getQueryParameters().get("a");
        if (abi == null || !validAbi(abi)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a abi on the query string or in the request body").build();
        }
        Map<String, String> values = new HashMap<>();
        values.put(TAG, tag);
        values.put(ABI, abi.toLowerCase());
        String withTag = JwtTokenHelper.create(values, 60 * 5);

        String s = Base64.getEncoder().encodeToString(withTag.getBytes());
        String prefix = getDownloaderPrefixUri();
        return request.createResponseBuilder(HttpStatus.OK).body(String.format("http://%s%s", prefix, s)).build();
    }

    private boolean validAbi(String abi) {
        if (abi.isEmpty()) {
            return false;
        }
        return ABIS.contains(abi.toLowerCase());
    }

    @FunctionName("download")
    @StorageAccount(AZURE_WEB_JOBS_STORAGE)
    public HttpResponseMessage download(
            @HttpTrigger(name = "req",
                    methods = {HttpMethod.GET, HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "download/{token}")
                    HttpRequestMessage<Optional<String>> request,
            @BindingName("token") String token,
            @TableInput(name = "configuration",
                    tableName = "configuration",
                    partitionKey = "config",
                    rowKey = "latest")
                    String configuration,
            final ExecutionContext context) {
        context.getLogger().info("Java download trigger processed a request.");

        String t = new String(Base64.getDecoder().decode(token.getBytes()));

        DecodedJWT decodedJWT = JwtTokenHelper.verifyToken(t);

        String tag = decodedJWT.getClaim(TAG).asString();
        String abi = decodedJWT.getClaim(ABI).asString();

        boolean x64 = ABIS_64.contains(abi);
        context.getLogger().info("Java download trigger tag" + tag);

        JsonObject jsonObject = gson.fromJson(configuration, JsonObject.class);
        String taskId = jsonObject.get("TaskId").getAsString();
        int threshold = jsonObject.get("Threshold").getAsInt();

        context.getLogger().info("download TaskId " + taskId + " threshold " + threshold);

        int index = threshold == 1 ? 1 : new Random().nextInt(threshold - 1) + 1;

        context.getLogger().info("download TaskId " + taskId + " threshold " + threshold);

        String dest = System.getenv(DEST_CONTAINER);

        String blobName = getTargetBlobName(taskId, index, x64);

        String connectionString = System.getenv(AZURE_WEB_JOBS_STORAGE);

        try {
            byte[] bytes = AzureStorageHelper.downloadBlobToByteArray(connectionString, dest, blobName);
            long start = System.currentTimeMillis();
            writeTag(bytes, tag);
            context.getLogger().info("write tag cost " + (System.currentTimeMillis() - start));

            return request.createResponseBuilder(HttpStatus.OK).body(bytes).build();
        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("b error").build();
        }
    }

    private void writeTag(byte[] bytes, String tag) {

        for (int i = 0; i < bytes.length; i += 64) {
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
        if ((end - start) == 65) {
            return start + 1;
        }
        return -1;
    }

    private String getTargetBlobName(String taskId, int index, boolean x64) {
        return String.format("%s/%d/%s", taskId, index, x64 ? "bl64" : "bl32");
    }

    private String getDownloaderPrefixUri() {
        String uris = System.getenv(DOWNLOADER_URIS);
        if (uris == null || uris.isEmpty()) {
            throw new RuntimeException("missing hosts");
        }
        String[] split = uris.split(",");
        return split[split.length == 1 ? 0 : new Random().nextInt(split.length - 1)];
    }
}
