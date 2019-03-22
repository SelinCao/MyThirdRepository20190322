package com.znv.fss.es.FssNightOutSearch;

import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisSearchQueryParam;

/**
 * Created by Administrator on 2017/12/5.
 */
public class NightOutSearchJsonIn {
    private String id;
    private NightOutSearchQueryParam params;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setParams(NightOutSearchQueryParam params) {
        this.params = params;
    }

    public NightOutSearchQueryParam getParams() {
        return params;
    }
}
