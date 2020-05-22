using System;
using System.Threading.Tasks;
using AzureBootloaderCompiler.QueueMaker;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;

namespace AzureBootloaderCompiler.Docker.Api
{
    public static class BooloaderCompilerFunction
    {
        [FunctionName("Compiler")]
        public static async Task RunAsync([QueueTrigger("bootloaderjobs")]string jsonString, ILogger log)
        {
            log.LogInformation($"C# Queue trigger function processed: {jsonString}");
            await BootLoaderCompiler.DoCompileSync(jsonString, log);
        }
    }
}
