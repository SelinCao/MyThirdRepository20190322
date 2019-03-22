package com.znv.fss.es.TraceAnalysisSearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.znv.fss.es.FormatObject.formatTime;


/**
 * Created by Administrator on 2017/12/5.
 */
public class TraceAnalysisSearch extends BaseEsSearch {
    protected static final Logger LOGGER = LogManager.getLogger(TraceAnalysisSearch.class);
    private String esurl;
    private String templateName;

    public TraceAnalysisSearch(String esurl, String tempalteName) {
        this.esurl = esurl;
        this.templateName = tempalteName;
    }

    public JSONObject initConnectParams() {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;
    }

    public JSONObject initConnectParams(String esurl) {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;
    }

    // 重新封装返回结果
    @Override
    public JSONObject requestSearch(String params) {
        //FeatureCompUtil fc = new FeatureCompUtil();
        TraceAnalysisSearchJsonIn inputParam = JSON.parseObject(params, TraceAnalysisSearchJsonIn.class);
        TraceAnalysisSearchQueryParam queryParams = inputParam.getParams();
        int errCode = paramCheck(queryParams);
        if (errCode != FssErrorCodeEnum.SUCCESS.getCode()) {
            return getErrorResult(errCode);
        }
        //重新封装查询参数
        JSONObject obj = getTemplateParams(queryParams);

        // HTTP连接
        JSONObject httpConResult = initConnectParams();
        if (httpConResult != null) {
            return httpConResult;
        }

        StringBuffer sb = super.getSearchResult(obj);
        // [lq-add]
        if (sb.toString().equals(String.valueOf(FssErrorCodeEnum.SENSETIME_FEATURE_POINTS_ERROR.getCode()))) {
            return getErrorResult(FssErrorCodeEnum.SENSETIME_FEATURE_POINTS_ERROR.getCode());
        }
        if (sb.toString().equals(String.valueOf(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()))) {
            return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        }
        String esResults = sb.toString();
        JSONObject jsonEsResult = JSONObject.parseObject(esResults);
        if (jsonEsResult.get("error") != null) {
            LOGGER.info("Query es error!! params:" + obj.toJSONString() + ".\terror:" + jsonEsResult.toJSONString());
            return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        }
        if (jsonEsResult.getBoolean("timed_out")) {
            return getErrorResult(FssErrorCodeEnum.ES_TIMEOUT_EXCEPTION.getCode());
        }
        //System.out.println("读取es查询结果如下：");
        //System.out.println(JSON.toJSONString(jsonEsResult));
        // 从es查询结果中获取hits
        TrackAnalysisSearchJsonOut outputResult = new TrackAnalysisSearchJsonOut();
        if(queryParams.getPersonAggregation()==true){
            JSONArray aggBuckets = jsonEsResult.getJSONObject("aggregations").getJSONObject("group_by_person").getJSONArray("buckets");
            TraceAnalysisSearchPersonQueryAgg outAgg = new TraceAnalysisSearchPersonQueryAgg();
            int top = queryParams.getTop();
            List<TraceAnalysisSearchQueryBucket> lFBucket = new ArrayList<>(top);
            List<TraceAnalysisSearchQueryBucket> mFBucket = new ArrayList<>(top);
            List<TraceAnalysisSearchQueryBucket> hFBucket = new ArrayList<>(top);
            int lSize = 0, mSize = 0, hSize = 0;
            for (int i = 0; i < aggBuckets.size(); i++) {
                JSONObject agg = aggBuckets.getJSONObject(i);
                TraceAnalysisSearchQueryBucket personBuket = new TraceAnalysisSearchQueryBucket();
                int frequency = agg.getInteger("doc_count");
                if ((frequency >= queryParams.getHFThreshold() && hSize < top) || ((queryParams.getLFThreshold() <= frequency) && (frequency < queryParams.getHFThreshold()) && mSize < top)) {
                    personBuket.setPersonFrequency(frequency);
                    personBuket.setPersonId(agg.getString("key"));
                    JSONObject personData = (JSONObject) agg.getJSONObject("top_person_hits").getJSONObject("hits").getJSONArray("hits").get(0);
                    JSONObject source = personData.getJSONObject("_source");
                    personBuket.setImgUrl(source.getString("img_url"));
                    personBuket.setEnterTime(formatTime(source.getString("enter_time")));
                    personBuket.setBigPictureUuid(source.getString("big_picture_uuid"));
                    if (frequency >= queryParams.getHFThreshold()) {
                        if (hSize < top) {
                            hSize++;
                            hFBucket.add(personBuket);
                        }
                    } else if (frequency >= queryParams.getLFThreshold()) {
                        if (mSize < top) {
                            mSize++;
                            mFBucket.add(personBuket);
                        }
                    }
                } else {
                    break;
                }
            }
            for (int j =  aggBuckets.size()-1; j>= 0; j--) {
                JSONObject agg2 = aggBuckets.getJSONObject(j);
                TraceAnalysisSearchQueryBucket personBuket2 = new TraceAnalysisSearchQueryBucket();
                int frequency = agg2.getInteger("doc_count");
                if ((frequency < queryParams.getLFThreshold()) && lSize < top) {
                    lSize++;
                    personBuket2.setPersonFrequency(frequency);
                    personBuket2.setPersonId(agg2.getString("key"));
                    JSONObject personData = (JSONObject) agg2.getJSONObject("top_person_hits").getJSONObject("hits").getJSONArray("hits").get(0);
                    JSONObject source = personData.getJSONObject("_source");
                    personBuket2.setImgUrl(source.getString("img_url"));
                    personBuket2.setEnterTime(formatTime(source.getString("enter_time")));
                    personBuket2.setBigPictureUuid(source.getString("big_picture_uuid"));
                    lFBucket.add(personBuket2);
                }
            }
            if (queryParams.getCountOrder().equals("asc")) {
                Collections.sort(hFBucket, new Comparator<TraceAnalysisSearchQueryBucket>() {
                    @Override
                    public int compare(TraceAnalysisSearchQueryBucket o1, TraceAnalysisSearchQueryBucket o2) {
                        return Integer.compare(o1.getPersonFrequency(), o2.getPersonFrequency());
                    }
                });
                Collections.sort(mFBucket, new Comparator<TraceAnalysisSearchQueryBucket>() {
                    @Override
                    public int compare(TraceAnalysisSearchQueryBucket o1, TraceAnalysisSearchQueryBucket o2) {
                        return Integer.compare(o1.getPersonFrequency(), o2.getPersonFrequency());
                    }
                });
                Collections.sort(lFBucket, new Comparator<TraceAnalysisSearchQueryBucket>() {
                    @Override
                    public int compare(TraceAnalysisSearchQueryBucket o1, TraceAnalysisSearchQueryBucket o2) {
                        return Integer.compare(o1.getPersonFrequency(), o2.getPersonFrequency());
                    }
                });
            }
            if (queryParams.getCountOrder().equals("desc")) {
                Collections.sort(lFBucket, new Comparator<TraceAnalysisSearchQueryBucket>() {
                    @Override
                    public int compare(TraceAnalysisSearchQueryBucket o1, TraceAnalysisSearchQueryBucket o2) {
                        return Integer.compare(o2.getPersonFrequency(), o1.getPersonFrequency());
                    }
                });
            }
            outAgg.setHfBucket(hFBucket);
            outAgg.setMfBucket(mFBucket);
            outAgg.setLfBucket(lFBucket);
            outputResult.setPersonQueryAgg(outAgg);
        }else if(queryParams.getCameraAggregation()==true){
            JSONArray cameraBuckets = jsonEsResult.getJSONObject("aggregations").getJSONObject("group_by_camera").getJSONArray("buckets");
            TraceAnalysisSearchCameraQueryAgg cameraAgg = new TraceAnalysisSearchCameraQueryAgg();
            int top = queryParams.getTop();
            List<TraceAnalysisCameraQueryBucket> cameraBucket = new ArrayList<>(top);
            for(int i = 0;i < cameraBuckets.size();i++ ){
                if(i<top){
                    JSONObject agg = cameraBuckets.getJSONObject(i);
                    TraceAnalysisCameraQueryBucket camBucket = new TraceAnalysisCameraQueryBucket();
                    camBucket.setCameraFrequency(agg.getInteger("doc_count"));
                    camBucket.setCameraId(agg.getString("key"));
                    JSONObject cameraData = (JSONObject) agg.getJSONObject("top_camera_hits").getJSONObject("hits").getJSONArray("hits").get(0);
                    camBucket.setCameraName(cameraData.getJSONObject("_source").getString("camera_name"));
                    cameraBucket.add(camBucket);
                }else{
                    break;
                }
            }
            if(queryParams.getCountOrder().equals("asc")){
                Collections.sort(cameraBucket, new Comparator<TraceAnalysisCameraQueryBucket>() {
                    @Override
                    public int compare(TraceAnalysisCameraQueryBucket o1, TraceAnalysisCameraQueryBucket o2) {
                        return Integer.compare(o1.getCameraFrequency(), o2.getCameraFrequency());
                    }
                });
            }
            cameraAgg.setCameraBucket(cameraBucket);
            outputResult.setCameraQueryAgg(cameraAgg);

        }else if(queryParams.getTimeAggregation()==true){
            JSONArray buckets = jsonEsResult.getJSONObject("aggregations").getJSONObject("group_by_time").getJSONArray("buckets");
            TraceAnalysisSearchTimeQueryAgg timeAgg = new TraceAnalysisSearchTimeQueryAgg();
            List<TraceAnalysisTimeQueryBucket> timeBuckets = new ArrayList<>();
            Map<String,Integer> timeMap = new HashMap<>();
            for(int i = 0;i < buckets.size();i++){
                JSONObject agg = buckets.getJSONObject(i);
                String time = agg.getString("key_as_string").split(" ")[1];
                int count = agg.getInteger("doc_count");
                if(timeMap!=null){
                    if( timeMap.containsKey(time)){
                        int countSum = timeMap.get(time)+count;
                        timeMap.put(time,countSum);
                    }else{
                        timeMap.put(time,count);
                    }
                }

            }
            Iterator<Map.Entry<String,Integer>> timeItr = timeMap.entrySet()
                    .iterator();
            while(timeItr.hasNext()){
                Map.Entry<String,Integer> entry = timeItr.next();
                TraceAnalysisTimeQueryBucket timeBuket = new TraceAnalysisTimeQueryBucket();
                timeBuket.setTime(entry.getKey());
                timeBuket.setTimeFrequency(entry.getValue());
                timeBuckets.add(timeBuket);
            }
            if(queryParams.getCountOrder()!=null && queryParams.getCountOrder().equals("asc")){
                Collections.sort(timeBuckets, new Comparator<TraceAnalysisTimeQueryBucket>() {
                    @Override
                    public int compare(TraceAnalysisTimeQueryBucket o1, TraceAnalysisTimeQueryBucket o2) {
                        return o1.getTime().compareTo( o2.getTime());
                    }
                });
            }
            if(queryParams.getCountOrder()!=null && queryParams.getCountOrder().equals("desc")){
                Collections.sort(timeBuckets, new Comparator<TraceAnalysisTimeQueryBucket>() {
                    @Override
                    public int compare(TraceAnalysisTimeQueryBucket o1, TraceAnalysisTimeQueryBucket o2) {
                        return o2.getTime().compareTo( o1.getTime());
                    }
                });
            }
            timeAgg.setCameraBucket(timeBuckets);
            outputResult.setTimeQueryAgg(timeAgg);

        } else if(queryParams.getTrailAggregation()==true){
            JSONArray buckets = jsonEsResult.getJSONObject("aggregations").getJSONObject("group_by_trail").getJSONArray("buckets");
            TraceAnalysisSearchTrailQueryAgg trailAgg = new TraceAnalysisSearchTrailQueryAgg();
            List<TraceAnalysisSearchTrailTimeQueryBucket> timeBuckets = new ArrayList<>();
           for(int i=0;i<buckets.size();i++){
               JSONObject agg1 = buckets.getJSONObject(i);
               TraceAnalysisSearchTrailTimeQueryBucket timeBucket =new TraceAnalysisSearchTrailTimeQueryBucket();
               timeBucket.setEnterTime(agg1.getString("key_as_string"));
               timeBucket.setTimeCount(agg1.getInteger("doc_count"));
               JSONArray camBuckets = agg1.getJSONObject("group_by_camera").getJSONArray("buckets");
               List<TraceAnalysisTrailCameraQueryBucket> cameraBuckets = new ArrayList<>();
               for(int j=0;j<camBuckets.size();j++){
                   JSONObject agg2 = camBuckets.getJSONObject(j);
                   TraceAnalysisTrailCameraQueryBucket cameraBucket = new TraceAnalysisTrailCameraQueryBucket();
                   cameraBucket.setCameraId(agg2.getString("key"));
                   cameraBucket.setCameraCount(agg2.getInteger("doc_count"));
                   JSONArray hits= agg2.getJSONObject("camera_person_hits").getJSONObject("hits").getJSONArray("hits");
                   List<TraceAnalysisSearchQueryHit> outHits = new ArrayList<>(hits.size());
                   for (int n = 0; n < hits.size(); n++) {
                       TraceAnalysisSearchQueryHit outHit = new TraceAnalysisSearchQueryHit();
                       JSONObject hit = hits.getJSONObject(n);
                       JSONObject source = hit.getJSONObject("_source"); // 从hits数组中获取_source
                       outHit.setBigPictureUuid(source.getString("big_picture_uuid"));
                       outHit.setImgUrl(source.getString("img_url"));
                       outHit.setCameraId(source.getString("camera_id"));
                       outHit.setCameraName(source.getString("camera_name"));
                       outHit.setOfficeId(source.getString("office_id"));
                       outHit.setOfficeName(source.getString("office_name"));
                       outHit.setEnterTime(formatTime(source.getString("enter_time")));
                       outHit.setLeaveTime(formatTime(source.getString("leave_time")));
                       outHit.setOpTime(formatTime(source.getString("op_time")));
                       outHit.setLibId(source.getInteger("lib_id"));
                       outHit.setPersonId(source.getString("person_id"));
                       outHit.setSimilarity(source.getFloatValue("similarity"));
                       if (source.containsKey("img_width")){
                           outHit.setImgWidth(source.getIntValue("img_width"));
                       }
                       if (source.containsKey("img_height")){
                           outHit.setImgWidth(source.getIntValue("img_height"));
                       }
                       if (source.containsKey("left_pos")){
                           outHit.setLeftPos(source.getIntValue("left_pos"));
                       }
                       if (source.containsKey("top")){
                           outHit.setTop(source.getIntValue("top"));
                       }
                       outHits.add(outHit);
                   }
                   cameraBucket.setPersonHits(outHits);
                   cameraBuckets.add(cameraBucket);
               }
               timeBucket.setTimeBucket(cameraBuckets);
               timeBuckets.add(timeBucket);
           }
            trailAgg.setTrailBucket(timeBuckets);
            outputResult.setTrailQueryAgg(trailAgg);
        }else{
            JSONArray esHits = jsonEsResult.getJSONObject("hits").getJSONArray("hits");
            List<TraceAnalysisSearchQueryHit> outHits = new ArrayList<>(esHits.size());
            for (int i = 0; i < esHits.size(); i++) {
                TraceAnalysisSearchQueryHit outHit = new TraceAnalysisSearchQueryHit();
                JSONObject hit = esHits.getJSONObject(i);
                JSONObject source = hit.getJSONObject("_source"); // 从hits数组中获取_source
                outHit.setBigPictureUuid(source.getString("big_picture_uuid"));
                outHit.setImgUrl(source.getString("img_url"));
                outHit.setCameraId(source.getString("camera_id"));
                outHit.setCameraName(source.getString("camera_name"));
                outHit.setOfficeId(source.getString("office_id"));
                outHit.setOfficeName(source.getString("office_name"));
                outHit.setEnterTime(formatTime(source.getString("enter_time")));
                outHit.setLeaveTime(formatTime(source.getString("leave_time")));
                outHit.setOpTime(formatTime(source.getString("op_time")));
                outHit.setLibId(source.getInteger("lib_id"));
                outHit.setPersonId(source.getString("person_id"));
                outHit.setSimilarity(source.getFloatValue("similarity"));
                if (source.containsKey("img_width")){
                    outHit.setImgWidth(source.getIntValue("img_width"));
                }
                if (source.containsKey("img_height")){
                    outHit.setImgWidth(source.getIntValue("img_height"));
                }
                if (source.containsKey("left_pos")){
                    outHit.setLeftPos(source.getIntValue("left_pos"));
                }
                if (source.containsKey("top")){
                    outHit.setTop(source.getIntValue("top"));
                }
                outHits.add(outHit);
            }
            outputResult.setHits(outHits);
        }

        int total = jsonEsResult.getJSONObject("hits").getInteger("total");
        int took = jsonEsResult.getInteger("took");
        outputResult.setErrorcode(FssErrorCodeEnum.SUCCESS.getCode());
        outputResult.setTotal(total);
        outputResult.setTook(took);
        return (JSONObject) JSONObject.toJSON(outputResult);

    }

