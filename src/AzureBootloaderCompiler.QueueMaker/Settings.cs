using System;
using System.Collections.Generic;
using System.Text;

namespace AzureBootloaderCompiler.QueueMaker
{
    class Settings
    {
        public static string AZURE_WEBJOBS_STORAGE_KEY = "AzureWebJobsStorage";
        public static string JOB_QUEUE_KEY = "job_queue";
        public static string JOB_TABLE_KEY = "job_table";
        public static string JOB_HISTORY_TABLE_KEY = "job_history_table";
        public static string DEPLOY_TABLE_KEY = "deploy_table";
        public static string SOURCE_PATH_KEY = "source_path";
        public static string TARGET_PATH_KEY = "target_path";
        public static string MAX_RESULT_KEY = "max_result";
        public static string STEP_KEY = "step";
        public static string STEP_THRESHOLD_KEY = "step_threshold";

        public string AzureWebJobsStorage { get; set; }
        public string JobTableName { get; set; }
        public string JobHistoryTableName { get; set; }
        public string DeployTableName { get; set; }
        public string JobQueueName { get; set; }
        public string SourcePath { get; set; }
        public string TargetPath { get; set; }
        public string MaxResult { get; set; }
        public string Step { get; set; }
        public string StepThreshold { get; set; }

        public static Settings Load()
        {
            Settings settings = new Settings();
            var connectionString = Environment.GetEnvironmentVariable(AZURE_WEBJOBS_STORAGE_KEY);
            var jobTable = Environment.GetEnvironmentVariable(JOB_TABLE_KEY);
            var jobHistoryTable = Environment.GetEnvironmentVariable(JOB_HISTORY_TABLE_KEY);
            var deployTable = Environment.GetEnvironmentVariable(DEPLOY_TABLE_KEY);
            var jobQueue = Environment.GetEnvironmentVariable(JOB_QUEUE_KEY);
            var sourcePath = Environment.GetEnvironmentVariable(SOURCE_PATH_KEY);
            var targetPath = Environment.GetEnvironmentVariable(TARGET_PATH_KEY);
            var maxResult = Environment.GetEnvironmentVariable(MAX_RESULT_KEY);
            var step = Environment.GetEnvironmentVariable(STEP_KEY);
            var stepThreshold = Environment.GetEnvironmentVariable(STEP_THRESHOLD_KEY);

            settings.AzureWebJobsStorage = connectionString;
            settings.JobTableName = jobTable;
            settings.JobHistoryTableName = jobHistoryTable;
            settings.DeployTableName = deployTable;
            settings.JobQueueName = jobQueue;
            settings.SourcePath = sourcePath;
            settings.TargetPath = targetPath;
            settings.MaxResult = maxResult;
            settings.Step = step;
            settings.StepThreshold = stepThreshold;

            return settings;
        }
    }
}
