package com.znv.fss.es.AbnormalRationSearch;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 目标人信息
 * Created by zhuhx on 2018/3/6.
 */
public class AimPersonQueryHit {
    private String uuid = "";
    private String imgUrl = "";
    @JSONField(name = "enter_time")
    private String enterTime;
    @JSONField(name = "leave_time")
    private String leaveTime;
    @JSONField(name = "duration_time")
    private long durationTime = 1L;
    private float sim = 0.0f;
    @JSONField(name = "camera_id")
    private String cameraId;
    @JSONField(name = "camera_name")
    private String cameraName = "";

    private double gpsx = 0.0;
    private double gpsy = 0.0;
    private double gpsz = 0.0;


    public float getSim() {
        return sim;
    }

    public void setSim(float sim) {
        this.sim = sim;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getEnterTime() {
        return enterTime;
    }

    public void setEnterTime(String enterTime) {
        this.enterTime = enterTime;
    }

    public String getLeaveTime() {
        return leaveTime;
    }

    public void setLeaveTime(String leaveTime) {
        this.leaveTime = leaveTime;
    }

    public long getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(long durationTime) {
        this.durationTime = durationTime;
    }


    public double getGpsx() {
        return gpsx;
    }

    public void setGpsx(double gpsx) {
        this.gpsx = gpsx;
    }

    public double getGpsy() {
        return gpsy;
    }

    public void setGpsy(double gpsy) {
        this.gpsy = gpsy;
    }

    public double getGpsz() {
        return gpsz;
    }

    public void setGpsz(double gpsz) {
        this.gpsz = gpsz;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }
}
