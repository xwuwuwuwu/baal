package com.battlelab;

import java.util.Arrays;
import java.util.List;

public class Constant {

    public static final String SECRET_V1_FORMATER = "%s_df4n7920cap1d92";

    public static final int MAX_TAG_LENGTH = 20;

    public static class Downloader {
        public static final String TAG = "tag";
        public static final String ABI = "abi";
        public static final String IS_X64 = "x64";
        public static final int ELF_DEFAULT_TAG_LENGTH = 64;
        public static final String DOWNLOADER_URIS_FORMATTER = "DownloaderUris_V%d";
        public static final String TARGET_PATH = "target_path";
        public static final String AZURE_WEB_JOBS_STORAGE = "AzureWebJobsStorage";
        public static final int DEFAULT_BOOTLOADER_BLOCK_SIZE = 10000;
        public static final String VERSION = "v";
        public static List<String> ABIS_32 = Arrays.asList("armeabi", "armeabi-v7a");
        public static List<String> ABIS_64 = Arrays.asList("arm64-v8a");
        public static final List<String> ABIS = Arrays.asList("armeabi", "armeabi-v7a", "arm64-v8a");
        public static final int TOKEN_EXPIRED_SECONDS = 60 * 10;
    }

    public static class DeployTask {
        public static final String TASK_ID = "TaskId";
        public static final String THRESHOLD = "Threshold";
        public static final String DEPLOY_AT = "DeployAt";
        public static final String TIMESTAMP = "timestamp";
    }

    public static class Alert {
        public static final String ApplicationInsightQueryUrl = "ApplicationInsightQueryUrl";
        public static final String ApplicationInsightAPIKey = "ApplicationInsightAPIKey";
        public static final String AlertEmails = "AlertEmails";
        public static final String COUNT_FUNCTION_NAME = "FunctionDistinctCount";
        public static final String FREQUENCY_FUNCTION_NAME = "FunctionFrequencyCount";
        public static final double RATE = 0.7;
    }

    public static class ApiV2{
        public static final String DOMAIN_RECORD_TABLE_NAME = "domainRecord";

        public static final String DOMAIN_PARTITION_KEY = "apiV2";
    }
}
