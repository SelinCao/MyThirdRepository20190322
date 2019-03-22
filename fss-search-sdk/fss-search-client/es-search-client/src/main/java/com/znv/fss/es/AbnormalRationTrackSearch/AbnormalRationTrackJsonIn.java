package com.znv.fss.es.AbnormalRationTrackSearch;

/**
 * Created by User on 2017/8/25.
 */
public class AbnormalRationTrackJsonIn {
    private String id;
    private AbnormalRationTrackQueryParam params;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setParams(AbnormalRationTrackQueryParam params) {
        this.params = params;
    }

    public AbnormalRationTrackQueryParam getParams() {
        return this.params;
    }
}
