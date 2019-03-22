package com.znv.fss.es.FssNightOutSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * Created by Administrator on 2017/12/5.
 */
public class NightOutSearchQueryParam {
    @JSONField(name = "date_start")
    private String dateStart;
    @JSONField(name = "date_End")
    private String dateEnd;
    @JSONField(name = "time_start")
    private String timeStart;
    @JSONField(name = "time_end")
    private String timeEnd;
    @JSONField(name = "office_id")
    private List<String> officeId;
    @JSONField(name = "camera_id")
    private List<String> cameraId;
    @JSONField(name = "fused_id")
    private String fusedId;
    @JSONField(name = "person_aggregation")
    private boolean personAggregation;
    @JSONField(name = "person_size")
    private int personSize;
    @JSONField(name = "count_order")
    private String countOrder="desc";
    @JSONField(name = "person_numbers")
    private int personNumbers=20;
    @JSONField(name = "trail_aggregation")
    private boolean trailAggregation;
    private int from;
    private int size;
    @JSONField(name = "sort_field")
    private String sortField;
    @JSONField(name = "sort_order")
    private String sortOrder;
    private int frequency;//频率阈值


    public void setDateStart(String dateStart) {
        this.dateStart = dateStart;
    }

    public String getDateStart() {
        return dateStart;
    }

    public void setDateEnd(String dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getDateEnd() {
        return dateEnd;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getTimeEnd() {
        return timeEnd;
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

    public String getFusedId() {
        return fusedId;
    }

    public void setFusedId(String fusedId) {
        this.fusedId = fusedId;
    }

    public void setPersonAggregation(boolean personAggregation) {
        this.personAggregation = personAggregation;
    }

    public boolean getPersonAggregation() {
        return personAggregation;
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

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getFrequency() {
        return frequency;
    }

    public String getCountOrder() {
        return countOrder;
    }

    public void setCountOrder(String countOrder) {
        this.countOrder = countOrder;
    }

    public void setPersonSize(int personSize) {
        this.personSize = personSize;
    }

    public int getPerosnSize() {
        return personSize;
    }

    public void setPersonNumbers(int personNumbers) {
        this.personNumbers = personNumbers;
    }

    public int getPersonNumbers() {
        return personNumbers;
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

}
