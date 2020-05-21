using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Table;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using AzureBootloaderCompiler.Utils.Arguments;

namespace AzureBootloaderCompiler.Utils
{
    class DeployHelper
    {
        public static async Task DoDeployAsync(DeployArgument argument)
        {
            Console.WriteLine($"deploy {argument.Task} start");
            Settings settings = Settings.LoadCfg(argument.SettingFile);
            if (!CheckKey(argument.Task))
            {
                Console.WriteLine($"task key {argument.Task} error");
                Environment.Exit(-1);
            }
            var account = CloudStorageAccount.Parse(settings.AzureWebJobsStorage);
            var tableClient = account.CreateCloudTableClient();
            var jobHistoryTable = tableClient.GetTableReference(settings.JobHistoryTable);
            var deployTable = tableClient.GetTableReference(settings.DeployTable);

            if (!await jobHistoryTable.ExistsAsync())
            {
                Console.WriteLine("jobHistoryTable not exists");
                Environment.Exit(-1);
            }
            if (!await deployTable.ExistsAsync())
            {
                Console.WriteLine("jobHistoryTable not exists");
                Environment.Exit(-1);
            }

            JobEntity jobEntity;
            {
                var partitionKey = argument.Task;
                var rowKey = argument.Task;
                TableOperation retrieveOperation = TableOperation.Retrieve<JobEntity>(partitionKey, rowKey);
                TableResult result = await jobHistoryTable.ExecuteAsync(retrieveOperation);
                jobEntity = result.Result as JobEntity;

                if (jobEntity == null)
                {
                    Console.WriteLine("job Entity not 1");
                    Environment.Exit(-1);
                }
            }

            {
                DynamicTableEntity deploy = new DynamicTableEntity(jobEntity.Version, "latest");
                Dictionary<string, EntityProperty> pairs = new Dictionary<string, EntityProperty>();

                pairs["TaskId"] = new EntityProperty(jobEntity.PartitionKey);
                pairs["Threshold"] = new EntityProperty(jobEntity.Amount);
                pairs["UpdateAt"] = new EntityProperty(new DateTime());
                deploy.Properties = pairs;
                TableOperation insertOrMerge = TableOperation.InsertOrMerge(deploy);
                _ = deployTable.ExecuteAsync(insertOrMerge);
                Console.WriteLine($"deploy {argument.Task} stop");
            }
        }

        private static Boolean CheckKey(string keyString)
        {
            try
            {
                Guid uuid = Guid.Parse(keyString);
                return true;
            }
            catch (FormatException)
            {
                return false;
            }
        }
    }
}
