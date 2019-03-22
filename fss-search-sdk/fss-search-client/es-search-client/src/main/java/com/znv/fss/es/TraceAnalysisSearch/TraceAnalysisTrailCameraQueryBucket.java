package com.znv.fss.es.TraceAnalysisSearch;

import java.util.List;

public class TraceAnalysisTrailCameraQueryBucket {

     private String cameraId;
     private int cameraCount;
     private List<TraceAnalysisSearchQueryHit> personHits;

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
    }

    public String getCameraId() {
        return cameraId;
    }

    public int getCameraCount() {
        return cameraCount;
    }

    public void setCameraCount(int cameraCount) {
        this.cameraCount = cameraCount;
    }

    public List<TraceAnalysisSearchQueryHit> getPersonHits() {
        return personHits;
    }

    public void setPersonHits(List<TraceAnalysisSearchQueryHit> personHits) {
        this.personHits = personHits;
    }


}
