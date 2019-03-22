package com.znv.fss.es.AbnormalRationSearch;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * Created by zhuhx on 2018/3/6.
 */
public class AbnormalRationJSONOut {

    private String id;
    private String type;
    @JSONField(name = "errorCode")
    private int errorCode;
    private long time;
    private int count;
    @JSONField(name = "relationshipData")


    
    private List<AbnormalRationSearchDataOut> fssPeerSearchPeerDataOut;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<AbnormalRationSearchDataOut> getFssPeerSearchPeerDataOut() {
        return fssPeerSearchPeerDataOut;
    }

    public void setFssPeerSearchPeerDataOut(List<AbnormalRationSearchDataOut> fssPeerSearchPeerDataOut) {
        this.fssPeerSearchPeerDataOut = fssPeerSearchPeerDataOut;
    }
}
