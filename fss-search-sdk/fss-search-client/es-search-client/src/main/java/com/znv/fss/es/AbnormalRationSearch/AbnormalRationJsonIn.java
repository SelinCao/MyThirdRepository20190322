package com.znv.fss.es.AbnormalRationSearch;

/**
 * Created by User on 2017/8/25.
 */
public class AbnormalRationJsonIn {
    private String id;
    private AbnormalRationQueryParam params;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setParams(AbnormalRationQueryParam params) {
        this.params = params;
    }

    public AbnormalRationQueryParam getParams() {
        return this.params;
    }
}
