package com.znv.fss.es.FssHistorySearch;

/**
 * Created by Administrator on 2017/12/5.
 */
public class FssHistorySearchJsonIn {
    private String id;
    private FssHistorySearchQueryParam params;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setParams(FssHistorySearchQueryParam params) {
        this.params = params;
    }

    public FssHistorySearchQueryParam getParams() {
        return params;
    }
}
