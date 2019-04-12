package com.lab.azure.bootloader.utils.arguments;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;

/**
 * @author willsun
 */
public class DeployArgument {
    @Parameter
    private String task;
    @Parameter(names = "-v")
    private String version;
    @Parameter(names = "-s", converter = FileConverter.class)
    private File settingFile;

    public String getTask() {
        return this.task;
    }

    public String getVersion() {
        return this.version;
    }

    public File getSettingFile() {
        return this.settingFile;
    }
}
