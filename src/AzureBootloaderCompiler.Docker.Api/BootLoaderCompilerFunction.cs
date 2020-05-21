using AzureBootloaderCompiler.QueueMaker;
using Microsoft.Azure.WebJobs;
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
            await BootLoaderCompiler.DoCompile(jsonString, logger);
        }
    }
}
