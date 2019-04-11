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
            if (!js.TryGetValue("version", out var versionToken))
            {
                throw new CompileException($"jobID is null");
            }
            if (!js.TryGetValue("taskID", out var taskIDToken))
            {
                throw new CompileException($"taskID is null");
            }
            if (!js.TryGetValue("jobID", out var jobIDToken))
            {
                throw new CompileException($"jobID is null");
            }
            if (!js.TryGetValue("sourcePath", out var sourcePathToken))
            {
                throw new CompileException($"sourcePath is null");
            }
            if (!js.TryGetValue("targetPath", out var targetPathToken))
            {
                throw new CompileException($"targetPath is null");
            }
            var version = versionToken.ToObject<string>();
            var taskID = taskIDToken.ToObject<string>();
            var jobID = jobIDToken.ToObject<string>();
            var sourcePath = sourcePathToken.ToObject<string>();
            var targetPath = targetPathToken.ToObject<string>();
            if (string.IsNullOrWhiteSpace(version))
            {
                throw new CompileException($"version is empty");
            }
            if (string.IsNullOrWhiteSpace(taskID))
            {
                throw new CompileException($"taskID is empty");
            }
            if (string.IsNullOrWhiteSpace(jobID))
            {
                throw new CompileException($"jobID is empty");
            }
            if (string.IsNullOrWhiteSpace(sourcePath))
            {
                throw new CompileException($"sourcePath is empty");
            }
            if (string.IsNullOrWhiteSpace(targetPath))
            {
                throw new CompileException($"targetPath is empty");
            }

            var workspace = $"/tmp/bootloader/{version}/{taskID}/{jobID}";
            try
            {
                await BuildAsync(taskID, jobID, sourcePath, targetPath, workspace);
            }
            catch (AggregateException ae)
            {
                logger.LogInformation(ae.InnerException.Message);
                throw ae.InnerException;
            }
            catch (Exception e)
            {
                logger.LogInformation(e.Message);
                throw e;
            }
            finally
            {
                if (Directory.Exists(workspace))
                {
                    Directory.Delete(workspace, true);
                }
            }
            logger.LogInformation($"version : {version}, taskID : {taskID}, jobID : {jobID} success");
        }

        private static async Task BuildAsync(string version, string taskID, string jobID, string sourcePath, string targetPath, string workspace)
        {
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
            var outputID = jobID;
            foreach (var kv in outputMap)
            {
                var blobPath = $"{targetPath}/{version}/{taskID}/{outputID}/{kv.Value}";
                await CompilerHelper.UploadOutputAsync(kv.Key, blobPath);
            }
            if (Directory.Exists(workspace))
            {
                Directory.Delete(workspace, true);
            }
        }
    }
}
