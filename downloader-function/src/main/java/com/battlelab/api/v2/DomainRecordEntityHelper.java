package com.battlelab.api.v2;

import com.battlelab.Constant;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableOperation;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public interface DomainRecordEntityHelper {
    default DomainRecordEntity queryRecordEntity(String tableName, String partitionKey, String rowkey) throws URISyntaxException, InvalidKeyException, StorageException {
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudTableClient cloudTableClient = account.createCloudTableClient();

        CloudTable tableReference = cloudTableClient.getTableReference(tableName);
        TableOperation retrieve = TableOperation.retrieve(partitionKey, rowkey, DomainRecordEntity.class);

        return tableReference.execute(retrieve).getResultAsType();
    }

    default void insertRecordEntity(String tableName, DomainRecordEntity entity) throws URISyntaxException, InvalidKeyException, StorageException {
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudTableClient cloudTableClient = account.createCloudTableClient();
        CloudTable tableReference = cloudTableClient.getTableReference(tableName);
        TableOperation insert = TableOperation.insert(entity);
        tableReference.execute(insert);
    }
}
