package com.znv.fss.es.AbnormalRationTrackSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * Created by Administrator on 2017/6/6.
 */
public class AbnormalRationTrackQueryParam {
    @JSONField(name = "enter_time_start")
    private String enterTimeStart;
    @JSONField(name = "enter_time_end")
    private String enterTimeEnd;
    @JSONField(name = "office_id")
    private List<String> officeId;
    @JSONField(name = "camera_id")
    private List<String> cameraId;
    @JSONField(name = "topN")
    private int topn;
    @JSONField(name = "peerInterval")
    private int peerinterval;
    @JSONField(name = "sim_threshold")
    private double simThreshold;
    @JSONField(name = "sortType")
    private String sorttype; // 1-次数，2-时长
    @JSONField(name = "feature_value")
    private List<String> featureValue;
    @JSONField(name = "is_calcSim")
    private boolean isCalcSim;
    @JSONField(name = "filter_type")
    private String filterType;
    private int from;
    private int size;

    public void setEnterTimeStart(String enterTimeStart) {
        this.enterTimeStart = enterTimeStart;
    }

    public String getEnterTimeStart() {
        return this.enterTimeStart;
    }

    public void setEnterTimeEnd(String enterTimeEnd) {
        this.enterTimeEnd = enterTimeEnd;
    }

    public String getEnterTimeEnd() {
        return this.enterTimeEnd;
    }

    public void setOfficeId(List<String> officeId) {
        this.officeId = officeId;
    }

    public List<String> getOfficeId() {
        return this.officeId;
    }

    public void setCameraId(List<String> cameraId) {
        this.cameraId = cameraId;
    }

    public List<String> getCameraId() {
        return this.cameraId;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setIsCalcSim(boolean isCalcSim) {
        this.isCalcSim = isCalcSim;
    }

    public boolean getIsCalcSim() {
        return this.isCalcSim;
    }

    public double getSimThreshold() {
        return simThreshold;
    }

    public void setSimThreshold(double simThreshold) {
        this.simThreshold = simThreshold;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }

    public void setTopn(int topn) {
        this.topn = topn;
    }

    public int getTopn() {
        return topn;
    }

    public void setPeerinterval(int peerinterval) {
        this.peerinterval = peerinterval;
    }

    public int getPeerinterval() {
        return peerinterval;
    }

    public String getSorttype() {
        return sorttype;
    }

    public void setSorttype(String sorttype) {
        this.sorttype = sorttype;
    }

    public void setFeatureValue(List<String>featureValue) {
        this.featureValue = featureValue;
    }

    public List<String> getFeatureValue() {
        return featureValue;
    }

}
