using CommandLine;
using CommandLine.Text;
using System;
using System.Collections.Generic;
using System.Text;

namespace AzureBootloaderCompiler.Utils.Arguments
{
    [Verb("plan", HelpText = "生成编译任务, 记入至Table中")]
    class PlanArgument
    {
        [Option('t', "task", Required = true, HelpText = "任务ID")]
        public string Task { get; set; }

        [Option('c', "count", Required = true, HelpText = "编译总个数")]
        public int Count { get; set; }

        [Option('f', "settingFile", Required = true, HelpText = "配置文件")]
        public string SettingFile { get; set; }

        [Option('v', "version", Required = true, HelpText = "版本")]
        public string Version { get; set; }
    }
}
