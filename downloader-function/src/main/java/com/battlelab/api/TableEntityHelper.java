package com.battlelab.api;

import com.battlelab.Constant;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableOperation;
import com.microsoft.azure.storage.table.TableServiceEntity;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public interface TableEntityHelper {

    default <T extends TableServiceEntity> T query(String tableName, String partitionKey, String rowkey, Class<T> clz) throws URISyntaxException, StorageException, InvalidKeyException {
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudTableClient cloudTableClient = account.createCloudTableClient();

        CloudTable tableReference = cloudTableClient.getTableReference(tableName);
        TableOperation retrieve = TableOperation.retrieve(partitionKey, rowkey, clz);

        return tableReference.execute(retrieve).getResultAsType();
    }

    default void insert(String tableName, TableServiceEntity entity) throws StorageException, URISyntaxException, InvalidKeyException {
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudTableClient cloudTableClient = account.createCloudTableClient();
        CloudTable tableReference = cloudTableClient.getTableReference(tableName);
        TableOperation insert = TableOperation.insert(entity);
        tableReference.execute(insert);
    }

    default void update(String tableName, TableServiceEntity entity) throws StorageException, URISyntaxException, InvalidKeyException {
        String connectionString = System.getenv(Constant.Downloader.AZURE_WEB_JOBS_STORAGE);
        CloudStorageAccount account = CloudStorageAccount.parse(connectionString);
        CloudTableClient cloudTableClient = account.createCloudTableClient();
        CloudTable tableReference = cloudTableClient.getTableReference(tableName);
        TableOperation replace = TableOperation.replace(entity);
        tableReference.execute(replace);
    }
}
