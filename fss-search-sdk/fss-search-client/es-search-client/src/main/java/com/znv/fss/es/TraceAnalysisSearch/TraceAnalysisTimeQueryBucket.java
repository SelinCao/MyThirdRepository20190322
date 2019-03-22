package com.znv.fss.es.TraceAnalysisSearch;

import com.alibaba.fastjson.annotation.JSONField;

public class TraceAnalysisTimeQueryBucket {

     private String time;
     private int timeFrequency;

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public int getTimeFrequency() {
        return timeFrequency;
    }

    public void setTimeFrequency(int timeFrequency) {
        this.timeFrequency = timeFrequency;
    }
}
