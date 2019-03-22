package com.znv.fss.es.FssNightOutSearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.znv.fss.es.FormatObject.formatTime;

public class NightOutSearchThread extends Thread {
    private final Log LOGGER = LogFactory.getLog(NightOutSearchThread.class);
    private CountDownLatch threadsSignal;
    private String esurl;
    private String queryParam;
    private Map<String, NightOutSearchQueryPersonHit> personMap;

    public NightOutSearchThread(CountDownLatch threadsSignal, String esurl, String queryParam, Map<String,
            NightOutSearchQueryPersonHit> personMap) {
        this.threadsSignal = threadsSignal;
        this.esurl = esurl;
        this.queryParam = queryParam;
        this.personMap = personMap;
    }

    // 重新封装返回结果
    public void nightPersonSearch(String param) {
        JSONObject queryParam = JSONObject.parseObject(param);
        JSONObject jsonEsResult = getRequestSearch(queryParam);

        if (jsonEsResult.getInteger("errorCode") == 100000) {
            JSONArray aggBuckets = jsonEsResult.getJSONObject("aggregations").getJSONObject("group_by_person").getJSONArray("buckets");
            for (int i = 0; i < aggBuckets.size(); i++) {
                JSONObject agg = aggBuckets.getJSONObject(i);
                NightOutSearchQueryPersonHit personHit = new NightOutSearchQueryPersonHit();
                if(agg.getInteger("doc_count") >= queryParam.getJSONObject("params").getInteger("frequency")){
                    String fusedId = agg.getString("key");
                    personHit.setFusedId(fusedId);
                    personHit.setPersonFrequency(agg.getInteger("doc_count"));
                    JSONObject personData = (JSONObject) agg.getJSONObject("top_person_hits").getJSONObject("hits").getJSONArray("hits").get(0);
                    JSONObject source = personData.getJSONObject("_source");
                    personHit.setBigPictureUuid(source.getString("big_picture_uuid"));
                    String enterTime = formatTime(source.getString("enter_time"));
                    personHit.setEnterTime(enterTime);
                    personHit.setImgUrl(source.getString("img_url"));
                    if (personMap != null) {
                        if (personMap.containsKey(fusedId)) {
                            NightOutSearchQueryPersonHit hit = personMap.get(fusedId);
                            int countSum = hit.getPersonFrequency() + personHit.getPersonFrequency();
                            if (enterTime.compareTo(hit.getEnterTime()) > 0) {
                                personHit.setPersonFrequency(countSum);
                                personMap.put(fusedId, personHit);
                            } else {
                                hit.setPersonFrequency(countSum);
                                personMap.put(fusedId, hit);
                            }
                        } else {
                            personMap.put(fusedId, personHit);
                        }
                    }

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
