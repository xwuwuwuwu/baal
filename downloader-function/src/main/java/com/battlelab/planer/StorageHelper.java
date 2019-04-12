package com.battlelab.planer;

import org.apache.commons.lang3.time.StopWatch;

import java.util.Objects;

/**
 * @author willsun
 */
public class StorageHelper {
    public static boolean isTimeout(StopWatch stopWatch, int timeout) {
        return timeout * 1000 - stopWatch.getTime() <= 0;
    }

    public static class Settings {
        public static final String AZURE_WEBJOBS_STORAGE_KEY = "AzureWebJobsStorage";
        public static final String JOB_QUEUE_KEY = "job_queue";
        public static final String JOB_TABLE_KEY = "job_table";
        public static final String JOB_HISTORY_TABLE_KEY = "job_history_table";
        public static final String DEPLOY_TABLE_KEY = "deploy_table";
        public static final String SOURCE_PATH_KEY = "source_path";
        public static final String TARGET_PATH_KEY = "target_path";
        public static final String REDIS_KEY = "redis";

        private String azureWebJobsStorage;
        private String jobTableName;
        private String jobHistoryTableName;
        private String deployTableName;
        private String jobQueueName;
        private String sourcePath;
        private String targetPath;
        private String redis;


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

        public String getRedis() {
            return this.redis;
        }

        public void setRedis(String redis) {
            this.redis = redis;
        }

        public static Settings load() {
            Settings settings = new Settings();
            settings.setAzureWebJobsStorage(System.getenv(Settings.AZURE_WEBJOBS_STORAGE_KEY));
            settings.setJobTableName(System.getenv(Settings.JOB_TABLE_KEY));
            settings.setJobHistoryTableName(System.getenv(Settings.JOB_HISTORY_TABLE_KEY));
            settings.setDeployTableName(System.getenv(Settings.DEPLOY_TABLE_KEY));
            settings.setJobQueueName(System.getenv(Settings.JOB_QUEUE_KEY));
            settings.setSourcePath(System.getenv(Settings.SOURCE_PATH_KEY));
            settings.setTargetPath(System.getenv(Settings.TARGET_PATH_KEY));
            settings.setRedis(System.getenv(Settings.REDIS_KEY));
            Objects.requireNonNull(settings.getAzureWebJobsStorage());
            Objects.requireNonNull(settings.getJobTableName());
            Objects.requireNonNull(settings.getJobHistoryTableName());
            Objects.requireNonNull(settings.getDeployTableName());
            Objects.requireNonNull(settings.getJobQueueName());
            Objects.requireNonNull(settings.getSourcePath());
            Objects.requireNonNull(settings.getTargetPath());
            Objects.requireNonNull(settings.getRedis());
            return settings;
        }


    }
}
