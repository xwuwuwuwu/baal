package com.battlelab.api.alert;

import com.microsoft.azure.storage.table.TableServiceEntity;

public class DownloaderAlertRecordEntity extends TableServiceEntity {

    private int totalAlertCount;
    private int frequencyAlertCount;

    public int getTotalAlertCount() {
        return totalAlertCount;
    }

    public void setTotalAlertCount(int totalAlertCount) {
        this.totalAlertCount = totalAlertCount;
    }

    public int getFrequencyAlertCount() {
        return frequencyAlertCount;
    }

    public void setFrequencyAlertCount(int frequencyAlertCount) {
        this.frequencyAlertCount = frequencyAlertCount;
    }
}
