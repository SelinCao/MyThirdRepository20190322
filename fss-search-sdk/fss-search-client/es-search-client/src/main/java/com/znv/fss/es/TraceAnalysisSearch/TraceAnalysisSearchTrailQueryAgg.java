package com.znv.fss.es.TraceAnalysisSearch;

import java.util.List;

public class TraceAnalysisSearchTrailQueryAgg {
    private List<TraceAnalysisSearchTrailTimeQueryBucket> trailBucket;

    public void setTrailBucket(List<TraceAnalysisSearchTrailTimeQueryBucket> trailBucket) {
        this.trailBucket = trailBucket;
    }

    public List<TraceAnalysisSearchTrailTimeQueryBucket> getTrailBucket() {
        return trailBucket;
    }

}
