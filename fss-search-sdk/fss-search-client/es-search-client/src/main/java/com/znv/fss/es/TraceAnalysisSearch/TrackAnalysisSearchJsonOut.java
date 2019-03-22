package com.znv.fss.es.TraceAnalysisSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * Created by Administrator on 2017/12/5.
 */
public class TrackAnalysisSearchJsonOut {
    private List<TraceAnalysisSearchQueryHit> hits;
    private TraceAnalysisSearchPersonQueryAgg personQueryAgg;
    private TraceAnalysisSearchCameraQueryAgg cameraQueryAgg;
    private TraceAnalysisSearchTimeQueryAgg timeQueryAgg;
    private TraceAnalysisSearchTrailQueryAgg trailQueryAgg;
    private int total;
    @JSONField(name = "errorCode")
    private int errorcode;
    private int took;

    public void setHits(List<TraceAnalysisSearchQueryHit> hits) {
        this.hits = hits;
    }

    public List<TraceAnalysisSearchQueryHit> getHits() {
        return hits;
    }

    public void setPersonQueryAgg(TraceAnalysisSearchPersonQueryAgg personQueryAgg) {
        this.personQueryAgg = personQueryAgg;
    }

    public TraceAnalysisSearchPersonQueryAgg  getPersonQueryAgg() {
        return personQueryAgg;
    }

    public void setCameraQueryAgg(TraceAnalysisSearchCameraQueryAgg cameraQueryAgg) {
        this.cameraQueryAgg = cameraQueryAgg;
    }

    public TraceAnalysisSearchCameraQueryAgg  getCamersQueryAgg() {
        return cameraQueryAgg;
    }

    public void setTimeQueryAgg(TraceAnalysisSearchTimeQueryAgg timeQueryAgg) {
        this.timeQueryAgg = timeQueryAgg;
    }

    public TraceAnalysisSearchTrailQueryAgg  getTrailQueryAgg() {
        return trailQueryAgg;
    }

    public void setTrailQueryAgg(TraceAnalysisSearchTrailQueryAgg trailQueryAgg) {
        this.trailQueryAgg = trailQueryAgg;
    }

    public TraceAnalysisSearchTimeQueryAgg  getTimeQueryAgg() {
        return timeQueryAgg;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotal() {
        return total;
    }

    public void setErrorcode(int errorcode) {
        this.errorcode = errorcode;
    }

    public int getErrorcode() {
        return errorcode;
    }

    public void setTook(int took) {
        this.took = took;
    }

    public int getTook() {
        return took;
    }
}
