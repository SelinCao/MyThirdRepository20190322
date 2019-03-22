package com.znv.fss.hbase.NightFreqSearch;

import com.znv.fss.hbase.staytime.StayTimeInput;

/**
 * Created by ZNV on 2017/6/7.
 */
public class NightFreqReportServiceIn {
    private String id;
    private String type;
    private int analysis; //不传时默认为0
    private int size;
    private int frequency;
    private NightFreqInput nightFreq;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return this.size;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setNightFreq(NightFreqInput nightFreq) {
        this.nightFreq = nightFreq;
    }

    public NightFreqInput getNightFreq() {
        return this.nightFreq;
    }

    public void setAnalysis(int analysis) {
        this.analysis = analysis;
    }

    public int getAnalysis() {
        return this.analysis;
    }
}
