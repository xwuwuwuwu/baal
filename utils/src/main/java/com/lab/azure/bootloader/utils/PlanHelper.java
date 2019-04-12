package com.lab.azure.bootloader.utils;

import com.lab.azure.bootloader.utils.arguments.PlanArgument;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PlanHelper {
    public static void makePlan(PlanArgument planArgument) throws IOException, URISyntaxException, InvalidKeyException, StorageException {
        Logger logger = LoggerFactory.getLogger("plan");
        logger.info("plan {} start", planArgument.getTask());
        Settings settings = Settings.loadCfg(planArgument.getSettingFile());
        if (!settings.validate()) {
            logger.warn("settings error");
            System.exit(-1);
        }
        if (!checkKey(planArgument.getTask())) {
            logger.warn("task key {} error", planArgument.getTask());
            System.exit(-1);
        }
        CloudStorageAccount account = CloudStorageAccount.parse(settings.getAzureWebJobsStorage());
        CloudTableClient tableClient = account.createCloudTableClient();
        CloudTable jobTable = tableClient.getTableReference(settings.getJobTable());
        jobTable.createIfNotExists();

        {
            String filter;
            {
                String pkFilter = TableQuery.generateFilterCondition("PartitionKey", TableQuery.QueryComparisons.EQUAL, planArgument.getTask());
                String rkFilter = TableQuery.generateFilterCondition("RowKey", TableQuery.QueryComparisons.EQUAL, planArgument.getTask());
                filter = TableQuery.combineFilters(pkFilter, TableQuery.Operators.AND, rkFilter);
            }

            TableQuery<JobEntity> query = TableQuery.from(JobEntity.class)
                .where(filter);

            Iterable<JobEntity> entities = jobTable.execute(query);

            List<JobEntity> tasks = StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());
            if (!tasks.isEmpty()) {
                logger.warn("task {} exist", planArgument.getTask());
                System.exit(-1);
            }
            if (planArgument.getCount() <= 0) {
                logger.warn("task count {} error", planArgument.getCount());
                System.exit(-1);
            }
        }

        {
            JobEntity jobEntity = new JobEntity();
            jobEntity.setPartitionKey(planArgument.getTask());
            jobEntity.setRowKey(planArgument.getTask());
            jobEntity.setVersion(planArgument.getVersion());
            jobEntity.setAmount(planArgument.getCount());
            jobEntity.setCurrent(0);
            jobEntity.updateTimestamp();
            TableOperation operation = TableOperation.insert(jobEntity);
            jobTable.execute(operation);

        }
        logger.info("plan {} finish", planArgument.getTask());
    }

    private static boolean checkKey(String keyString) {
        try {
            UUID uuid = UUID.fromString(keyString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
