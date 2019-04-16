package com.battlelab;

public class BuilderTaskInfo {
    private String taskId;
    private String sourceContainer;
    private String sourceName;
    private String targetContainer;
    private String targetPath;

    public String getTaskId() {
        return taskId;
    }

    public BuilderTaskInfo setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public String getSourceContainer() {
        return sourceContainer;
    }

    public BuilderTaskInfo setSourceContainer(String sourceContainer) {
        this.sourceContainer = sourceContainer;
        return this;
    }

    public String getSourceName() {
        return sourceName;
    }

    public BuilderTaskInfo setSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
    }

    public String getTargetContainer() {
        return targetContainer;
    }

    public BuilderTaskInfo setTargetContainer(String targetContainer) {
        this.targetContainer = targetContainer;
        return this;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public BuilderTaskInfo setTargetPath(String targetPath) {
        this.targetPath = targetPath;
        return this;
    }
}
