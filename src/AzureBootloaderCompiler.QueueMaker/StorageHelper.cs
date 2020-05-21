using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Blob;
using System;
using System.Linq;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks;

namespace AzureBootloaderCompiler.QueueMaker
{
    public class StorageHelper
    {
        public static int DEFAULT_MAX_RESULT = 1000;
        public static int DEFAULT_STEP = 10000;

        public static async Task<long> GetBlobFileCountAsync(CloudBlobContainer blobContainer, string path)
        {
            return await GetBlobFileCountAsync(blobContainer, path, DEFAULT_MAX_RESULT);
        }

        public static async Task<long> GetBlobFileCountAsync(CloudBlobContainer blobContainer, string path, int maxResult)
        {
            BlobContinuationToken token = null;
            long count = 0;
            if (maxResult <= 0)
            {
                maxResult = DEFAULT_MAX_RESULT;
            }
            do
            {
                BlobResultSegment resultSegment = await blobContainer.ListBlobsSegmentedAsync(path, true, BlobListingDetails.Metadata,
                        maxResult, token, null, null);
                token = resultSegment.ContinuationToken;
                count += resultSegment.Results.Count();
            } while (token != null);
            return count;
        }
    }
}
