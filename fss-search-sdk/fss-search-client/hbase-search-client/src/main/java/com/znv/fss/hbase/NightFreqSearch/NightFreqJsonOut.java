package com.znv.fss.hbase.NightFreqSearch;

import com.znv.fss.hbase.staytime.StayTimeReportServiceOut;

/**
 * Created by ZNV on 2017/6/7.
 */
public class NightFreqJsonOut {
    private NightFreqReportServiceOut reportService;

    public void setReportService(NightFreqReportServiceOut reportService) {
        this.reportService = reportService;
    }

    public NightFreqReportServiceOut getReportService() {
        return reportService;
    }
}
