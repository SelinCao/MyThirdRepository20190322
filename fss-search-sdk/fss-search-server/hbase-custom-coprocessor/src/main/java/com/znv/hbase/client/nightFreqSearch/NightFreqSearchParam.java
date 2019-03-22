package com.znv.hbase.client.nightFreqSearch;

/**
 * Created by ZNV on 2017/6/2.
 */
public class NightFreqSearchParam {
    private int size = 10; // 返回top N 条数据，默认10
    private float threshold = 0.89f; // 相似度阈值，默认值92%
    private  int frequency = 5; //抓拍频次，默认为5
    private String startTime = ""; // 开始时间
    private String endTime = ""; // 结束时间
 //   private String analysis = "2";//1:不分析,2,分析；默认2

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /*public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }*/

}
