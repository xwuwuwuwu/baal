using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using NLog;
using System;
using System.Collections.Generic;
using System.IO;
using System.IO.Compression;
using System.Threading.Tasks;

namespace AzureBootloaderCompiler
{
    public static class BootLoaderCompiler
    {
        public static IEnumerable<string> ABIS = new List<string>() { "armeabi", "arm64-v8a" };

        [FunctionName("BootLoaderCompiler")]
        public static async Task RunAsync(
            [QueueTrigger("bootloaderjobs")]
            string jsonString,
            Microsoft.Extensions.Logging.ILogger logger)
        {
            var js = JsonConvert.DeserializeObject<JObject>(jsonString);
            if (!js.TryGetValue("taskID", out var taskIDToken))
            {
                throw new CompileException($"taskID is null");
            }
            if (!js.TryGetValue("sourcePath", out var sourcePathToken))
            {
                throw new CompileException($"sourcePath is null");
            }
            if (!js.TryGetValue("targetPath", out var targetPathToken))
            {
                throw new CompileException($"targetPath is null");
            }
            var taskID = taskIDToken.ToObject<string>();
            var sourcePath = sourcePathToken.ToObject<string>();
            var targetPath = targetPathToken.ToObject<string>();
            if (string.IsNullOrWhiteSpace(taskID))
            {
                throw new CompileException($"taskID is empty");
            }
            if (string.IsNullOrWhiteSpace(sourcePath))
            {
                throw new CompileException($"sourcePath is empty");
            }
            if (string.IsNullOrWhiteSpace(targetPath))
            {
                throw new CompileException($"targetPath is empty");
            }

            Logger nlogger = CompilerHelper.GetLogger(taskID);


            var workspace = $"/tmp/{taskID}";

            if (Directory.Exists(workspace))
            {
                Directory.Delete(workspace, true);
            }
            Directory.CreateDirectory(workspace);

            var sourceZip = $"{workspace}/bootloader.zip";
            {
                var buffer = await CompilerHelper.DownloadSourceCodeAsync(sourcePath);
                var fs = new FileStream(sourceZip, FileMode.CreateNew);
                fs.Write(buffer, 0, buffer.Length);
                fs.Flush();
                fs.Close();
            }
            var sourceCodePath = $"{workspace}/sourceCode";
            if (Directory.Exists(sourceCodePath))
            {
                Directory.Delete(sourceCodePath);
            }
            Directory.CreateDirectory(sourceCodePath);
            {
                ZipFile.ExtractToDirectory(sourceZip, sourceCodePath);
            }

            var outputMap = new Dictionary<string, string>();

            var soPath = $"{workspace}/so";
            Directory.CreateDirectory(soPath);

            foreach (var abi in ABIS)
            {
                string filename;
                var outputPath = $"{workspace}/output";
                switch (abi)
                {
                    case "armeabi":
                        filename = "bl32";
                        break;
                    case "arm64-v8a":
                        filename = "bl64";
                        break;
                    default:
                        throw new CompileException($"NDK unsupport {abi}");
                }
                var outputSoPath = $"{outputPath}/{abi}/libbootloader.so";

                var buildSpace = $"{workspace}/buildSpace";
                if (Directory.Exists(buildSpace))
                {
                    Directory.Delete(buildSpace, true);
                }
                Directory.CreateDirectory(buildSpace);
                CompilerHelper.DirectoryCopy(sourceCodePath, buildSpace);

                CompilerHelper.NdkBuild(abi, buildSpace, outputPath);
                var targetSoPath = $"{soPath}/{filename}";
                File.Copy(outputSoPath, targetSoPath);
                outputMap.Add(targetSoPath, filename);
            }
            var outputID = await CompilerHelper.GetIDFromRedisAsync(taskID);
            foreach (var kv in outputMap)
            {
                var blobPath = $"{targetPath}/{taskID}/{outputID}/{kv.Value}";
                await CompilerHelper.UploadOutputAsync(kv.Key, blobPath);
            }
        }
    }
}
