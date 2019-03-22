package com.znv.fss.es.TraceAnalysisSearch;

import java.util.List;

public class TraceAnalysisSearchTrailTimeQueryBucket {
    private List<TraceAnalysisTrailCameraQueryBucket>  timeBucket;
    private String enterTime;
    private int timeCount;

    public void setTimeBucket(List<TraceAnalysisTrailCameraQueryBucket> timeBucket) {
        this.timeBucket = timeBucket;
    }

    public List<TraceAnalysisTrailCameraQueryBucket> getTimeBucket() {
        return timeBucket;
    }

    public void setEnterTime(String enterTime) {
        this.enterTime = enterTime;
    }

    public String getEnterTime() {
        return enterTime;
    }

    public void setTimeCount(int timeCount) {
        this.timeCount = timeCount;
    }

    public int getTimeCount() {
        return timeCount;
    }

}
