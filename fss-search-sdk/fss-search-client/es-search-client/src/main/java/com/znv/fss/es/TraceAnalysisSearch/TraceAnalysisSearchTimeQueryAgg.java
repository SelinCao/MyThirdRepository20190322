package com.znv.fss.es.TraceAnalysisSearch;

import java.util.List;

public class TraceAnalysisSearchTimeQueryAgg {
    private List<TraceAnalysisTimeQueryBucket> timeBucket;

    public void setCameraBucket(List<TraceAnalysisTimeQueryBucket> timeBucket) {
        this.timeBucket = timeBucket;
    }

    public List<TraceAnalysisTimeQueryBucket> getTimeBucket() {
        return timeBucket;
    }

}
