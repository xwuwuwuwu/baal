#!/bin/sh
#azure-build.sh armeabi /Users/willsun/workspace/blade-bootloader /Users/willsun/workspace/blade-bootloader/output
ERROR=8
if [ "$#" -ne "3" ]; then
    echo "incorrect argument number count"
    exit ${ERROR}
fi

ABI=$1
SOURCE_PATH=$2
OUTPUT_PATH=$3

echo "ABI : ${ABI}, SOURCE : ${SOURCE_PATH}, OUTPUT : ${OUTPUT_PATH}"

if [ -d ${OUTPUT_PATH} ]; then
    rm -rf ${OUTPUT_PATH}
    if [ $? -ne 0 ]; then
        echo "rm ${OUTPUT_PATH} error"
        exit ${ERROR}
    fi
fi
mkdir -p ${OUTPUT_PATH}
if [ $? -ne 0 ]; then
    echo "mkdir ${OUTPUT_PATH} error"
    exit ${ERROR}
fi

if [ -d "${SOURCE_PATH}/obj" ];then
    rm -rf "${SOURCE_PATH}/obj"
fi

if [ -d "${SOURCE_PATH}/libs" ];then
    rm -rf "${SOURCE_PATH}/libs"
fi

if [ -d "${SOURCE_PATH}/jni-backup" ];then
    rm -rf "${SOURCE_PATH}/jni-backup"
fi

if [ -d "${SOURCE_PATH}/jni-build" ];then
    rm -rf "${SOURCE_PATH}/jni-build"
fi

cp -r "${SOURCE_PATH}/jni" "${SOURCE_PATH}/jni-backup"
if [ $? -ne 0 ]; then
    echo "cp -r ${SOURCE_PATH}/jni ${SOURCE_PATH}/jni-backup error"
    exit ${ERROR}
fi

python3 "${SOURCE_PATH}/replace_str.py"
if [ $? -ne 0 ]; then
    echo "replace_str error"
    exit ${ERROR}
fi
echo "replace string success"
python3 "${SOURCE_PATH}/enc_str.py" "${SOURCE_PATH}/jni/strings.txt"
if [ $? -ne 0 ]; then
    echo "enc_str error"
    exit ${ERROR}
fi
#echo "encode string success"
ndk-build APP_ABI="${ABI}" NDK_PROJECT_PATH="${SOURCE_PATH}" NDK_LIBS_OUT="${OUTPUT_PATH}"
if [ $? -ne 0 ]; then
    echo "ndk-build error"
    exit ${ERROR}
fi

