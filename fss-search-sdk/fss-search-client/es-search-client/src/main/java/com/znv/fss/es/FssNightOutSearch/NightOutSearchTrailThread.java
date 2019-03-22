package com.znv.fss.es.FssNightOutSearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisSearchQueryHit;
import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisSearchTrailTimeQueryBucket;
import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisTrailCameraQueryBucket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.znv.fss.es.FormatObject.formatTime;

public class NightOutSearchTrailThread extends Thread {
    private final Log LOGGER = LogFactory.getLog(NightOutSearchThread.class);
    private CountDownLatch threadsSignal;
    private String esurl;
    private String queryParam;
    private Map<String, TraceAnalysisSearchTrailTimeQueryBucket> personTrailMap;

    public NightOutSearchTrailThread(CountDownLatch threadsSignal, String esurl, String queryParam,
                                     Map<String, TraceAnalysisSearchTrailTimeQueryBucket> personTrailMap) {
        this.threadsSignal = threadsSignal;
        this.esurl = esurl;
        this.queryParam = queryParam;
        this.personTrailMap = personTrailMap;
    }

    // 重新封装返回结果
    public void nightPersonSearch(String param) {
        JSONObject queryParam = JSONObject.parseObject(param);
        JSONObject jsonEsResult = getRequestSearch(queryParam);

        if (jsonEsResult.getInteger("errorCode") == 100000) {
            JSONArray buckets = jsonEsResult.getJSONObject("aggregations").getJSONObject("group_by_trail").getJSONArray("buckets");
            for (int i = 0; i < buckets.size(); i++) {
                JSONObject agg1 = buckets.getJSONObject(i);
                TraceAnalysisSearchTrailTimeQueryBucket timeBucket = new TraceAnalysisSearchTrailTimeQueryBucket();
                String enterTime = agg1.getString("key_as_string");
                timeBucket.setEnterTime(enterTime);
                timeBucket.setTimeCount(agg1.getInteger("doc_count"));
                JSONArray camBuckets = agg1.getJSONObject("group_by_camera").getJSONArray("buckets");
                List<TraceAnalysisTrailCameraQueryBucket> cameraBuckets = new ArrayList<>();
                for (int j = 0; j < camBuckets.size(); j++) {
                    JSONObject agg2 = camBuckets.getJSONObject(j);
                    TraceAnalysisTrailCameraQueryBucket cameraBucket = new TraceAnalysisTrailCameraQueryBucket();
                    cameraBucket.setCameraId(agg2.getString("key"));
                    cameraBucket.setCameraCount(agg2.getInteger("doc_count"));
                    JSONArray hits = agg2.getJSONObject("camera_person_hits").getJSONObject("hits").getJSONArray("hits");
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
//                        outHit.setLeaveTime(formatTime(source.getString("leave_time")));
//                        outHit.setOpTime(formatTime(source.getString("op_time")));
                        outHit.setLibId(source.getInteger("lib_id"));
                        outHit.setPersonId(source.getString("person_id"));
                        outHit.setSimilarity(source.getFloatValue("similarity"));
                        if (source.containsKey("img_width")) {
                            outHit.setImgWidth(source.getIntValue("img_width"));
                        }
                        if (source.containsKey("img_height")) {
                            outHit.setImgWidth(source.getIntValue("img_height"));
                        }
                        if (source.containsKey("left_pos")) {
                            outHit.setLeftPos(source.getIntValue("left_pos"));
                        }
                        if (source.containsKey("top")) {
                            outHit.setTop(source.getIntValue("top"));
                        }
                        outHits.add(outHit);
                    }
                    cameraBucket.setPersonHits(outHits);
                    cameraBuckets.add(cameraBucket);
                }
                timeBucket.setTimeBucket(cameraBuckets);
                if (personTrailMap != null) {
                    personTrailMap.put(enterTime, timeBucket);
                }
            }
        }
    }


    public JSONObject getRequestSearch(JSONObject param) {
        BaseEsSearch bs = new BaseEsSearch();
        // HTTP连接
        JSONObject httpCon = bs.httpConnection.esHttpConnect(esurl);
        if (httpCon != null) {
            return httpCon;
        }

        StringBuffer sb = bs.getSearchResult(param);
        // [lq-add]
        if (sb.toString().equals(new StringBuffer(FssErrorCodeEnum.SENSETIME_FEATURE_POINTS_ERROR.getCode()).toString())) {
            return bs.getErrorResult(FssErrorCodeEnum.SENSETIME_FEATURE_POINTS_ERROR.getCode());
        }
        if (sb.toString().equals(new StringBuffer(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()).toString())) {
            return bs.getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        }
        String esResults = sb.toString();
        JSONObject jsonEsResult = JSONObject.parseObject(esResults);
        if (jsonEsResult.get("error") != null) {
            LOGGER.info("Query es error!! params:" + param.toJSONString() + ".\terror:" + jsonEsResult.toJSONString());
            return bs.getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        }
        if (jsonEsResult.getBoolean("timed_out")) {
            return bs.getErrorResult(FssErrorCodeEnum.ES_TIMEOUT_EXCEPTION.getCode());
        }
        jsonEsResult.put("errorCode", FssErrorCodeEnum.SUCCESS.getCode());
        return jsonEsResult;
    }

    @Override
    public void run() {
        nightPersonSearch(queryParam);
        threadsSignal.countDown();
    }
}
