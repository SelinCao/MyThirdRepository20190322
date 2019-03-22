package com.znv.fss.es.AbnormalBehaviorSearch;


public class AbnBehaviorJsonIn {

    private String id;
    private AbnBehaviorQueryParam params;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setParams(AbnBehaviorQueryParam params) {
        this.params = params;
    }

    public AbnBehaviorQueryParam getParams() {
        return params;
    }
}
