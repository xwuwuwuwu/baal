package com.lab.azure.bootloader.utils;

import com.lab.azure.bootloader.utils.arguments.QueryArgument;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.CloudTable;
import com.microsoft.azure.storage.table.CloudTableClient;
import com.microsoft.azure.storage.table.TableQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

public class QueryHelper {
    public static void queryJob(QueryArgument queryArgument) throws URISyntaxException, StorageException, InvalidKeyException, IOException {
        Logger logger = LoggerFactory.getLogger("queryJob");
        logger.info("queryJob {} start", queryArgument.getTask());
        Settings settings = Settings.loadCfg(queryArgument.getSettingFile());
        if (!settings.validate()) {
            logger.warn("settings error");
            System.exit(-1);
        }

        CloudStorageAccount account = CloudStorageAccount.parse(settings.getAzureWebJobsStorage());
        CloudTableClient tableClient = account.createCloudTableClient();
        CloudTable jobHistoryTable = tableClient.getTableReference(settings.getJobHistoryTable());
        jobHistoryTable.createIfNotExists();

        TableQuery<JobEntity> query;

        if("all".equalsIgnoreCase(queryArgument.getTask())){
            query = TableQuery.from(JobEntity.class);
        }
        else{
            String pfilter = TableQuery.generateFilterCondition("PartitionKey", TableQuery.QueryComparisons.EQUAL, queryArgument.getTask());
            String rfilter = TableQuery.generateFilterCondition("RowKey", TableQuery.QueryComparisons.EQUAL, queryArgument.getTask());
            String cfilter = TableQuery.combineFilters(pfilter, TableQuery.Operators.AND, rfilter);
            query = TableQuery.from(JobEntity.class).where(cfilter);
        }

        Iterable<JobEntity> entityIterable = jobHistoryTable.execute(query);
        logger.info("===== show tasks =====");
        for (JobEntity i: entityIterable){
            logger.info("version : {}, task : {}, amount : {}", i.getVersion(), i.getPartitionKey(), i.getAmount());
        }
        logger.info("===== show tasks =====");
        logger.info("queryJob {} stop", queryArgument.getTask());
    }
}
