package com.znv.fss.hbase.NightFreqSearch;

/**
 * Created by ZNV on 2017/6/7.
 */
public class NightFreqInput {
    private String[] officeIds;
    private String[] cameraIds;
    private String startTime;
    private String endTime;
    private int simThreshold = 89; // 相似度：50~100,默认89

    public void setOfficeIds(String[] officeIds) {
        if (officeIds != null) {
            this.officeIds = officeIds.clone();
        }
    }

    public String[] getOfficeIds() {
        String[] temp = this.officeIds;
        return temp;
    }

    public void setCameraIds(String[] cameraIds) {
        if (cameraIds != null) {
            this.cameraIds = cameraIds.clone();
        }
    }

    public String[] getCameraIds() {
        String[] temp = this.cameraIds;
        return temp;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStartTime() {
        return this.startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setSimThreshold(int simThreshold) {
        this.simThreshold = simThreshold;
    }

    public int getSimThreshold() {
        return this.simThreshold;
    }

}
