package com.lab.azure.bootloader.utils;

import com.lab.azure.bootloader.utils.arguments.DeployArgument;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.table.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class DeployHelper {

    public static void deploy(DeployArgument deployArgument) throws StorageException, URISyntaxException, InvalidKeyException, IOException {
        Logger logger = LoggerFactory.getLogger("deploy");
        logger.info("deploy {} start", deployArgument.getTask());
        Settings settings = Settings.loadCfg(deployArgument.getSettingFile());
        if (!settings.validate()) {
            logger.warn("settings error");
            System.exit(-1);
        }
        if (!Helper.checkKey(deployArgument.getTask())) {
            logger.warn("task key {} error", deployArgument.getTask());
            System.exit(-1);
        }
        CloudStorageAccount account = CloudStorageAccount.parse(settings.getAzureWebJobsStorage());
        CloudTableClient tableClient = account.createCloudTableClient();
        CloudTable jobHistoryTable = tableClient.getTableReference(settings.getJobHistoryTable());
        CloudTable deployTable = tableClient.getTableReference(settings.getDeployTable());
        if (!jobHistoryTable.exists()) {
            logger.warn("jobHistoryTable not exists");
            System.exit(-1);
        }
        if (!deployTable.exists()) {
            logger.warn("jobHistoryTable not exists");
            System.exit(-1);
        }

        JobEntity jobEntity;
        {
            String pfilter = TableQuery.generateFilterCondition("PartitionKey", TableQuery.QueryComparisons.EQUAL, deployArgument.getTask());
            String rfilter = TableQuery.generateFilterCondition("RowKey", TableQuery.QueryComparisons.EQUAL, deployArgument.getTask());
            String cfilter = TableQuery.combineFilters(pfilter, TableQuery.Operators.AND, rfilter);
            TableQuery<JobEntity> query = TableQuery.from(JobEntity.class).where(cfilter);
            Iterable<JobEntity> jobEntityIterable = jobHistoryTable.execute(query);
            List<JobEntity> jobEntities = StreamSupport.stream(jobEntityIterable.spliterator(), false).collect(Collectors.toList());
            if(jobEntities.size()!=1){
                logger.warn("job Entity not 1");
                System.exit(-1);
            }
            jobEntity = jobEntities.get(0);
        }
        {
            DynamicTableEntity deploy = new DynamicTableEntity(jobEntity.getVersion(), "latest");
            HashMap<String, EntityProperty> map = new HashMap<>();
            map.put("TaskId",new EntityProperty(jobEntity.getPartitionKey()));
            map.put("Threshold",new EntityProperty(jobEntity.getAmount()));
            map.put("UpdateAt",new EntityProperty(new Date()));
            deploy.setProperties(map);
            TableOperation insertOrMerge = TableOperation.insertOrMerge(deploy);
            deployTable.execute(insertOrMerge);
        }
        logger.info("deploy {} stop", deployArgument.getTask());

    }
}
