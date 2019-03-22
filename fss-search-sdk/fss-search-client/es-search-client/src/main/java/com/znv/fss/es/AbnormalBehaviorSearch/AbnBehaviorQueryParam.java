package com.znv.fss.es.AbnormalBehaviorSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class AbnBehaviorQueryParam {
    @JSONField(name = "enter_time_start")
    private String enterTimeStart;
    @JSONField(name = "enter_time_end")
    private String enterTimeEnd;
    @JSONField(name = "office_id")
    private List<String> officeId;
    @JSONField(name = "lib_id")
    private List<String> libId;

    @JSONField(name = "sort_field")
    private String sortField;
    @JSONField(name = "sort_order")
    private String sortOrder;

    @JSONField(name = "is_lib")
    private boolean hasLibId;
    @JSONField(name = "is_office")
    private boolean hasOfficeId;

    private int from;
    private int size;


    public String getEnterTimeStart() {
        return enterTimeStart;
    }

    public void setEnterTimeStart(String enterTimeStart) {
        this.enterTimeStart = enterTimeStart;
    }

    public String getEnterTimeEnd() {
        return enterTimeEnd;
    }

    public void setEnterTimeEnd(String enterTimeEnd) {
        this.enterTimeEnd = enterTimeEnd;
    }

    public List<String> getOfficeId() {
        return officeId;
    }

    public void setOfficeId(List<String> officeId) {
        this.officeId = officeId;
    }

    public List<String> getLibId() {
        return libId;
    }

    public void setLibId(List<String> libId) {
        this.libId = libId;
    }

    public boolean isHasLibId() {
        return hasLibId;
    }

    public void setHasLibId(boolean hasLibId) {
        this.hasLibId = hasLibId;
    }

    public boolean isHasOfficeId() {
        return hasOfficeId;
    }

    public void setHasOfficeId(boolean hasOfficeId) {
        this.hasOfficeId = hasOfficeId;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
