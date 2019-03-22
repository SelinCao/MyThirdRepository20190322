package com.znv.fss.es.TraceAnalysisSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * Created by Administrator on 2017/12/5.
 */
public class TraceAnalysisSearchQueryParam {
    @JSONField(name = "enter_time_start")
    private String enterTimeStart;
    @JSONField(name = "enter_time_end")
    private String enterTimeEnd;
    @JSONField(name = "office_id")
    private List<String> officeId;
    @JSONField(name = "camera_id")
    private List<String> cameraId;
    private float similarity;
    @JSONField(name = "person_id")
    private String personId;
    @JSONField(name = "lib_id")
    private List<String> libId;
    @JSONField(name = "search_interval")
    private String searchInterval;
    @JSONField(name = "count_order")
    private String countOrder;
    @JSONField(name = "time_aggregation")
    private boolean timeAggregation;
    @JSONField(name = "person_aggregation")
    private boolean personAggregation;
    @JSONField(name = "person_size")
    private int personSize;
    @JSONField(name = "camera_aggregation")
    private boolean cameraAggregation;
    @JSONField(name = "trail_aggregation")
    private boolean trailAggregation;
    private int from;
    private int size;
    @JSONField(name = "sort_field")
    private String sortField;
    @JSONField(name = "sort_order")
    private String sortOrder;
    int hFThreshold;
    int lFThreshold;
    int top = 10; //默认10

    public void setEnterTimeStart(String enterTimeStart) {
        this.enterTimeStart = enterTimeStart;
    }
    public String getEnterTimeStart() {
        return enterTimeStart;
    }

    public void setEnterTimeEnd(String enterTimeEnd) {
        this.enterTimeEnd = enterTimeEnd;
    }

    public String getEnterTimeEnd() {
        return enterTimeEnd;
    }

    public void setOfficeId(List<String> officeId) {
        this.officeId = officeId;
    }

    public List<String> getOfficeId() {
        return officeId;
    }

    public void setCameraId(List<String> cameraId) {
        this.cameraId = cameraId;
    }

    public List<String> getCameraId() {
        return cameraId;
    }

    public void setSearchInterval(String searchInterval) {
        this.searchInterval = searchInterval;
    }

    public String getSearchInterval() {
        return searchInterval;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getPersonId() {
        return personId;
    }

    public void setLibId(List<String> libId) {
        this.libId = libId;
    }

    public List<String> getLibId() {
        return libId;
    }

    public void setCountOrder(String countOrder) {
        this.countOrder = countOrder;
    }

    public String getCountOrder() {
        return countOrder;
    }

    public void setCameraAggregation(boolean cameraAggregation) {
        this.cameraAggregation = cameraAggregation;
    }

    public boolean getCameraAggregation() {
        return cameraAggregation;
    }

    public void setPersonAggregation(boolean personAggregation) {
        this.personAggregation = personAggregation;
    }

    public boolean getPersonAggregation() {
        return personAggregation;
    }

    public void setTimeAggregation(boolean timeAggregation) {
        this.timeAggregation = timeAggregation;
    }

    public boolean getTimeAggregation() {
        return timeAggregation;
    }

    public void setTrailAggregation(boolean trailAggregation) {
        this.trailAggregation = trailAggregation;
    }

    public boolean getTrailAggregation() {
        return trailAggregation;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getFrom() {
        return from;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public void setPersonSize(int personSize) {
        this.personSize = personSize;
    }

    public int getPerosnSize() {
        return personSize;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setHFThreshold(int hFThreshold) {
        this.hFThreshold = hFThreshold;
    }

    public int getHFThreshold() {
        return hFThreshold;
    }
    public void setLFThreshold(int lFThreshold) {
        this.lFThreshold = lFThreshold;
    }

    public int getLFThreshold() {
        return lFThreshold;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getTop() {
        return top;
    }

}
