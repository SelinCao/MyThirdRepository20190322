package com.znv.fss.es.TraceAnalysisSearch;

import java.util.List;

public class TraceAnalysisSearchPersonQueryAgg {
    private List<TraceAnalysisSearchQueryBucket> lfBucket;
    private List<TraceAnalysisSearchQueryBucket> mfBucket;
    private List<TraceAnalysisSearchQueryBucket> hfBucket;

    public void setLfBucket(List<TraceAnalysisSearchQueryBucket> lfBucket) {
        this.lfBucket = lfBucket;
    }

    public List<TraceAnalysisSearchQueryBucket> getLfBucket() {
        return lfBucket;
    }

    public void setMfBucket(List<TraceAnalysisSearchQueryBucket> mfBucket) {
        this.mfBucket = mfBucket;
    }

    public List<TraceAnalysisSearchQueryBucket> getMfBucket() {
        return mfBucket;
    }

    public void setHfBucket(List<TraceAnalysisSearchQueryBucket> hfBucket) {
        this.hfBucket = hfBucket;
    }

    public List<TraceAnalysisSearchQueryBucket> getHfBucket() {
        return hfBucket;
    }

}
