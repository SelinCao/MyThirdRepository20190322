package com.znv.fss.es.AbnormalRationSearch;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 给前端返回的同行信息封装
 * Created by zhuhx on 2018/3/6.
 */
public class AbnormalRationSearchDataOut {

    //todo 去掉图片 添加uuid
    @JSONField(name = "imgUrl")
    private String imgUrl;
    @JSONField(name = "imageData")
    private String imagedata;
    @JSONField(name = "peerCount")
    private long peercount; // 同行次数
    @JSONField(name = "peerTime")
    private long peertime; // 同行时长，单位为秒
    @JSONField(name = "peerLibId")
    private int peerlibid;
    @JSONField(name = "peerPersonId")
    private String peerpersonid;

    public String getImagedata() {
        return imagedata;
    }

    public void setImagedata(String imagedata) {
        this.imagedata = imagedata;
    }

    public long getPeercount() {
        return peercount;
    }

    public void setPeercount(long peercount) {
        this.peercount = peercount;
    }

    public long getPeertime() {
        return peertime;
    }

    public void setPeertime(long peertime) {
        this.peertime = peertime;
    }

    public int getPeerlibid() {
        return peerlibid;
    }

    public void setPeerlibid(int peerlibid) {
        this.peerlibid = peerlibid;
    }

    public String getPeerpersonid() {
        return peerpersonid;
    }

    public void setPeerpersonid(String peerpersonid) {
        this.peerpersonid = peerpersonid;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}
