# 生产环境配置
```
"AzureWebJobsStorage": "DefaultEndpointsProtocol=https;AccountName=millionlize;AccountKey=/RVs5ynXEReL+z9ERbYAfIJpM/zzTI8zkAbiXhyv95XbxjACDDU+DkZgyCqg24Y0fy+XYspIISAL927XKK9Owg==;EndpointSuffix=core.windows.net"
"DownloaderUris_V1": "127.0.0.1:7071/api/download/v1/,127.0.0.1:7071/api/download/v1/"
"job_queue": "bootloaderjobs"
"job_table": "planedjobs"
"job_history_table": "finishedjobs"
"deploy_table": "deploy"
"source_path": "https://millionlize.blob.core.windows.net/sourcecode/bootloader.zip"
"target_path": "https://millionlize.blob.core.windows.net/bootloaderstorage"
Blob文件统计每次的返回记录最大数
"max_result":"1000"
计划任务的每次个数
"step":"10000"
计划下一个任务的条件, 超过已经编译的Blob个数则启动
"step_threshold":"6000"

```
 