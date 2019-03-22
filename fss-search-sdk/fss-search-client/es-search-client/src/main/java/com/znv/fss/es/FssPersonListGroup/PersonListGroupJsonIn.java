package com.znv.fss.es.FssPersonListGroup;

/**
 * Created by ZNV on 2018/12/16.
 */
public class PersonListGroupJsonIn {
    private String id;
    private PersonListGroupQueryParam params;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setParams(PersonListGroupQueryParam params) {
        this.params = params;
    }

    public PersonListGroupQueryParam getParams() {
        return params;
    }
}
