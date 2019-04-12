package com.lab.azure.bootloader.utils.arguments;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;

/**
 * @author willsun
 */
public class QueryArgument {
    @Parameter
    private String task;
    @Parameter(names = "-s", converter = FileConverter.class)
    private File settingFile;

    public String getTask() {
        return this.task;
    }

    public File getSettingFile() {
        return this.settingFile;
    }
}
