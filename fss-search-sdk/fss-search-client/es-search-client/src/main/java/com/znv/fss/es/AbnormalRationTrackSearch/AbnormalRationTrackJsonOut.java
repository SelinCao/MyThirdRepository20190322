package com.znv.fss.es.AbnormalRationTrackSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class AbnormalRationTrackJsonOut {
    private String id;
    private String type;
    @JSONField(name = "errorCode")
    private int errorcode;
    private int total;
    private long time;
    private int count;
    @JSONField(name = "peerTrackData")
    private List<AbnormalRationTrackoutputData> peerTrackData;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setErrorcode(int errorcode) {
        this.errorcode = errorcode;
    }

    public int getErrorcode() {
        return errorcode;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<AbnormalRationTrackoutputData> getPeerTrackData() {
        return peerTrackData;
    }

    public void setPeerTrackData(List<AbnormalRationTrackoutputData> peerTrackData) {
        this.peerTrackData = peerTrackData;
    }
}