    private int paramCheck(TraceAnalysisSearchQueryParam inParam) {
        if (StringUtils.isEmpty(inParam.getEnterTimeStart())) {
            LOGGER.info("StartTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (StringUtils.isEmpty(inParam.getEnterTimeEnd())) {
            LOGGER.info("EndTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (inParam.getEnterTimeStart().compareTo(inParam.getEnterTimeEnd()) > 0) {
            LOGGER.info("EndTime can't larger than startTime ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (inParam.getFrom() < 0) {
            LOGGER.info("From is too small ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (inParam.getSize() < 0) {
            LOGGER.info("Size is too small ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if ((inParam.getFrom() + inParam.getSize()) >= 10000) {
            LOGGER.info("Request result set out of range ！");
            return FssErrorCodeEnum.ES_SIZE_OUT_OF_RANGE.getCode();
        }
        return FssErrorCodeEnum.SUCCESS.getCode();
    }

//    private JSONObject getErrorResult(int errCode) {
//        JSONObject result = new JSONObject();
//        result.put("errorCode", errCode);
//        result.put("total", 0);
//        return result;
//    }

    private JSONObject getTemplateParams(TraceAnalysisSearchQueryParam inParam) {
        JSONObject paramsT = new JSONObject();
        paramsT.put("enter_time_start", inParam.getEnterTimeStart());
        paramsT.put("enter_time_end", inParam.getEnterTimeEnd());
        paramsT.put("similarity", inParam.getSimilarity());
        paramsT.put("from", inParam.getFrom());
        paramsT.put("size", inParam.getSize());
        if( inParam.getTimeAggregation()==true){
            paramsT.put("time_aggregation",inParam.getTimeAggregation());
            paramsT.put("count_order","desc");
            paramsT.put("search_interval","1h");
        }
        if (inParam.getPersonAggregation() == true) {
            String includes[] = {"person_id", "img_url", "enter_time", "big_picture_uuid"};
            paramsT.put("person_aggregation", inParam.getPersonAggregation());
            paramsT.put("includes", includes);
            paramsT.put("count_order", "desc");
        }
        if( inParam.getCameraAggregation()==true){
            paramsT.put("camera_aggregation",inParam.getCameraAggregation());
            paramsT.put("count_order","desc");
            if(inParam.getTop()!= 0 ){
                paramsT.put("camera_top_num",inParam.getTop());
            }
        }
        if(inParam.getTrailAggregation()==true){
            paramsT.put("trail_aggregation",inParam.getTrailAggregation());
            String includes[] = {"person_id","img_url","leave_time","img_width","img_height","camera_id","left_pos","office_id","office_name","top","lib_id","img_url","similarity","camera_name","enter_time","score","op_time","big_picture_uuid"};
            paramsT.put("includes",includes);
            paramsT.put("person_size",inParam.getPerosnSize());
            paramsT.put("search_interval","1m");
        }
        if(inParam.getPersonId()!=null && !inParam.getPersonId().isEmpty()){
            paramsT.put("person_id",inParam.getPersonId());
            paramsT.put("is_person",true);
        }
        if (inParam.getOfficeId() != null && !inParam.getOfficeId().isEmpty()) {
            paramsT.put("office_id", inParam.getOfficeId());
            paramsT.put("is_office",true);
        }
        if (inParam.getCameraId() != null && !inParam.getCameraId().isEmpty()) {
            paramsT.put("camera_id", inParam.getCameraId());
            paramsT.put("is_camera",true);
        }
        if (inParam.getLibId() != null && !inParam.getLibId().isEmpty()) {
            paramsT.put("lib_id", inParam.getLibId());
            paramsT.put("is_lib",true);
        }
        if( inParam.getSortField() != null && !inParam.getSortField().isEmpty()){
            paramsT.put("sortField", inParam.getSortField());
            paramsT.put("sortOrder", inParam.getSortOrder());
            paramsT.put("is_sort",true);
        }
        JSONObject obj = new JSONObject();
        obj.put("id", templateName);
        obj.put("params", paramsT);

        return obj;
    }

}
