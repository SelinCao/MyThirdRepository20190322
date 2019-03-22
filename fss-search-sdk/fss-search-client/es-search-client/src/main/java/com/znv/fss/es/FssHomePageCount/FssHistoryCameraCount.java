package com.znv.fss.es.FssHomePageCount;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.FssArbitrarySearch.FsArSeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FssHistoryCameraCount extends BaseEsSearch {

    protected static final Logger LOGGER = LogManager.getLogger(FssHistoryCameraCount.class);
    private String esurl;
    private String templateName;
    public FssHistoryCameraCount(String esurl,String templateName) {
        this.esurl = esurl;
        this.templateName = templateName;
    }

    public JSONObject initConnectParams() {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;

    }

    public JSONObject requestSearch(String params) {
        JSONObject outputResult = new JSONObject();
        JSONObject jsonEsResult1 = subRequestSearch(params);
        int took = jsonEsResult1.getIntValue("took");
        String total = jsonEsResult1.getJSONObject("hits").getString("total");
        JSONArray agg = jsonEsResult1.getJSONObject("aggregations").getJSONObject("group_by_camera").getJSONArray("buckets");
        JSONArray hits = new JSONArray();
        if (null != agg && agg.size() != 0) {
            for (int i = 0; i < agg.size(); i++) {
                JSONObject topHit = (JSONObject) agg.getJSONObject(i).getJSONObject("camera_hits").getJSONObject("hits").getJSONArray("hits").get(0);
                JSONObject source = topHit.getJSONObject("_source");
                source.put("camera_id", agg.getJSONObject(i).getString("key"));
                source.put("doc_count",agg.getJSONObject(i).getString("doc_count"));
                hits.add(i,source);
            }
        }

        outputResult.put("took", took);
        outputResult.put("errorCode", FssErrorCodeEnum.SUCCESS.getCode());
        outputResult.put("hits", hits);
        outputResult.put("total", total);
        return outputResult;
    }

    public JSONObject subRequestSearch(String params){

        String inParam = JSON.parseObject(params).getString("params");
        JSONObject jsonEsResult = initConnectParams();
        if (jsonEsResult == null ) {
            String idParam = "{'id':'" + templateName + "'}";
            JSONObject obj = JSON.parseObject(idParam);
            try{
                JSONObject obj2 = JSON.parseObject(inParam);
                if (obj2.getJSONArray("camera_id") != null && !obj2.getJSONArray("camera_id").isEmpty()) {
                    obj2.put("is_camera",true);
                }
                obj2.put("from",0);
                obj2.put("size",0);
                obj2.put("camera_aggregation",true);
                obj.put("params", obj2);
            }catch (Exception e) {
                jsonEsResult.put("errorCode", FssErrorCodeEnum.ES_INVALID_PARAM.getCode());
                LOGGER.error("invalid parameters" + e);
                return jsonEsResult;
            }

            StringBuffer sb = super.getSearchResult(obj);
            // [lq-add]
            if (sb.toString().equals(new StringBuffer(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()).toString())) {
                return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
            } else {
               // String esResults = sb.toString();
                jsonEsResult = JSONObject.parseObject(sb.toString());

            }
        }
        return jsonEsResult;
    }

}
