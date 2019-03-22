package com.znv.fss.es.TraceAnalysisSearch;

import com.alibaba.fastjson.annotation.JSONField;

public class TraceAnalysisCameraQueryBucket {

    @JSONField(name = "camera_id")
     private String cameraId;
    @JSONField(name = "camera_name")
    private String cameraName;
    private int cameraFrequency;

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }

    public String getCameraName() {
        return cameraName;
    }


    public int getCameraFrequency() {
        return cameraFrequency;
    }

    public void setCameraFrequency(int cameraFrequency) {
        this.cameraFrequency = cameraFrequency;
    }
}
