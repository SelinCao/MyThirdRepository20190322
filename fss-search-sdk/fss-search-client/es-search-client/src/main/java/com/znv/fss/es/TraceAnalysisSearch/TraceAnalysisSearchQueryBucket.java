package com.znv.fss.es.TraceAnalysisSearch;


import com.alibaba.fastjson.annotation.JSONField;

/**
 * Created by Administrator on 2017/12/5.
 */
public class TraceAnalysisSearchQueryBucket {

    private int personFrequency;
    @JSONField(name = "person_id")
    private String personId;
 /*   @JSONField(name = "lib_id")
    private int libId;*/
    @JSONField(name = "enter_time")
    private String enterTime;
    @JSONField(name = "leave_time")
    private String leaveTime;
    @JSONField(name = "img_url")
    private String imgUrl;
    @JSONField(name = "big_picture_uuid")
    private String bigPictureUuid;

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getPersonId() {
        return personId;
    }

  /*  public void setLibId(int libId) {
        this.libId = libId;
    }

    public int getLibId() {
        return libId;
    }*/

    public void setEnterTime(String enterTime) {
        this.enterTime = enterTime;
    }

    public String getEnterTime() {
        return enterTime;
    }

    public void setLeaveTime(String leaveTime) {
        this.leaveTime = leaveTime;
    }

    public String getLeaveTime() {
        return leaveTime;
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

    public void setPersonFrequency(int timeFrequency) {
        this.personFrequency = timeFrequency;
    }

}
