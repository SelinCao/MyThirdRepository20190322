package com.znv.fss.es.TraceAnalysisSearch;

import java.util.List;

public class TraceAnalysisSearchCameraQueryAgg {
    private List<TraceAnalysisCameraQueryBucket> cameraBucket;

    public void setCameraBucket(List<TraceAnalysisCameraQueryBucket> cameraBucket) {
        this.cameraBucket = cameraBucket;
    }

    public List<TraceAnalysisCameraQueryBucket> getCameraBucket() {
        return cameraBucket;
    }

}
