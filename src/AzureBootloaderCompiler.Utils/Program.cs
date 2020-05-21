using System;
using System.Threading.Tasks;
using CommandLine;
using AzureBootloaderCompiler.Utils.Arguments;

namespace AzureBootloaderCompiler.Utils
{
    class Program
    {
        static async Task Main(string[] args)
        {
            Console.WriteLine("Hello World!");
            await Parser.Default
                .ParseArguments<PlanArgument, DeployArgument, QueryArgument>(args)
                .MapResult(
                    (PlanArgument argument) => MakePlanAsync(argument),
                    (DeployArgument argument) => DoDeploy(argument),
                    (QueryArgument argument) => DoQuery(argument),
                    error => Task.FromResult(0));
        }

        private static async Task<int> MakePlanAsync(PlanArgument argument)
        {
            await PlanHelper.MakePlanAsync(argument);
            return 0;
        }

        private static async Task<int> DoDeploy(DeployArgument argument)
        {
            return 0;
        }

        private static async Task<int> DoQuery(QueryArgument argument)
        {
            return 0;
        }
    }
}
