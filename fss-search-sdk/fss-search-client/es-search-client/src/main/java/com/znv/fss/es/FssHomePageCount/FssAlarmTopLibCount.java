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

public class FssAlarmTopLibCount extends BaseEsSearch {

    protected static final Logger LOGGER = LogManager.getLogger(FssAlarmTopLibCount.class);
    private String esurl;

    public FssAlarmTopLibCount(String esurl) {
        this.esurl = esurl;
        //  this.templateName = tempalteName;
    }

    public JSONObject initConnectParams() {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;
    }

    public JSONObject requestSearch(String params) {
        JSONObject outputResult = new JSONObject();
        JSONObject jsonEsResult = subRequestSearch(params);
        int took = jsonEsResult.getIntValue("took");
        String total = jsonEsResult.getJSONObject("hits").getString("total");
        JSONArray esAgg = jsonEsResult.getJSONObject("aggregations").getJSONObject("lib_ids").getJSONArray("buckets");
        outputResult.put("took", took);
        outputResult.put("total", total);
        outputResult.put("buckets",esAgg);
        outputResult.put("errorCode", FssErrorCodeEnum.SUCCESS.getCode());
        return outputResult;
    }


    public JSONObject subRequestSearch(String params){

        JSONObject jsonParam = JSON.parseObject(params).getJSONObject("params");
        JSONObject jsonEsResult = initConnectParams();
        String  stringParam ="{'aggs':{'lib_ids':{'terms':{'field':'lib_id','size':10}}},'from':0, 'size':0}";
        JSONObject stringParamJson = JSON.parseObject(stringParam);
        if (jsonEsResult == null ) {
            if(jsonParam.containsKey("top")){
                stringParamJson.getJSONObject("aggs").getJSONObject("lib_ids").getJSONObject("terms").put("size",jsonParam.getInteger("top"));
            }

            if(jsonParam.containsKey("lib_id") && jsonParam.getJSONArray("lib_id") != null && !jsonParam.getJSONArray("lib_id").isEmpty()){
                JSONObject stringParamTerms = JSON.parseObject("{'bool':{'filter':{'bool':{'should':{'terms':{'lib_id':[]}}}}}}");
               stringParamTerms.getJSONObject("bool").getJSONObject("filter").getJSONObject("bool").getJSONObject("should").getJSONObject("terms").put("lib_id",jsonParam.getJSONArray("lib_id"));
                stringParamJson.put("query",stringParamTerms);
            }

            StringBuffer sb = super.getSearchResult(stringParamJson);
            if (sb.toString().equals(new StringBuffer(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()).toString()))           {
                return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
            } else {
                jsonEsResult = JSONObject.parseObject(sb.toString());
            }
        }
        return jsonEsResult;
    }






}
