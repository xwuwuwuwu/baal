package com.lab.azure.bootloader.utils;

import com.microsoft.azure.storage.table.TableServiceEntity;

import java.util.Date;

public class JobEntity extends TableServiceEntity {
    private String version;
    private Integer amount;
    private Integer current;
    private Date createAt;
    private Date updateAt;

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getAmount() {
        return this.amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }

    public Integer getCurrent() {
        return this.current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Date getCreateAt() {
        return this.createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public Date getUpdateAt() {
        return this.updateAt;
    }

    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }

    public void updateTimestamp() {
        if (this.createAt == null) {
            this.createAt = new Date();
        }
        this.updateAt = new Date();
    }
}