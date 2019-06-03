package com.battlelab.api.v2;

import com.battlelab.Constant;
import com.battlelab.TraceHelper;
import com.battlelab.api.TableEntityHelper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.DeleteSnapshotsOption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DocumentBuilder implements TableEntityHelper {

    private static final String DOMAIN_RECORD_TABLE_NAME = "domainRecord";

    private static final String LOCKER_CONTAINER_NAME = "api-lock";

    public static final String DOMAIN_PARTITION_KEY = "apiV2";

    private static Gson gson = new Gson();

    @FunctionName("docV2")
    @StorageAccount(Constant.Downloader.AZURE_WEB_JOBS_STORAGE)
    public HttpResponseMessage makeDocV2(
        @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "v2/doc")
            HttpRequestMessage<Optional<String>> request,
        @TableInput(name = "domainConfig",
            tableName = "domainConfig",
            partitionKey = "apiV2",
            rowKey = "latest"
        ) String domains,
        final ExecutionContext context) throws IOException, URISyntaxException, InvalidKeyException, StorageException {

        TraceHelper trace = new TraceHelper(context.getLogger());

        if (domains == null || domains.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("渠道对应的域名池没有配置.").build();
        }

        String tag = request.getQueryParameters().get("tag");

        if (tag == null || tag.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("需要传入必要的渠道标签.").build();
        }

        trace.trace("docV2 : 当前域名池 " + domains);

        JsonObject jsonObject = gson.fromJson(domains, JsonObject.class);

        String[] split = jsonObject.get("domains").getAsString().split(",");
        String domain = null;
        String leaseID = UUID.randomUUID().toString();
        for (int i = 0; i < split.length; i++) {
            domain = split[i];
            trace.trace("docV2 : 检测域名 " + domain);
            if (checkDomain(domain)) {
                if (lockDomain(domain, leaseID)) {
                    break;
                }
            }
            domain = null;
        }

        if (domain == null) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("域名池已经用尽,需要更新域名池.").build();
        }

        recordDomain(tag, domain);

        clearLock(domain, leaseID);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        ZipEntry rEntry = new ZipEntry("readme.md");
        zipOutputStream.putNextEntry(rEntry);
        InputStream resourceAsStream = getClass().getResourceAsStream("/docV2.md");
        byte[] buffer = new byte[2048];
        int readLen = 0;
        ByteArrayOutputStream docV2 = new ByteArrayOutputStream();
        while ((readLen = resourceAsStream.read(buffer)) != -1) {
            docV2.write(buffer, 0, readLen);
        }
        resourceAsStream.close();
        docV2.close();
        String doc = new String(docV2.toByteArray(), Charset.forName("UTF-8"));
        doc = doc.replace("####{host}####", domain);
        zipOutputStream.write(doc.getBytes(Charset.forName("UTF-8")));
        zipOutputStream.close();
        outputStream.close();

        return request.createResponseBuilder(HttpStatus.OK)
            .body(outputStream.toByteArray()).build();
    }

    private void clearLock(String domain, String leaseId) throws StorageException, URISyntaxException, InvalidKeyException {
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);

        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();
        CloudBlobContainer containerReference = cloudBlobClient.getContainerReference(LOCKER_CONTAINER_NAME);
        String replace = domain.replace("/", "_");
        CloudAppendBlob appendBlobReference = containerReference.getAppendBlobReference(replace);
        if (!appendBlobReference.exists()) {
            return;
        }
        appendBlobReference.delete(DeleteSnapshotsOption.NONE, AccessCondition.generateLeaseCondition(leaseId), null, null);
    }

    private void recordDomain(String tag, String domain) throws InvalidKeyException, StorageException, URISyntaxException {
        DomainRecordEntity entity = new DomainRecordEntity();
        entity.setPartitionKey(DOMAIN_PARTITION_KEY);
        entity.setRowKey(domain);
        entity.setTag(tag);
        entity.setRecordAt(new Date());
        insert(DOMAIN_RECORD_TABLE_NAME, entity);
    }

    private boolean lockDomain(String domain, String leaseId) throws URISyntaxException, InvalidKeyException, StorageException {
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);

        CloudBlobClient cloudBlobClient = account.createCloudBlobClient();
        CloudBlobContainer containerReference = cloudBlobClient.getContainerReference(LOCKER_CONTAINER_NAME);
        String replace = domain.replace("/", "_");
        CloudAppendBlob appendBlobReference = containerReference.getAppendBlobReference(replace);
        if (appendBlobReference.exists()) {
            return false;
        }
        appendBlobReference.createOrReplace();
        String s = appendBlobReference.acquireLease(null, leaseId);
        return leaseId.equals(s);
    }

    private boolean checkDomain(String domain) throws URISyntaxException, InvalidKeyException, StorageException {
        DomainRecordEntity entity = query(DOMAIN_RECORD_TABLE_NAME, DOMAIN_PARTITION_KEY, domain, DomainRecordEntity.class);
        return entity == null;
    }

}
