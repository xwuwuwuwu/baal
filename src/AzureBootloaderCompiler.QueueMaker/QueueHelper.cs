using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Queue;
using Microsoft.WindowsAzure.Storage.Table;
using System.Linq;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;
using Microsoft.WindowsAzure.Storage.Blob;
using Newtonsoft.Json.Linq;
using Microsoft.Extensions.Logging;

namespace AzureBootloaderCompiler.QueueMaker
{
    public class QueueHelper
    {
        public static async Task<bool> MakeQueueAsync(ILogger logger)
        {
            Settings settings = Settings.Load();
            CloudStorageAccount account = CloudStorageAccount.Parse(settings.AzureWebJobsStorage);
            CloudTableClient tableClient = account.CreateCloudTableClient();
            CloudQueueClient queueClient = account.CreateCloudQueueClient();
            CloudQueue jobQueue = queueClient.GetQueueReference(settings.JobQueueName);
            _ = await jobQueue.CreateIfNotExistsAsync();
            CloudTable jobTable = tableClient.GetTableReference(settings.JobTableName);

            CloudTable jobHistoryTable = tableClient.GetTableReference(settings.JobHistoryTableName);
            _ = await jobHistoryTable.CreateIfNotExistsAsync();

            JobEntity plan = null;

            {
                TableQuery<JobEntity> query = new TableQuery<JobEntity>().Take(8);
                TableQuerySegment<JobEntity> segments = await jobTable.ExecuteQuerySegmentedAsync(query, null);
                var jobEntities = segments.Results;
                var ordered = jobEntities.OrderByDescending(i => i.CreateAt);
                plan = ordered.First();
            }

            if (plan == null)
            {
                return true;
            }

            int step = int.Parse(settings.Step);
            if (step <= 0)
            {
                step = 10000;
            }

            {
                string targetPath = settings.TargetPath;
                Uri uri = new Uri(targetPath);
                CloudBlobContainer blobContainer = new CloudBlobContainer(uri, account.Credentials);

                int section = plan.Current / step;
                string path = $"{plan.Version}/{plan.PartitionKey}/{section * step}";
                long fileCount = await StorageHelper.GetBlobFileCountAsync(blobContainer, path, int.Parse(settings.MaxResult));
                if (fileCount <= int.Parse(settings.StepThreshold) && fileCount > 0)
                {
                    logger.LogInformation($"file count {fileCount} less than step threshold");
                    return true;
                }
            }


            if (plan.Current >= plan.Amount)
            {
                TableOperation delete = TableOperation.Delete(plan);
                _ = jobTable.ExecuteAsync(delete);
                //plan.updateTimestamp();
                TableOperation insertOrMerge = TableOperation.InsertOrMerge(plan);
                _ = jobHistoryTable.ExecuteAsync(insertOrMerge);
                return true;
            }
            else
            {
                int start = plan.Current;
                int current = plan.Current + step;
                int count = step;
                if (current > plan.Amount)
                {
                    count = plan.Amount - plan.Current;
                    plan.Current = plan.Amount;
                }
                else
                {
                    plan.Current += step;
                }
                //plan.updateTimestamp();
                TableOperation insertOrMerge = TableOperation.InsertOrMerge(plan);
                _ = jobTable.ExecuteAsync(insertOrMerge);
                MakeQueueMessageAsync(jobQueue, plan, start, count, settings.SourcePath, settings.TargetPath);
            }
            return true;
        }


        private static int MakeQueueMessageAsync(CloudQueue cloudQueue, JobEntity jobEntity, 
            int start, int count, string sourcePath, string targetPath)
        {
            //AtomicInteger errorCounter = new AtomicInteger();
            Parallel.For(start, start + count, i =>
            {
                JObject jObject = new JObject();
                jObject.Add("taskID", jobEntity.PartitionKey);
                jObject.Add("jobID", i);
                jObject.Add("version", jobEntity.Version);
                jObject.Add("sourcePath", sourcePath);
                jObject.Add("targetPath", targetPath);

                CloudQueueMessage message = new CloudQueueMessage(jObject.ToString());
                try
                {
                    cloudQueue.AddMessageAsync(message);
                }
                catch (StorageException)
                {
                    //errorCounter.incrementAndGet();
                }
                Console.WriteLine(i);
            });
            return 1;
        }
    }
}
