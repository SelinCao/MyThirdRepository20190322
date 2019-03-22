package com.znv.fss.es.FssNightOutSearch;

import com.alibaba.fastjson.annotation.JSONField;
import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisSearchCameraQueryAgg;
import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisSearchTrailQueryAgg;


import java.util.List;

/**
 * Created by Administrator on 2017/12/5.
 */
public class NightOutSearchJsonOut {
    private List<NightOutSearchQueryHit> hits;
    private List<NightOutSearchQueryPersonHit> personHits;
    private TraceAnalysisSearchTrailQueryAgg trailQueryAgg;
    private int total;
    @JSONField(name = "errorCode")
    private int errorcode;
    private long took;

    public void setHits(List<NightOutSearchQueryHit> hits) {
        this.hits = hits;
    }

    public List<NightOutSearchQueryHit> getHits() {
        return hits;
    }

    public void setPersonHits(List<NightOutSearchQueryPersonHit> personHits) {
        this.personHits = personHits;
    }

    public List<NightOutSearchQueryPersonHit> getPersonHits() {
        return personHits;
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

    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }

    public TraceAnalysisSearchTrailQueryAgg getTrailQueryAgg() {
        return trailQueryAgg;
    }

    public void setTrailQueryAgg(TraceAnalysisSearchTrailQueryAgg trailQueryAgg) {
        this.trailQueryAgg = trailQueryAgg;
    }
}
