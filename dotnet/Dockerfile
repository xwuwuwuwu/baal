FROM mcr.microsoft.com/dotnet/core/sdk:2.2 AS runtime-image

RUN mkdir /workspace
COPY ./src/ /workspace/
RUN rm -rf /workspace/bin
RUN rm -rf /workspace/obj

RUN mkdir -p /home/site/wwwroot && \
    dotnet publish /workspace/AzureBootloaderCompiler.csproj --output /home/site/wwwroot

FROM azure-bootloader:1.0 

COPY --from=runtime-image ["/home/site/wwwroot", "/home/site/wwwroot"]

CMD ["dotnet", "/azure-functions-host/Microsoft.Azure.WebJobs.Script.WebHost.dll"]
