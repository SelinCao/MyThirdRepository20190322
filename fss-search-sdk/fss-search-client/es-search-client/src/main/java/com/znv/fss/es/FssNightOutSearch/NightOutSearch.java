package com.znv.fss.es.FssNightOutSearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisSearchTrailQueryAgg;
import com.znv.fss.es.TraceAnalysisSearch.TraceAnalysisSearchTrailTimeQueryBucket;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;


/**
 * Created by Administrator on 2017/12/5.
 */
public class NightOutSearch extends BaseEsSearch {
    protected static final Logger LOGGER = LogManager.getLogger(NightOutSearch.class);
    private String esurl;
    private String templateName;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public NightOutSearch(String esurl, String tempalteName) {
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


    @Override
    public JSONObject requestSearch(String params) {
        long startTime = System.currentTimeMillis();
        //FeatureCompUtil fc = new FeatureCompUtil();
        NightOutSearchJsonIn inputParam = JSON.parseObject(params, NightOutSearchJsonIn.class);
        NightOutSearchQueryParam queryParams = inputParam.getParams();
        int errCode = paramCheck(queryParams);
        if (errCode != FssErrorCodeEnum.SUCCESS.getCode()) {
            return getErrorResult(errCode);
        }

        //重新封装查询参数,参数中不添加时间
        JSONObject obj = getTemplateParams(queryParams);

        //多线程进行封装参数及查询
        int days = 0;
        Date initialStartDate;
        Date initialEndDate;
        String timeStart = queryParams.getTimeStart();
        String timeEnd = queryParams.getTimeEnd();
        try {
            initialStartDate = sdf.parse(queryParams.getDateStart());
            initialEndDate = sdf.parse(queryParams.getDateEnd());
            days = (int) (initialEndDate.getTime() - initialStartDate.getTime()) / 1000 / 60 / 60 / 24 + 1;
        } catch (ParseException e) {
            LOGGER.error("频繁夜出时间转换错误，如期参数必须是yyyy-MM-dd格式");
            return getErrorResult(FssErrorCodeEnum.ES_INVALID_PARAM.getCode());
        }

        NightOutSearchJsonOut outputResult = new NightOutSearchJsonOut();
        //按人员分组后的结果
        List<NightOutSearchQueryPersonHit> personlist = new ArrayList<>();
        //按时间聚合后结果
        List<TraceAnalysisSearchTrailTimeQueryBucket> timeBuckets = new ArrayList<>();
        Map<String, NightOutSearchQueryPersonHit> personMap = new ConcurrentHashMap<>();
        Map<String, TraceAnalysisSearchTrailTimeQueryBucket> personTrailMap = new ConcurrentHashMap<>();
        CountDownLatch threadsSignal = new CountDownLatch(days);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(7);
        Calendar calendar = new GregorianCalendar();
        for (int i = 0; i < days; i++) {
            calendar.setTime(initialStartDate);
            calendar.add(calendar.DATE, i);
            Date dateStart = calendar.getTime();
            String enterTimeStart = sdf.format(dateStart) + " " + timeStart;
            String enterTimeEnd = null;
            if (timeEnd.compareTo(timeStart) > 0) {
                enterTimeEnd = sdf.format(dateStart) + " " + timeEnd;
            } else {
                calendar.add(calendar.DATE, 1);
                Date dateEnd = calendar.getTime();
                enterTimeEnd = sdf.format(dateEnd) + " " + timeEnd;
            }
            JSONObject queryParam = (JSONObject) obj.clone();
            queryParam.getJSONObject("params").put("enter_time_start", enterTimeStart);
            queryParam.getJSONObject("params").put("enter_time_end", enterTimeEnd);

            if (queryParams.getPersonAggregation()) {
                Thread t = new NightOutSearchThread(threadsSignal, esurl, queryParam.toString(), personMap);
                fixedThreadPool.execute(t);

            } else if (queryParams.getTrailAggregation()) {
                Thread t = new NightOutSearchTrailThread(threadsSignal, esurl, queryParam.toString(), personTrailMap);
                fixedThreadPool.execute(t);
            }
        }

        //等待所有子线程执行完
        try {
            threadsSignal.await();
        } catch (InterruptedException e) {
            fixedThreadPool.shutdown();
            e.printStackTrace();
            LOGGER.error("线程等待失败" + e);
        }
        fixedThreadPool.shutdown();

        if (queryParams.getPersonAggregation()) {
            //将结果放在集合里
            for (Map.Entry<String, NightOutSearchQueryPersonHit> entry : personMap.entrySet()) {
                NightOutSearchQueryPersonHit val = entry.getValue();
                personlist.add(val);
            }

            //取前topN条显示
            List<NightOutSearchQueryPersonHit> outList = new ArrayList<>();
            if (personlist != null && personlist.size() > 0 && queryParams.getPersonNumbers() != 0) {
                getCatchCountSortList(personlist, queryParams);
                outList = getPeronResult(personlist, queryParams.getPersonNumbers());
            }

            if (outList == null || outList.size() == 0)
                outputResult.setTotal(0);
            else
                outputResult.setTotal(outList.size());
            outputResult.setPersonHits(outList);

        } else if (queryParams.getTrailAggregation()) {
            //将结果放在集合里
            for (Map.Entry<String, TraceAnalysisSearchTrailTimeQueryBucket> entry : personTrailMap.entrySet()) {
                TraceAnalysisSearchTrailTimeQueryBucket val = entry.getValue();
                timeBuckets.add(val);
            }

            //对返回结果进行排序
            if (timeBuckets != null && timeBuckets.size() > 0) {
                getCatchTimeSortList(timeBuckets, queryParams);
            }

            //封装返回结果
            TraceAnalysisSearchTrailQueryAgg trailAgg = new TraceAnalysisSearchTrailQueryAgg();
            if (timeBuckets == null || timeBuckets.size() == 0)
                outputResult.setTotal(0);
            else
                outputResult.setTotal(timeBuckets.size());
            trailAgg.setTrailBucket(timeBuckets);
            outputResult.setTrailQueryAgg(trailAgg);

        }
        outputResult.setErrorcode(FssErrorCodeEnum.SUCCESS.getCode());
        long endTime = System.currentTimeMillis();
        outputResult.setTook((endTime - startTime));
        return (JSONObject) JSONObject.toJSON(outputResult);
    }

    public List<NightOutSearchQueryPersonHit> getPeronResult(List<NightOutSearchQueryPersonHit> personlist, int topN) {
        if (personlist == null || personlist.size() <= 0) {
            return null;
        }
        List<NightOutSearchQueryPersonHit> outList = new ArrayList<>();
        if (personlist.size() <= topN) {
            outList = personlist;
        } else {
            for (int i = 0; i < topN; i++) {
                outList.add(personlist.get(i));
            }
        }
        return outList;
    }

    //对查询结果进行排序
    public void getCatchCountSortList(List<NightOutSearchQueryPersonHit> personlist, NightOutSearchQueryParam queryParams) {
        if (queryParams.getCountOrder().equals("asc")) {
            Collections.sort(personlist, new Comparator<NightOutSearchQueryPersonHit>() {
                @Override
                public int compare(NightOutSearchQueryPersonHit o1, NightOutSearchQueryPersonHit o2) {
                    return Integer.compare(o1.getPersonFrequency(), o2.getPersonFrequency());
                }
            });
        } else {
            Collections.sort(personlist, new Comparator<NightOutSearchQueryPersonHit>() {
                @Override
                public int compare(NightOutSearchQueryPersonHit o1, NightOutSearchQueryPersonHit o2) {
                    return Integer.compare(o2.getPersonFrequency(), o1.getPersonFrequency());
                }
            });
        }
    }

    // 查询结果降序排序
    public void getCatchTimeSortList(List<TraceAnalysisSearchTrailTimeQueryBucket> list, NightOutSearchQueryParam queryParams) {
        // enter_time降序排序
        Collections.sort(list, new Comparator<TraceAnalysisSearchTrailTimeQueryBucket>() {
            @Override
            public int compare(TraceAnalysisSearchTrailTimeQueryBucket o1, TraceAnalysisSearchTrailTimeQueryBucket o2) {
                String catchTime1 = o1.getEnterTime();
                String catchTime2 = o2.getEnterTime();
                if (queryParams.getSortOrder().equals("asc")) {
                    return catchTime1.compareTo(catchTime2);
                } else{
                    return catchTime2.compareTo(catchTime1);//默认降序
                }
            }
        });
    }

    private int paramCheck(NightOutSearchQueryParam inParam) {
        if (StringUtils.isEmpty(inParam.getDateStart())) {
            LOGGER.info("StartTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (StringUtils.isEmpty(inParam.getDateEnd())) {
            LOGGER.info("EndTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (inParam.getDateStart().compareTo(inParam.getDateEnd()) > 0) {
            LOGGER.info("EndTime can't larger than startTime ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        /*if (inParam.getFrom() < 0) {
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
        }*/
        return FssErrorCodeEnum.SUCCESS.getCode();
    }

    private JSONObject getTemplateParams(NightOutSearchQueryParam inParam) {

        JSONObject paramsT = new JSONObject();
        // paramsT.put("enter_time_start", inParam.getEnterTimeStart());
        // paramsT.put("enter_time_end", inParam.getEnterTimeEnd());
        paramsT.put("from", inParam.getFrom());
        paramsT.put("size", inParam.getSize());
        if (inParam.getPersonAggregation() == true) {
            String includes[] = {"uuid", "img_url", "enter_time", "big_picture_uuid"};
            paramsT.put("person_aggregation", inParam.getPersonAggregation());
            paramsT.put("includes", includes);
            paramsT.put("count_order", "desc");
            paramsT.put("person_numbers", 50);
            paramsT.put("frequency",inParam.getFrequency());
        }
        if (inParam.getTrailAggregation() == true) {
            paramsT.put("trail_aggregation", inParam.getTrailAggregation());
            String includes[] = {"person_id", "img_url", "leave_time", "img_width", "img_height", "camera_id", "left_pos", "office_id", "office_name", "top", "lib_id", "img_url", "similarity", "camera_name", "enter_time", "score", "op_time", "big_picture_uuid", "uuid"};
            paramsT.put("includes", includes);
            paramsT.put("person_size", inParam.getPerosnSize());
            paramsT.put("search_interval", "1m");
            if (inParam.getFusedId() != null && !inParam.getFusedId().isEmpty()) {
                paramsT.put("fused_id", inParam.getFusedId());
                paramsT.put("is_fused", true);
            }

        }
        if (inParam.getOfficeId() != null && !inParam.getOfficeId().isEmpty()) {
            paramsT.put("office_id", inParam.getOfficeId());
            paramsT.put("is_office", true);
        }
        if (inParam.getCameraId() != null && !inParam.getCameraId().isEmpty()) {
            paramsT.put("camera_id", inParam.getCameraId());
            paramsT.put("is_camera", true);
        }

        if (inParam.getSortField() != null && !inParam.getSortField().isEmpty()) {
            paramsT.put("sort_field", inParam.getSortField());
            paramsT.put("sort_order", inParam.getSortOrder());
            paramsT.put("is_sort", true);
        }
        JSONObject obj = new JSONObject();
        obj.put("id", templateName);
        obj.put("params", paramsT);

        return obj;
    }
}
