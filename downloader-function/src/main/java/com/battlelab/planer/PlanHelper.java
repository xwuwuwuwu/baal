package com.battlelab.planer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import com.microsoft.azure.storage.table.*;
import org.apache.commons.lang3.time.StopWatch;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author willsun
 */
public class PlanHelper {

    public static void plan(Logger logger) {
        logger.entering("com.lab.azure.bootloader.PlanHelper.plan", "run");
        StorageHelper.Settings settings = StorageHelper.Settings.load();
        logger.info("begin");
        String key = "bootloader_plan_lock";
        try {
            StopWatch sw = new StopWatch();
            sw.start();
            for (; ; ) {
                if (StorageHelper.isTimeout(sw, 5 * 60)) {
                    logger.info("time out");
                    return;
                }
                try {
                    boolean done = PlanHelper.doPlan(logger);
                    if (done) {
                        return;
                    }
                } catch (StorageException e) {
                    logger.log(Level.SEVERE, "error", e);
                }
            }
        } catch (InvalidKeyException | URISyntaxException e) {
            logger.log(Level.SEVERE, "error", e);
        } finally {
            logger.info("end");
            logger.exiting("com.lab.azure.bootloader.PlanHelper.plan", "run");
        }
    }


    private static boolean doPlan(Logger logger) throws URISyntaxException, InvalidKeyException, StorageException {
        logger.info("start doplan");
        StorageHelper.Settings settings = StorageHelper.Settings.load();
        CloudStorageAccount account = CloudStorageAccount.parse(settings.getAzureWebJobsStorage());
        CloudTableClient tableClient = account.createCloudTableClient();
        CloudQueueClient queueClient = account.createCloudQueueClient();
        CloudQueue jobQueue = queueClient.getQueueReference(settings.getJobQueueName());
        jobQueue.createIfNotExists();
        CloudTable jobTable = tableClient.getTableReference(settings.getJobTableName());

        CloudTable jobHistoryTable = tableClient.getTableReference(settings.getJobHistoryTableName());
        jobHistoryTable.createIfNotExists();


        TableQuery<JobEntity> query = TableQuery.from(JobEntity.class).take(8);
        Iterable<JobEntity> entities = jobTable.execute(query);

        List<JobEntity> es = StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());
        if (es.isEmpty()) {
            return true;
        }

        for (JobEntity i : entities) {

            Gson gson = new Gson();
            String jsonString = gson.toJson(i);
            logger.info(jsonString);

            if (i.current >= i.amount) {
                TableOperation delete = TableOperation.delete(i);
                jobTable.execute(delete);
                i.updateTimestamp();
                TableOperation insertOrMerge = TableOperation.insertOrMerge(i);
                jobHistoryTable.execute(insertOrMerge);
                return true;
            } else {
                int start = i.current;
                i.current += 1000;
                i.updateTimestamp();
                TableOperation insertOrMerge = TableOperation.insertOrMerge(i);
                jobTable.execute(insertOrMerge);
                makeQueueMessage(jobQueue, i, start, 1000, settings.getSourcePath(), settings.getTargetPath());

            }
        }
        logger.info("stop doplan");
        return false;
    }

    private static void makeQueueMessage(CloudQueue cloudQueue, JobEntity jobEntity, int start, int count, String sourcePath, String targetPath) {
        for (int i = 0; i < count; i++) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("taskID", jobEntity.getPartitionKey());
            jsonObject.addProperty("jobID", start + i);
            jsonObject.addProperty("version", jobEntity.getVersion());
            jsonObject.addProperty("sourcePath", sourcePath);
            jsonObject.addProperty("targetPath", targetPath);
            CloudQueueMessage message = new CloudQueueMessage(jsonObject.toString());
            try {
                cloudQueue.addMessage(message);
            } catch (StorageException e) {
            }
        }
    }


    public static class JobEntity extends TableServiceEntity {
        private String version;
        private Integer amount;
        private Integer current;
        private Date createAt;
        private Date updateAt;

        public String getVersion() {
            return this.version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Integer getAmount() {
            return this.amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }

        public Integer getCurrent() {
            return this.current;
        }

        public void setCurrent(Integer current) {
            this.current = current;
        }

        public Date getCreateAt() {
            return this.createAt;
        }

        public void setCreateAt(Date createAt) {
            this.createAt = createAt;
        }

        public Date getUpdateAt() {
            return this.updateAt;
        }

        public void setUpdateAt(Date updateAt) {
            this.updateAt = updateAt;
        }

        public void updateTimestamp() {
            if (this.createAt == null) {
                this.createAt = new Date();
            }
            this.updateAt = new Date();
        }
    }
}

