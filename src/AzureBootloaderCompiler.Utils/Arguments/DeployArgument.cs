using CommandLine;
using System;
using System.Collections.Generic;
using System.Text;

namespace AzureBootloaderCompiler.Utils.Arguments
{
    [Verb("deploy", HelpText = "编译文件发布上线")]
    class DeployArgument
    {
        [Option('t', "task", Required = true, HelpText = "任务ID")]
        public string Task { get; set; }

        [Option('v', "version", Required = true, HelpText = "版本")]
        public string Version { get; set; }

        [Option('f', "settingFile", Required = true, HelpText = "配置文件")]
        public string SettingFile { get; set; }
    }
}
