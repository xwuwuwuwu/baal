using Microsoft.VisualBasic.CompilerServices;
using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Table;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using AzureBootloaderCompiler.Utils.Arguments;

namespace AzureBootloaderCompiler.Utils
{
    class PlanHelper
    {
        public static async Task MakePlanAsync(PlanArgument planArgument) 
        {
            Console.WriteLine($"plan {planArgument.Task} start");
            Settings settings = Settings.LoadCfg(planArgument.SettingFile);

            if (!CheckKey(planArgument.Task))
            {
                Console.WriteLine($"task key {planArgument.Task} error");
                Environment.Exit(-1);
            }

            var account = CloudStorageAccount.Parse(settings.AzureWebJobsStorage);
            var tableClient = account.CreateCloudTableClient();
            var cloudTable = tableClient.GetTableReference(settings.JobTable);
            _ = cloudTable.CreateIfNotExistsAsync();
            Console.WriteLine($"Table {settings.JobTable} create success");

            {
                var partitionKey = planArgument.Task;
                var rowKey = planArgument.Task;
                TableOperation retrieveOperation = TableOperation.Retrieve<JobEntity>(partitionKey, rowKey);
                TableResult result = await cloudTable.ExecuteAsync(retrieveOperation);
                
                if (result.Result is JobEntity)
                {
                    Console.WriteLine($"task {planArgument.Task} exist");
                    Environment.Exit(-1);
                }

                
                if (planArgument.Count <= 0)
                {
                    Console.WriteLine($"task count {planArgument.Count} error");
                    Environment.Exit(-1);
                }
            }

            {
                JobEntity jobEntity = new JobEntity();
                jobEntity.PartitionKey = planArgument.Task;
                jobEntity.RowKey = planArgument.Task;
                jobEntity.Version = planArgument.Version;
                jobEntity.Amount = planArgument.Count;
                jobEntity.Current = 0;
                jobEntity.CreateAt = DateTime.Now;
                jobEntity.UpdateAt = DateTime.Now;

                TableOperation insertOrMergeOperation = TableOperation.InsertOrMerge(jobEntity);
                await cloudTable.ExecuteAsync(insertOrMergeOperation);
                Console.WriteLine($"plan {planArgument.Task} stop");
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
