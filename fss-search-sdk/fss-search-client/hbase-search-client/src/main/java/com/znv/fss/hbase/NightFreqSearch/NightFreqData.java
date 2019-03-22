package com.znv.fss.hbase.NightFreqSearch;

/**
 * Created by ZNV on 2017/6/16.
 */
public class NightFreqData {
    private String camera_id;
    private String camera_name;
    private String duration_time;
    private float gpsx;
    private float gpsy;
    private String office_id;
    private String office_name;
    private String person_id;
    private String enter_time;
    private String img_url;
    private String uuid;
    private String big_picture_uuid;
    private int frequency;
    private byte[] rowKey;

    public void setCamera_id(String camera_id) {
        this.camera_id = camera_id;
    }

    public String getCamera_id() {
        return this.camera_id;
    }

    public void setCamera_name(String camera_name) {
        this.camera_name = camera_name;
    }

    public String getCamera_name() {
        return this.camera_name;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setDuration_time(String duration_time) {
        this.duration_time = duration_time;
    }

    public String getDuration_time() {
        return this.duration_time;
    }

    public void setGpsx(float gpsx) {
        this.gpsx = gpsx;
    }

    public float getGpsx() {
        return this.gpsx;
    }

    public void setGpsy(float gpsy) {
        this.gpsy = gpsy;
    }

    public float getGpsy() {
        return this.gpsy;
    }

    public void setOffice_id(String office_id) {
        this.office_id = office_id;
    }

    public String getOffice_id() {
        return this.office_id;
    }

    public void setOffice_name(String office_name) {
        this.office_name = office_name;
    }

    public String getOffice_name() {
        return this.office_name;
    }

    public void setBig_picture_uuid(String big_picture_uuid) {
        this.big_picture_uuid = big_picture_uuid;
    }

    public String getBig_picture_uuid() {
        return this.big_picture_uuid;
    }

    public void setPerson_id(String person_id) {
        this.person_id = person_id;
    }

    public String getPerson_id() {
        return this.person_id;
    }

    public void setEnter_time(String enter_time) {
        this.enter_time = enter_time;
    }

    public String getEnter_time() {
        return this.enter_time;
    }

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setRowKey(byte[] rowKey) {
        if (rowKey != null) {
            this.rowKey = rowKey.clone();
        }
    }

    public byte[] getRowKey() {
        byte[] tmp = this.rowKey;
        return tmp;
    }

}
