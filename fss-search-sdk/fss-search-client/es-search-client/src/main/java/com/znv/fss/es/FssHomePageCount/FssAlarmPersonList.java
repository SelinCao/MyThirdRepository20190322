package com.znv.fss.es.FssHomePageCount;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.EsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.znv.fss.es.FormatObject.formatTime;

public class FssAlarmPersonList extends BaseEsSearch {

    protected static final Logger LOGGER = LogManager.getLogger(FssAlarmPersonList.class);
    private String esurl;

    public FssAlarmPersonList(String esurl) {
        this.esurl = esurl;
        //  this.templateName = tempalteName;
    }

    public JSONObject initConnectParams() {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;
    }

    public JSONObject initConnectParams(String esurl) {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;
    }

    public JSONObject requestSearch(String params) {
        JSONObject outputResult = new JSONObject();
        JSONObject jsonEsResult1 = subRequestSearch(params);
        int took = jsonEsResult1.getIntValue("took");
        String total = jsonEsResult1.getJSONObject("hits").getString("total");
        JSONArray esHits = jsonEsResult1.getJSONObject("hits").getJSONArray("hits");
        JSONArray hits = new JSONArray();

        if (null != esHits && esHits.size() != 0) {
             String personIdList = "[";
            for (int i = 0; i < esHits.size(); i++) {
                JSONObject source = esHits.getJSONObject(i).getJSONObject("_source"); // 从hits数组中获取_source
                String enterTime = formatTime(source.getString("enter_time"));
                source.put("enter_time",enterTime);
                String personId = source.getString("person_id");
                if(i == (esHits.size()-1))
                    personIdList += personId + "]";
                else
                    personIdList += personId+",";
                hits.add(i, source);
               // personMap.put(personId,source); //告警去重
            }

            Map<String,JSONObject> personMap = new HashMap();
            String stringParamSecond = "{'_source':{'excludes':['feature','control_end_time','create_time','nation','control_start_time','community_name','sex','modify_time','birth','community_id','door_open']},'query':{'bool':{'filter':{'bool':{'should':{'terms':{'person_id':"+personIdList+"}}}}}},'from': 0,'size':"+esHits.size()+"}";
            String esUrlSecond = EsManager.createPersonListURL();
            initConnectParams(esUrlSecond);
            StringBuffer sbSecond = super.getSearchResult(JSON.parseObject(stringParamSecond));

            if (sbSecond.toString().equals(new StringBuffer(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()).toString())){
                return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
            }
            String esResults = sbSecond.toString();
            JSONObject jsonEsResultSecond = JSONObject.parseObject(esResults);
             took += jsonEsResultSecond.getIntValue("took");
            JSONArray esSecondHits = jsonEsResultSecond.getJSONObject("hits").getJSONArray("hits");
            if (null != esSecondHits && esSecondHits.size() != 0) {
                for (int i = 0; i < esSecondHits.size(); i++) {
                    JSONObject source = esSecondHits.getJSONObject(i).getJSONObject("_source");
                    String personId = source.getString("person_id");
                    personMap.put(personId,source);
                }
            }
            for (int i = 0; i < hits.size(); i++) {
                JSONObject personKey = hits.getJSONObject(i);
                String personIdKey = personKey.getString("person_id");
                if(personMap.containsKey(personIdKey)){
                  JSONObject personValue =  personMap.get(personIdKey);
                    personKey.put("card_id",personValue.getString("card_id"));
                    personKey.put("control_event_id",personValue.getString("control_event_id"));
                    personKey.put("control_community_id",personValue.getString("control_community_id"));
                }
            }
        }
        outputResult.put("took", took);
        outputResult.put("total", total);
        outputResult.put("hits",hits);
        outputResult.put("errorCode", FssErrorCodeEnum.SUCCESS.getCode());
        return outputResult;
    }


    public JSONObject subRequestSearch(String params){

        String inParam = JSON.parseObject(params).getString("params");
        JSONObject jsonEsResult = initConnectParams();
        int from =0;
        int size=10;
        String stringParam;
        JSONObject query = new JSONObject();
        if (jsonEsResult == null ) {
            JSONObject jsonParam = JSON.parseObject(inParam);
            if(jsonParam.containsKey("from") && jsonParam.containsKey("size")){
                from = jsonParam.getInteger("from");
                size = jsonParam.getInteger("size");
            }
            stringParam = String.format("{'_source':{'excludes':['rt_feature','op_time','rowkey','leave_time','camera_type','duration_time','track_idx','frame_index','right_pos','task_idx','bottom','birth']},'query':{'bool':{'filter':{}}},'from': %d,'size': %d,'sort': [{'enter_time':{'order':'desc'}},{'person_id':{'order':'desc'}}]}",from,size);
            StringBuffer sb = super.getSearchResult(JSON.parseObject(stringParam));
            if (sb.toString().equals(new StringBuffer(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()).toString()))           {
                return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
            } else {
                String esResults = sb.toString();
                jsonEsResult = JSONObject.parseObject(esResults);
            }
        }
        return jsonEsResult;
    }





}
