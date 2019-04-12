#!/usr/bin/env bash

SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)

JAR_PATH="$SHELL_FOLDER/target/utils-0.0.1-SNAPSHOT-jar-with-dependencies.jar"

SETTING_ARGUMENT="-s $SHELL_FOLDER/bootloader_dev.cfg"
ARGUMENTS=""
while [ $# != 0 ]
do
    if [ $# == 1 ]; then
        ARGUMENTS="$ARGUMENTS $SETTING_ARGUMENT"
    fi
    ARGUMENTS="$ARGUMENTS $1"
    shift
done

SHELL="java -jar $JAR_PATH $ARGUMENTS"
echo ">>>>>$SHELL"
$SHELL

