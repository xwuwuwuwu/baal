# 编译镜像




# COMMAND
    docker build -t bootloader . 编译镜像
    docker run -ti --rm -p 8080:80 bootloader 运行镜像， 并在推出后删除实例
    docker cp {container}:/workspace/blade-bootloader/libs dest
    docker tag bootloader:1.0 labtestdocker.azurecr.io/bootloader:1.0
    docker push labtestdocker.azurecr.io/bootloader:1.0
