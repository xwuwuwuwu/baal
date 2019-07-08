FROM mcr.microsoft.com/dotnet/core/sdk:2.2 AS runtime-image

RUN mkdir -p /home/site/wwwroot && \
    dotnet publish ./src/AzureBootloaderCompiler.csproj --output /home/site/wwwroot

FROM azure-bootloader:1.0 

COPY --from=runtime-image ["/home/site/wwwroot", "/home/site/wwwroot"]

CMD ["dotnet", "/azure-functions-host/Microsoft.Azure.WebJobs.Script.WebHost.dll"]
