//using AzureBootloaderCompiler.QueueMaker;
using Microsoft.Azure.WebJobs;
using Microsoft.Extensions.Logging;
using System;
using System.Threading.Tasks;

namespace AzureBootloaderCompiler.Docker.Api
{
    class BootLoaderCompilerFunction
    {
        [FunctionName("BootLoaderCompiler")]
        public static async Task RunAsync(
          [QueueTrigger("bootloaderjobsv2")]
            string jsonString,
          Microsoft.Extensions.Logging.ILogger logger)
        {
            logger.LogInformation($"C# QueueTrigger function executed at: {DateTime.Now}");
            //await BootLoaderCompiler.DoCompile(jsonString, logger);
        }
    }
}
