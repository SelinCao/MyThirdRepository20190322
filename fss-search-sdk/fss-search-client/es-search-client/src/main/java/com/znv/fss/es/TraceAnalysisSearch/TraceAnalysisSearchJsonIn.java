package com.znv.fss.es.TraceAnalysisSearch;

/**
 * Created by Administrator on 2017/12/5.
 */
public class TraceAnalysisSearchJsonIn {
    private String id;
    private TraceAnalysisSearchQueryParam params;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setParams(TraceAnalysisSearchQueryParam params) {
        this.params = params;
    }

    public TraceAnalysisSearchQueryParam getParams() {
        return params;
    }
}
