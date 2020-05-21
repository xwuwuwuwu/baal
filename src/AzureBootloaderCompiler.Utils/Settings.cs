using System;
using System.Collections.Generic;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System.IO;
using System.Text;

namespace AzureBootloaderCompiler.Utils
{
    class Settings
    {
        public string AzureWebJobsStorage { get; set; }
        public string JobQueue { get; set; }
        public string JobTable { get; set; }
        public string JobHistoryTable { get; set; }
        public string DeployTable { get; set; }
        public string SourcePath { get; set; }
        public string TargetPath { get; set; }

        public static Settings LoadCfg(string cfg)
        {
            var jsonString = File.ReadAllText(cfg, Encoding.Default);
            var js = JsonConvert.DeserializeObject<JObject>(jsonString);

            if (!js.TryGetValue("AzureWebJobsStorage", out var azureWebJobsStorage))
            {
                throw new Exception($"AzureWebJobsStorage is null");
            }
            if (!js.TryGetValue("JobQueue", out var jobQueue))
            {
                throw new Exception($"JobQueue is null");
            }
            if (!js.TryGetValue("JobTable", out var jobTable))
            {
                throw new Exception($"JobTable is null");
            }
            if (!js.TryGetValue("JobHistoryTable", out var jobHistoryTable))
            {
                throw new Exception($"JobHistoryTable is null");
            }
            if (!js.TryGetValue("DeployTable", out var deployTable))
            {
                throw new Exception($"DeployTable is null");
            }
            if (!js.TryGetValue("SourcePath", out var sourcePath))
            {
                throw new Exception($"SourcePath is null");
            }
            if (!js.TryGetValue("TargetPath", out var targetPath))
            {
                throw new Exception($"TargetPath is null");
            }

            Settings settings = new Settings();
            settings.AzureWebJobsStorage = azureWebJobsStorage.ToObject<string>();
            settings.JobQueue = jobQueue.ToObject<string>();
            settings.JobTable = jobTable.ToObject<string>();
            settings.JobHistoryTable = jobHistoryTable.ToObject<string>();
            settings.DeployTable = deployTable.ToObject<string>();
            settings.SourcePath = sourcePath.ToObject<string>();
            settings.TargetPath = targetPath.ToObject<string>();
            return settings;
        }
    }
}
