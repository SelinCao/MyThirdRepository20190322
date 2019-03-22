package com.znv.fss.es.FssNightOutSearch;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by Administrator on 2017/12/5.
 */
public class NightOutSearchQueryPersonHit {

    private String fusedId;
    int personFrequency;
    @JSONField(name = "enter_time")
    private String enterTime;
    @JSONField(name = "img_url")
    private String imgUrl;
    @JSONField(name = "big_picture_uuid")
    private String bigPictureUuid;

    public void setFusedId(String fusedId) {
        this.fusedId = fusedId;
    }

    public String getFusedId() {
        return fusedId;
    }

    public void setEnterTime(String enterTime) {
        this.enterTime = enterTime;
    }

    public String getEnterTime() {
        return enterTime;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setBigPictureUuid(String bigPictureUuid) {
        this.bigPictureUuid = bigPictureUuid;
    }

    public String getBigPictureUuid() {
        return bigPictureUuid;
    }

    public int getPersonFrequency() {
        return personFrequency;
    }

    public void setPersonFrequency(int personFrequency) {
        this.personFrequency = personFrequency;
    }

}
