using System;
using AzureBootloaderCompiler.QueueMaker;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;

namespace AzureBootloaderCompiler.Docker.Api
{
    public static class BooloaderCompilerFunction
    {
        [FunctionName("Compiler")]
        public static void Run([QueueTrigger("bootloaderjobs")]string jsonString, ILogger log)
        {
            log.LogInformation($"C# Queue trigger function processed: {jsonString}");
            BootLoaderCompiler.DoCompile(jsonString, log);
        }
    }
}
