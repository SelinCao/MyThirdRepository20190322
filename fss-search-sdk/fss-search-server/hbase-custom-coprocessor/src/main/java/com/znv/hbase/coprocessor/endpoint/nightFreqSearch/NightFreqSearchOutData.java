package com.znv.hbase.coprocessor.endpoint.nightFreqSearch;

/**
 * Created by ZNV on 2017/5/27.
 */
public class NightFreqSearchOutData {
    private long durationTime = 0L; // 列里面的hbase时长
    private int frequency = 1; //抓拍频次
    private int simCount = 0; // 相似度超过阈值的计数
    private int groupId = -1; // 所属分组Id，默认-1：未分组
    private String personId = "0";
    private byte[] rowKey;
    private byte[] feature;
    private String cameraId = "";
    private String cameraName = "";
    private String imgUrl = "";
    private String bigPictureUuid= "";
    private String uuid = "";

    public long getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(long durationTime) {
        this.durationTime = durationTime;
    }

    public byte[] getRowKey() {
        byte[] temp = this.rowKey;
        return temp;
    }

    public void setRowKey(byte[] rowKey) {
        if (rowKey != null) {
            this.rowKey = rowKey.clone();
        }
    }

    public byte[] getFeature() {
        byte[] temp = this.feature;
        return temp;
    }

    public void setFeature(byte[] feature) {
        if (feature != null) {
            this.feature = feature.clone();
        }
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getSimCount() {
        return simCount;
    }

    public void setSimCount(int simCount) {
        this.simCount = simCount;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId= cameraId;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName= cameraName;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getBigPictureUuid() {
        return bigPictureUuid;
    }

    public void setBigPictureUuid(String bigPictureUuid) {
        this.bigPictureUuid = bigPictureUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
