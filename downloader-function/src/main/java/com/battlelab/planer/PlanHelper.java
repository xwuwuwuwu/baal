package com.battlelab.planer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import com.microsoft.azure.storage.table.*;
import org.apache.commons.lang3.time.StopWatch;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * @author willsun
 */
public class PlanHelper {

    public static final int DEFAULT_STEP = 10000;
    public static final int QUEUE_SIZE = 4;

    public static void plan(Logger logger) {
        logger.entering("com.lab.azure.bootloader.PlanHelper.plan", "run");
        logger.info("begin");
        try {
            StopWatch sw = new StopWatch();
            sw.start();
            PlanHelper.doPlan(logger);
            sw.stop();
            logger.info(String.format("plan cost %d ms", sw.getTime()));
        } catch (InvalidKeyException | URISyntaxException | StorageException e) {
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

        Map<Integer, CloudQueue> queueMap = new HashMap<>(4);

        {
            for (int i = 0; i < QUEUE_SIZE; i++) {
                String queueName = String.format("%s%d", settings.getJobQueueName(), i);
                CloudQueue jobQueue = queueClient.getQueueReference(queueName);
                jobQueue.createIfNotExists();
                queueMap.put(i, jobQueue);
            }
        }

        CloudTable jobTable = tableClient.getTableReference(settings.getJobTableName());

        CloudTable jobHistoryTable = tableClient.getTableReference(settings.getJobHistoryTableName());
        jobHistoryTable.createIfNotExists();

        JobEntity plan = null;

        {
            TableQuery<JobEntity> query = TableQuery.from(JobEntity.class).take(8);
            Iterable<JobEntity> entities = jobTable.execute(query);

            List<JobEntity> es = StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());
            if (!es.isEmpty()) {
                es.sort(Comparator.comparing(JobEntity::getCreateAt));
                plan = es.get(0);
            }
        }

        if (plan == null) {
            logger.info("no plan");
            return true;
        }
        int step = settings.getStepAsInteger();
        if (step <= 0) {
            step = DEFAULT_STEP;
        }
        {
            String targetPath = settings.getTargetPath();
            URI uri = URI.create(targetPath);
            CloudBlobContainer blobContainer = new CloudBlobContainer(uri, account.getCredentials());

            int section = plan.getCurrent() / step;

            String path = String.format("%s/%s/%d/", plan.version, plan.getPartitionKey(), section * step);
            long fileCount = StorageHelper.getBlobFileCount(blobContainer, path, settings.getMaxResultAsInteger());
            if (fileCount <= settings.getStepThresholdAsInteger() && fileCount > 0) {
                logger.info(String.format("file count %d less than step threshold", fileCount));
                return true;
            }
        }


        Gson gson = new Gson();
        String jsonString = gson.toJson(plan);
        logger.info(jsonString);

        if (plan.current >= plan.amount) {
            TableOperation delete = TableOperation.delete(plan);
            jobTable.execute(delete);
            plan.updateTimestamp();
            TableOperation insertOrMerge = TableOperation.insertOrMerge(plan);
            jobHistoryTable.execute(insertOrMerge);
            return true;
        } else {
            int start = plan.current;
            int current = plan.current + step;

            int count = step;
            if (current > plan.getAmount()) {
                count = plan.amount - plan.current;
                plan.current = plan.amount;
            } else {
                plan.current += step;
            }
            plan.updateTimestamp();
            TableOperation insertOrMerge = TableOperation.insertOrMerge(plan);
            jobTable.execute(insertOrMerge);
            makeQueueMessageAsync(queueMap, plan, start, count, settings.getSourcePath(), settings.getTargetPath());
        }
        logger.info("stop doplan");
        return false;
    }

    private static int makeQueueMessageAsync(Map<Integer, CloudQueue> queueMap, JobEntity jobEntity, int start, int count, String sourcePath, String targetPath) {

        AtomicInteger errorCounter = new AtomicInteger();

        IntStream.range(start, start + count).parallel().forEach(i -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("taskID", jobEntity.getPartitionKey());
            jsonObject.addProperty("jobID", i);
            jsonObject.addProperty("version", jobEntity.getVersion());
            jsonObject.addProperty("sourcePath", sourcePath);
            jsonObject.addProperty("targetPath", targetPath);
            CloudQueueMessage message = new CloudQueueMessage(jsonObject.toString());
            try {
                int order = i % QUEUE_SIZE;
                CloudQueue cloudQueue = queueMap.get(order);
                cloudQueue.addMessage(message);
            } catch (StorageException e) {
                errorCounter.incrementAndGet();
            }
        });
        return errorCounter.get();
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

