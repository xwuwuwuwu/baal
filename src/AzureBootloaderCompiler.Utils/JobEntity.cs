using Microsoft.WindowsAzure.Storage.Table;
using System;
using System.Collections.Generic;
using System.Text;

namespace AzureBootloaderCompiler.Utils
{
    class JobEntity : TableEntity
    {
        public string Version { get; set; }
        public int Amount { get; set; }
        public int Current { get; set; }
        public DateTime CreateAt { get; set; }
        public DateTime UpdateAt { get; set; }
    }
}
