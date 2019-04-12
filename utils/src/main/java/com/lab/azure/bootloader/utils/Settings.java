package com.lab.azure.bootloader.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * @author willsun
 */
public class Settings {
    private String azureWebJobsStorage;
    private String jobQueue;
    private String jobTable;
    private String jobHistoryTable;
    private String deployTable;
    private String sourcePath;
    private String targetPath;

    public String getAzureWebJobsStorage() {
        return this.azureWebJobsStorage;
    }

    public void setAzureWebJobsStorage(String azureWebJobsStorage) {
        this.azureWebJobsStorage = azureWebJobsStorage;
    }

    public String getJobQueue() {
        return this.jobQueue;
    }

    public void setJobQueue(String jobQueue) {
        this.jobQueue = jobQueue;
    }


    public String getJobTable() {
        return this.jobTable;
    }

    public void setJobTable(String jobTable) {
        this.jobTable = jobTable;
    }

    public String getJobHistoryTable() {
        return this.jobHistoryTable;
    }

    public void setJobHistoryTable(String jobHistoryTable) {
        this.jobHistoryTable = jobHistoryTable;
    }

    public String getDeployTable() {
        return this.deployTable;
    }

    public void setDeployTable(String deployTable) {
        this.deployTable = deployTable;
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

    public boolean validate() {
        return !(this.azureWebJobsStorage == null || this.azureWebJobsStorage.trim().isEmpty()
                || this.deployTable == null || this.deployTable.trim().isEmpty()
                || this.jobQueue == null || this.jobQueue.trim().isEmpty()
                || this.jobTable == null || this.jobTable.trim().isEmpty()
                || this.jobHistoryTable == null || this.jobHistoryTable.trim().isEmpty()
                || this.sourcePath == null || this.sourcePath.trim().isEmpty()
                || this.targetPath == null || this.targetPath.trim().isEmpty()
        );
    }

    public static Settings loadCfg(File cfg) throws IOException {
        Properties properties = new Properties();
        try (FileReader fr = new FileReader(cfg)) {
            properties.load(fr);
        }
        Settings settings = new Settings();
        settings.setAzureWebJobsStorage(properties.getProperty("AzureWebJobsStorage"));
        settings.setDeployTable(properties.getProperty("DeployTable"));
        settings.setJobQueue(properties.getProperty("JobQueue"));
        settings.setJobTable(properties.getProperty("JobTable"));
        settings.setJobHistoryTable(properties.getProperty("JobHistoryTable"));
        settings.setSourcePath(properties.getProperty("SourcePath"));
        settings.setTargetPath(properties.getProperty("TargetPath"));
        return settings;
    }
}
