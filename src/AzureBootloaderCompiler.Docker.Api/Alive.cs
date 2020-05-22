using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Extensions.Http;
using Microsoft.Extensions.Logging;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace AzureBootloaderCompiler.Docker.Api
{
    public static class Alive
    {
        [FunctionName("Alive")]
        public static async Task<IActionResult> RunAsync(
            [HttpTrigger(AuthorizationLevel.Function, "get", "post", Route = null)]
            HttpRequest request,
            ILogger logger)
        {
            Dictionary<string, string> map = new Dictionary<string, string>();
            logger.LogInformation("environment >>>>>>>>>>");
            var keys = Environment.GetEnvironmentVariables().Keys;
            foreach (var key in keys)
            {
                logger.LogInformation($"key : {key.ToString()}, value : {Environment.GetEnvironmentVariable(key.ToString())}");
                map[key.ToString()] = Environment.GetEnvironmentVariable(key.ToString());
            }
            logger.LogInformation("environment <<<<<<<<<<");
            
            return await Task.FromResult<IActionResult>(new OkObjectResult(map));
        }
    }
}
