package com.battlelab.api.v2;

import com.microsoft.azure.storage.table.TableServiceEntity;

import java.util.Date;

public class DomainRecordEntity extends TableServiceEntity {

    private String tag;

    private Date recordAt;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Date getRecordAt() {
        return recordAt;
    }

    public void setRecordAt(Date recordAt) {
        this.recordAt = recordAt;
    }
}
