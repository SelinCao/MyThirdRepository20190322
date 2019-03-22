package com.znv.fss.hbase.NightFreqSearch;

import com.znv.fss.hbase.staytime.AnalysisData;

import java.util.List;

/**
 * Created by ZNV on 2017/10/24.
 */
public class NightAnalyses {
    private int frequency;
    private String img_url;
    private String uuid;
    private String enter_time;
   // private byte[] rowKey;
    private List<NightAnalysisData> analysisData;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setEnter_time(String enter_time) {
        this.enter_time = enter_time;
    }

    public String getEnter_time() {
        return this.enter_time;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }

    public String getImg_url() {
        return this.img_url;
    }



    public void setAnalysisData(List<NightAnalysisData> analysisData) {
        this.analysisData = analysisData;
    }

    public List<NightAnalysisData> getAnalysisData() {
        return analysisData;
    }

}
