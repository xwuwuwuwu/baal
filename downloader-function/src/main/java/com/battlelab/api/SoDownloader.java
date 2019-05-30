package com.battlelab.api;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.battlelab.Constant;
import com.battlelab.TraceHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.StorageException;
import org.apache.http.HttpHeaders;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.util.Optional;
import java.util.Random;


public class SoDownloader {

    private static final int MAX_RETRY_COUNT = 3;

    private static Gson gson = new Gson();

    //MARK version 升级的逻辑需要升级时候优化
    @FunctionName("download")
    @StorageAccount(Constant.Downloader.AZURE_WEB_JOBS_STORAGE)
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

        String tag = decodedJWT.getClaim(Constant.Downloader.TAG).asString();
        Boolean isX64 = decodedJWT.getClaim(Constant.Downloader.IS_X64).asBoolean();

        trace.info(String.format("download : tag %s , x64 %b .", tag, isX64));

        JsonObject jsonObject = gson.fromJson(configuration, JsonObject.class);
        String taskId = jsonObject.get(Constant.DeployTask.TASK_ID).getAsString();
        int threshold = jsonObject.get(Constant.DeployTask.THRESHOLD).getAsInt();

        byte[] target = null;
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            target = getRandomSo(isX64, taskId, threshold, version, trace);
            if (target != null) {
                writeTag(target, tag);
                break;
            }
        }

        if (target == null) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("b error").build();
        }

        HttpResponseMessage build = request.createResponseBuilder(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE,"application/octet-stream").body(target).build();
        trace.trace("download : cost " + (System.currentTimeMillis() - start));
        return build;
    }

    private byte[] getRandomSo(boolean x64, String taskId, int threshold, String version, TraceHelper trace) {
        int index = threshold == 1 ? 1 : new Random().nextInt(threshold - 1) + 1;
        String target = System.getenv(Constant.Downloader.TARGET_PATH);
        String blobName = getTargetBlobName(taskId, index, x64, version);
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
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

        for (int i = 0; i < bytes.length; i += Constant.Downloader.ELF_DEFAULT_TAG_LENGTH) {
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
        if ((end - start) == Constant.Downloader.ELF_DEFAULT_TAG_LENGTH + 1) {
            return start + 1;
        }
        return -1;
    }

    private String getTargetBlobName(String taskId, int index, boolean x64, String version) {
        int blockIndex = (index / Constant.Downloader.DEFAULT_BOOTLOADER_BLOCK_SIZE) *
            Constant.Downloader.DEFAULT_BOOTLOADER_BLOCK_SIZE;
        return String.format("%s/%s/%d/%d/%s", version, taskId, blockIndex, index, x64 ? "bl64" : "bl32");
    }
}
