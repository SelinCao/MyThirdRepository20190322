package com.znv.fss.es.AbnormalRationTrackSearch;

import com.znv.fss.es.AbnormalRationSearch.AimPersonQueryHit;

import java.util.List;

public class AbnormalRationTrackoutputData {
    private List<PeerOutputData> peerList; // 同行人相关信息
    private AimPersonQueryHit targetData; // 目标人信息

    public List<PeerOutputData> getPeerList() {
        return peerList;
    }

    public void setPeerList(List<PeerOutputData> peerList) {
        this.peerList = peerList;
    }

    public AimPersonQueryHit getTargetData() {
        return targetData;
    }

    public void setTargetData(AimPersonQueryHit targetData) {
        this.targetData = targetData;
    }
}
