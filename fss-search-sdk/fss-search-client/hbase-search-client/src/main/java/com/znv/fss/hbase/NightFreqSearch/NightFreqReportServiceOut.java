package com.znv.fss.hbase.NightFreqSearch;

import com.znv.fss.hbase.staytime.Analyses;
import com.znv.fss.hbase.staytime.StayTimeOut;

/**
 * Created by ZNV on 2017/6/7.
 */
public class NightFreqReportServiceOut {
    private String id;
    private String type;
    private String errorCode;
    private String time;
    private int count;
    //private NightFreqOut[] nightFreq;
    private NightAnalyses[] analyses;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

  /*  public void setNightFreq(NightFreqOut[] nightFreq) {
        if (nightFreq != null) {
            this.nightFreq = nightFreq.clone();
        }
    }

    public NightFreqOut[] getNightFreq() {
        NightFreqOut[] temp = this.nightFreq;
        return temp;
    }*/

    public void setNightAnalyses(NightAnalyses[] analyses) {
        if (analyses != null) {
            this.analyses = analyses.clone();
        }
    }

    public NightAnalyses[] getNightAnalyses() {
        NightAnalyses[] temp = this.analyses;
        return temp;
    }
}
