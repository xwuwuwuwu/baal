package com.battlelab.planer;

import com.microsoft.azure.storage.ResultContinuation;
import com.microsoft.azure.storage.ResultSegment;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobListingDetails;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;
import org.apache.commons.lang3.time.StopWatch;

import java.util.EnumSet;
import java.util.Objects;

/**
 * @author willsun
 */
public class StorageHelper {
    public static final int DEFAULT_MAX_RESULT = 1000;

    public static final int DEFAULT_STEP = 10000;

    public static boolean isTimeout(StopWatch stopWatch, int timeout) {
        return timeout * 1000 - stopWatch.getTime() <= 0;
    }

    public static long getBlobFileCount(CloudBlobContainer blobContainer, String path) throws StorageException {
        return getBlobFileCount(blobContainer, path, DEFAULT_MAX_RESULT);
    }

    public static long getBlobFileCount(CloudBlobContainer blobContainer, String path, int maxResult) throws StorageException {
        ResultContinuation token = null;
        long count = 0;
        if (maxResult <= 0) {
            maxResult = DEFAULT_MAX_RESULT;
        }
        do {
            ResultSegment<ListBlobItem> resultSegment = blobContainer.listBlobsSegmented(path, true, EnumSet.of(BlobListingDetails.METADATA),
                    maxResult, token, null, null);
            token = resultSegment.getContinuationToken();
            count += resultSegment.getLength();
        } while (token != null);
        return count;
    }


    public static class Settings {
        public static final String AZURE_WEBJOBS_STORAGE_KEY = "AzureWebJobsStorage";
        public static final String JOB_QUEUE_KEY = "job_queue";
        public static final String JOB_TABLE_KEY = "job_table";
        public static final String JOB_HISTORY_TABLE_KEY = "job_history_table";
        public static final String DEPLOY_TABLE_KEY = "deploy_table";
        public static final String SOURCE_PATH_KEY = "source_path";
        public static final String TARGET_PATH_KEY = "target_path";
        public static final String MAX_RESULT_KEY = "max_result";
        public static final String STEP_KEY = "step";
        public static final String STEP_THRESHOLD_KEY = "step_threshold";

        private String azureWebJobsStorage;
        private String jobTableName;
        private String jobHistoryTableName;
        private String deployTableName;
        private String jobQueueName;
        private String sourcePath;
        private String targetPath;
        private String maxResult;
        private String step;
        private String stepThreshold;

        public static Settings load() {
            Settings settings = new Settings();
            settings.setAzureWebJobsStorage(System.getenv(Settings.AZURE_WEBJOBS_STORAGE_KEY));
            settings.setJobTableName(System.getenv(Settings.JOB_TABLE_KEY));
            settings.setJobHistoryTableName(System.getenv(Settings.JOB_HISTORY_TABLE_KEY));
            settings.setDeployTableName(System.getenv(Settings.DEPLOY_TABLE_KEY));
            settings.setJobQueueName(System.getenv(Settings.JOB_QUEUE_KEY));
            settings.setSourcePath(System.getenv(Settings.SOURCE_PATH_KEY));
            settings.setTargetPath(System.getenv(Settings.TARGET_PATH_KEY));
            settings.setMaxResult(System.getenv(Settings.MAX_RESULT_KEY));
            settings.setMaxResult(System.getenv(Settings.STEP_KEY));
            settings.setStepThreshold(System.getenv(Settings.STEP_THRESHOLD_KEY));
            Objects.requireNonNull(settings.getAzureWebJobsStorage());
            Objects.requireNonNull(settings.getJobTableName());
            Objects.requireNonNull(settings.getJobHistoryTableName());
            Objects.requireNonNull(settings.getDeployTableName());
            Objects.requireNonNull(settings.getJobQueueName());
            Objects.requireNonNull(settings.getSourcePath());
            Objects.requireNonNull(settings.getTargetPath());
            return settings;
        }

        public String getAzureWebJobsStorage() {
            return this.azureWebJobsStorage;
        }

        public void setAzureWebJobsStorage(String azureWebJobsStorage) {
            this.azureWebJobsStorage = azureWebJobsStorage;
        }

        public String getJobTableName() {
            return this.jobTableName;
        }

        public void setJobTableName(String jobTableName) {
            this.jobTableName = jobTableName;
        }

        public String getJobHistoryTableName() {
            return this.jobHistoryTableName;
        }

        public void setJobHistoryTableName(String jobHistoryTableName) {
            this.jobHistoryTableName = jobHistoryTableName;
        }

        public String getDeployTableName() {
            return this.deployTableName;
        }

        public void setDeployTableName(String deployTableName) {
            this.deployTableName = deployTableName;
        }

        public String getJobQueueName() {
            return this.jobQueueName;
        }

        public void setJobQueueName(String jobQueueName) {
            this.jobQueueName = jobQueueName;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        public void setSourcePath(String sourcePath) {
            this.sourcePath = sourcePath;
        }

        public String getTargetPath() {
            return this.targetPath;
        }

        public void setTargetPath(String targetPath) {
            this.targetPath = targetPath;
        }

        public String getMaxResult() {
            return this.maxResult;
        }

        public void setMaxResult(String maxResult) {
            this.maxResult = maxResult;
        }

        public int getMaxResultAsInteger() {
            try {
                return Integer.valueOf(this.getMaxResult());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public String getStep() {
            return this.step;
        }

        public void setStep(String step) {
            this.step = step;
        }

        public int getStepAsInteger() {
            try {
                return Integer.valueOf(this.getStep());
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        public String getStepThreshold() {
            return this.stepThreshold;
        }

        public void setStepThreshold(String stepThreshold) {
            this.stepThreshold = stepThreshold;
        }

        public int getStepThresholdAsInteger() {
            try {
                return Integer.valueOf(this.getStepThreshold());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
