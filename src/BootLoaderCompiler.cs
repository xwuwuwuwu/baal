using Microsoft.Azure.WebJobs;
using Microsoft.Azure.WebJobs.Host;
using Microsoft.Extensions.Logging;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using NLog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
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
            var sw = Stopwatch.StartNew();
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
            var separateNumber = CompilerHelper.GetSeparateNumber();
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
            if (separateNumber <= 1000)
            {
                throw new CompileException($"separate number error");
            }
            var workspace = $"/tmp/bootloader/{version}/{taskID}/{jobID}";
            try
            {
                await BuildAsync(logger, version, taskID, jobID, sourcePath, targetPath, workspace, separateNumber);
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
            sw.Stop();
            logger.LogInformation($"cast : {sw.ElapsedMilliseconds} ms, version : {version}, taskID : {taskID}, jobID : {jobID} success");
        }

        private static async Task BuildAsync(Microsoft.Extensions.Logging.ILogger logger, string version, 
            string taskID, string jobID, string sourcePath, 
            string targetPath, string workspace, int separateNumber)
        {
            if (Directory.Exists(workspace))
            {
                Directory.Delete(workspace, true);
            }
            Directory.CreateDirectory(workspace);

            var sourceZip = $"{workspace}/bootloader.zip";
            {
                var sw = Stopwatch.StartNew();
                var buffer = await CompilerHelper.DownloadSourceCodeAsync(sourcePath);
                var fs = new FileStream(sourceZip, FileMode.CreateNew);
                fs.Write(buffer, 0, buffer.Length);
                fs.Flush();
                fs.Close();
                sw.Stop();
                logger.LogInformation($"task {taskID} download source code cost {sw.ElapsedMilliseconds} ms");
            }
            var sourceCodePath = $"{workspace}/sourceCode";
            {
                var sw = Stopwatch.StartNew();
                if (Directory.Exists(sourceCodePath))
                {
                    Directory.Delete(sourceCodePath);
                }
                Directory.CreateDirectory(sourceCodePath);
                {
                    ZipFile.ExtractToDirectory(sourceZip, sourceCodePath);
                }
                logger.LogInformation($"task {taskID} prepare cost {sw.ElapsedMilliseconds} ms");
            }

            var outputMap = new Dictionary<string, string>();

            var soPath = $"{workspace}/so";
            Directory.CreateDirectory(soPath);
            {
                var sw = Stopwatch.StartNew();
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
                sw.Stop();
                logger.LogInformation($"task {taskID} ndk build cost {sw.ElapsedMilliseconds} ms");
            }

            var outputID = jobID;
            {
                var sw = Stopwatch.StartNew();
                var jobNumber = int.Parse(jobID);
                var index = jobNumber / separateNumber;
                foreach (var kv in outputMap)
                {
                    var blobPath = $"{targetPath}/{version}/{taskID}/{index * separateNumber}/{outputID}/{kv.Value}";
                    await CompilerHelper.UploadOutputAsync(kv.Key, blobPath);
                }
                sw.Stop();
                logger.LogInformation($"task {taskID} upload to blob cost {sw.ElapsedMilliseconds} ms");
            }
            if (Directory.Exists(workspace))
            {
                Directory.Delete(workspace, true);
            }
        }
    }
}
