using CommandLine;
using CommandLine.Text;
using System;
using System.Collections.Generic;
using System.Text;

namespace AzureBootloaderCompiler.Utils.Arguments
{
    [Verb("query", HelpText = "查询任务执行情况")]
    class QueryArgument
    {
        [Option('t', "task", Required = true, HelpText = "任务ID")]
        public string Task { get; set; }

        [Option('f', "settingFile", Required = true, HelpText = "配置文件")]
        public string SettingFile { get; set; }
    }
}
