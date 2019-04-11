using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Blob;
using NLog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading.Tasks;

namespace AzureBootloaderCompiler
{
    public static class CompilerHelper
    {
        public static CloudStorageAccount GetAccount()
        {
            var connectionString = Environment.GetEnvironmentVariable("AzureWebJobsStorage");
            if (!CloudStorageAccount.TryParse(connectionString, out var account))
            {
                throw new CompileException($"Account error");
            }
            return account;
        }

        public static async Task<byte[]> DownloadSourceCodeAsync(string path)
        {
            var account = GetAccount();
            var uri = new Uri(path);
            var blob = new CloudBlob(uri, account.CreateCloudBlobClient());
            await blob.FetchAttributesAsync();
            var length = (int)blob.Properties.Length;
            var buffer = new byte[length];
            var count = await blob.DownloadToByteArrayAsync(buffer, 0);
            if (count != length)
            {
                throw new CompileException("Download source code error");
            }
            return buffer;
        }

        public static async Task UploadOutputAsync(string outputPath, string blobPath)
        {
            var account = GetAccount();
            var uri = new Uri(blobPath);
            var blob = new CloudBlockBlob(uri, account.CreateCloudBlobClient());
            if (await blob.ExistsAsync())
            {
                throw new CompileException($"blob {blobPath} exists");
            }
            await blob.UploadFromFileAsync(outputPath);
        }

        public static void NdkBuild(string abi, string source, string output)
        {
            {
                var psi = new ProcessStartInfo
                {
                    FileName = $"/bin/sh",
                    Arguments = $"-c \"chmod a+x {source}/azure-build.sh\"",
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                var ps = Process.Start(psi);
                ps.WaitForExit();
                int errorCode = ps.ExitCode;
                if (errorCode != 0)
                {
                    throw new CompileException($"chmod error");
                }
            }

            {
                var psi = new ProcessStartInfo
                {
                    FileName = $"/bin/sh",
                    Arguments = $"-c \"{source}/azure-build.sh {abi} {source} {output}\"",
                    UseShellExecute = false,
                    CreateNoWindow = true
                };
                var ps = Process.Start(psi);
                ps.WaitForExit();
                int errorCode = ps.ExitCode;
                if (errorCode != 0)
                {
                    throw new CompileException($"ndk build {abi} error");
                }
            }
        }

        public static void DirectoryCopy(string source, string destination, bool recursive = true)
        {
            DirectoryInfo di = new DirectoryInfo(source);

            if (!di.Exists)
            {
                throw new DirectoryNotFoundException(
                    "Source directory does not exist or could not be found: "
                    + source);
            }

            DirectoryInfo[] dis = di.GetDirectories();
            if (!Directory.Exists(destination))
            {
                Directory.CreateDirectory(destination);
            }

            FileInfo[] files = di.GetFiles();
            foreach (FileInfo file in files)
            {
                string path = Path.Combine(destination, file.Name);
                file.CopyTo(path, false);
            }

            if (recursive)
            {
                foreach (DirectoryInfo subdi in dis)
                {
                    string path = Path.Combine(destination, subdi.Name);
                    DirectoryCopy(subdi.FullName, path, recursive);
                }
            }
        }

        public static Logger GetLogger(string taskID)
        {
            return LogManager.GetLogger(taskID);
        }
    }
}
