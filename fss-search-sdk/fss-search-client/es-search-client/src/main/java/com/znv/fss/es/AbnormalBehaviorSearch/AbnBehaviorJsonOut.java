package com.znv.fss.es.AbnormalBehaviorSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class AbnBehaviorJsonOut {
    private List<AbnBehaviorQueryHit> hits;
    private int total;
    @JSONField(name = "errorCode")
    private int errorcode;
    private long took;

    public List<AbnBehaviorQueryHit> getHits() {
        return hits;
    }

    public void setHits(List<AbnBehaviorQueryHit> hits) {
        this.hits = hits;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getErrorcode() {
        return errorcode;
    }

    public void setErrorcode(int errorcode) {
        this.errorcode = errorcode;
    }

    public long getTook() {
        return took;
    }

    public void setTook(long took) {
        this.took = took;
    }
}
