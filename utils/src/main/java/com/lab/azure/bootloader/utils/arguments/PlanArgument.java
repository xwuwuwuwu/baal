package com.lab.azure.bootloader.utils.arguments;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;

import java.io.File;

/**
 * @author willsun
 */
public class PlanArgument {
    @Parameter
    private String task;
    @Parameter(names = "-c")
    private Integer count;
    @Parameter(names = "-s", converter = FileConverter.class)
    private File settingFile;
    @Parameter(names = "-v", required = false)
    private String version = "v1";

    public String getTask() {
        return this.task;
    }

    public Integer getCount() {
        return this.count;
    }

    public File getSettingFile() {
        return this.settingFile;
    }

    public String getVersion() {
        return this.version;
    }
}
