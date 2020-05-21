using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Table;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using AzureBootloaderCompiler.Utils.Arguments;

namespace AzureBootloaderCompiler.Utils
{
    class QueryHelper
    {
        public static async Task DoQueryAsync(QueryArgument argument)
        {
            Console.WriteLine($"queryJob {argument.Task} start");
            Settings settings = Settings.LoadCfg(argument.SettingFile);
            var account = CloudStorageAccount.Parse(settings.AzureWebJobsStorage);
            var tableClient = account.CreateCloudTableClient();
            var jobHistoryTable = tableClient.GetTableReference(settings.JobHistoryTable);
            _ = jobHistoryTable.CreateIfNotExistsAsync();

            TableQuery<JobEntity> query;

            if ("all".Equals(argument.Task.ToLower()))
            {
                query = new TableQuery<JobEntity>();
            }
            else
            {
                string pfilter = TableQuery.GenerateFilterCondition("PartitionKey", QueryComparisons.Equal, argument.Task);
                string rfilter = TableQuery.GenerateFilterCondition("RowKey", QueryComparisons.Equal, argument.Task);
                string cfilter = TableQuery.CombineFilters(pfilter, TableOperators.And, rfilter);
                query = new TableQuery<JobEntity>().Where(cfilter);
            }

            TableQuerySegment<JobEntity> sengment = await jobHistoryTable.ExecuteQuerySegmentedAsync(query, null);

            List<JobEntity> entities = sengment.Results;
            Console.WriteLine("===== show tasks =====");
            foreach (JobEntity i in entities)
            {
                Console.WriteLine($"version : {i.Version}, task : {i.PartitionKey}, amount : {i.Amount}");
            }
            Console.WriteLine("===== show tasks =====");
            Console.WriteLine($"queryJob {argument.Task} stop");
        }
    }
}
