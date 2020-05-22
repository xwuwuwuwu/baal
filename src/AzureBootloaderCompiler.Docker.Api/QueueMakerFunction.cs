using System;
using System.Threading.Tasks;
using AzureBootloaderCompiler.QueueMaker;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;

namespace AzureBootloaderCompiler.Docker.Api
{
    public static class QueueMakerFunction

    {
        [FunctionName("QueueMaker")]
        public static async Task RunAsync([TimerTrigger("0 */1 * * * *")]TimerInfo myTimer, ILogger log)
        {
            log.LogInformation($"C# Timer trigger function executed at: {DateTime.Now}");
            _ = await QueueHelper.MakeQueueAsync(log);
        }
    }
}
