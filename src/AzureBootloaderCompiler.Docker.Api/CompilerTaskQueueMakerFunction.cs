using System;
using System.Threading.Tasks;
//using AzureBootloaderCompiler.QueueMaker;
using Microsoft.Azure.WebJobs;
using Microsoft.Extensions.Logging;

namespace AzureBootloaderCompiler.Docker.Api
{
    public static class CompilerTaskQueueMakerFunction
    {
        [FunctionName("CompilerQueue")]
        [Singleton]
        public static async Task RunAsync([TimerTrigger("0 */1 * * * *")]TimerInfo myTimer, ILogger logger)
        {
            logger.LogInformation($"C# Timer trigger function executed at: {DateTime.Now}");
            //await QueueHelper.MakeQueueAsync(logger);
        }
    }
}
