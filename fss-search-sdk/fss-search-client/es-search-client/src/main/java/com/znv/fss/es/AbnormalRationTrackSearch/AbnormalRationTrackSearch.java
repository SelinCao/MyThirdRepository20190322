package com.znv.fss.es.AbnormalRationTrackSearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.common.utils.FeatureCompUtil;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.EsManager;
import com.znv.fss.es.AbnormalRationSearch.AimPersonQueryHit;
import com.znv.fss.es.AbnormalRationSearch.AbnormalRationSearch;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.znv.fss.es.FormatObject.formatTime;

public class AbnormalRationTrackSearch extends BaseEsSearch {
    protected static final Logger LOGGER = LogManager.getLogger(AbnormalRationSearch.class);
    private final int maxN = 100; // 最多返回条数
    private String sortOrder = "desc"; // asc：升序，desc：降序
//    private List<AbnormalRationTrackoutputData> outputList = new ArrayList<AbnormalRationTrackoutputData>();

    private String esurl;
    private String templateName;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    FeatureCompUtil fc = new FeatureCompUtil();

    public AbnormalRationTrackSearch(String esurl, String tempalteName) {
        this.esurl = esurl;
        this.templateName = tempalteName;
    }


    @Override
    protected JSONObject requestSearch(String params) throws Exception {
        long timeStart = System.currentTimeMillis();
        AbnormalRationTrackJsonIn fssSearchJsonIn = JSON.parseObject(params, AbnormalRationTrackJsonIn.class);
        AbnormalRationTrackQueryParam queryParams = fssSearchJsonIn.getParams();

        //输入参数校验
        int errCode = paramCheck(queryParams);
        if (errCode != FssErrorCodeEnum.SUCCESS.getCode()) {
            return getErrorResult(errCode, 0);
        }

        /*if (queryParams.getFrom() > 0 && outputList.size() > 0) {
            int startIndex = queryParams.getFrom();
            if (startIndex >= outputList.size()) {
                return getErrorResult(FssErrorCodeEnum.SUCCESS.getCode(), 0);
            }
            int stopIndex = startIndex + queryParams.getSize();
            if (startIndex > outputList.size()) {
                stopIndex = outputList.size();
            }
            List<AbnormalRationTrackoutputData> resultOutputList = new ArrayList<AbnormalRationTrackoutputData>();
            for (int i = startIndex; i < stopIndex; i++) {
                resultOutputList.add(outputList.get(i));
            }

            // 组输出结果
            AbnormalRationTrackJsonOut result = new AbnormalRationTrackJsonOut();
            result.setId(EsManager.SearchId.AbnormalRationTrackSearch.getId());
            result.setErrorcode(FssErrorCodeEnum.SUCCESS.getCode());
            result.setTotal(outputList.size());
            result.setCount(resultOutputList.size());
            result.setPeerTrackData(resultOutputList);
            long timeEnd = System.currentTimeMillis();
            result.setTime((timeEnd - timeStart));

            JSONObject jsonResult = (JSONObject) JSON.toJSON(result);
            return jsonResult;
        }*/
        //以脸搜脸参数
        JSONObject faceParam = getFaceSearchParam(queryParams);
        //以脸搜脸获取目标人结果
        JSONObject faceSearchResult = null;
        try {
            BaseEsSearch search = EsManager.createSearch(faceParam.toJSONString());
            faceSearchResult = search.getSearchResult(faceParam.toJSONString());
        } catch (Exception e) {
            faceSearchResult.put("errorCode", "FaceSearchFailed!");
            LOGGER.error("FaceSearchFailed!" + e);
        }

        int total=faceSearchResult.getJSONObject("hits").getIntValue("total");
        //以脸搜脸失败则失败
        if (!faceSearchResult.getString("errorCode").equals("100000")) {
            return getErrorResult(Integer.parseInt(faceSearchResult.getString("errorCode")), 0);
        }

        //  封装目标人查询结果，查询为空则返回查无数据
        List<AimPersonQueryHit> aimSearchResultHits = getFaceSearchResultJson(faceSearchResult);
        if (aimSearchResultHits == null || aimSearchResultHits.size() == 0) {
            return getErrorResult(FssErrorCodeEnum.SUCCESS.getCode(), 0);
        }

        // 目标人信息按时间倒排序
        getCatchTimeSortList(aimSearchResultHits);

        //创建同行人查询且封装同行人返回结果，查询为空则返回查无数据 queryParam web输入参数
        List<AbnormalRationTrackoutputData> outputList = excutePeerSearch(aimSearchResultHits, queryParams);
        // 组输出结果
        AbnormalRationTrackJsonOut result = new AbnormalRationTrackJsonOut();
        result.setId(EsManager.SearchId.AbnormalRationTrackSearch.getId());
        result.setErrorcode(FssErrorCodeEnum.SUCCESS.getCode());
        if (outputList == null || outputList.isEmpty()) {
            result.setCount(0);
            result.setTotal(total);
        } else {
            result.setCount(outputList.size());
            result.setTotal(total);
            result.setPeerTrackData(outputList);
            /*if (queryParams.getSize() >= outputList.size()) {
                result.setCount(outputList.size());
                result.setTotal(outputList.size());
                result.setPeerTrackData(outputList);
            } else {
                List<AbnormalRationTrackoutputData> resultOutputList = new ArrayList<AbnormalRationTrackoutputData>();
                for (int i = 0; i < queryParams.getSize(); i++) {
                    resultOutputList.add(outputList.get(i));
                }
                result.setTotal(outputList.size());
                result.setCount(resultOutputList.size());
                result.setPeerTrackData(resultOutputList);
            }*/
        }
        long timeEnd = System.currentTimeMillis();
        result.setTime((timeEnd - timeStart));

        JSONObject jsonResult = (JSONObject) JSON.toJSON(result);
        return jsonResult;
    }


    /**
     * 执行同行人查询结果
     *
     * @param aimSearchHits
     * @param queryParams
     * @return
     */
    private List<AbnormalRationTrackoutputData> excutePeerSearch(List<AimPersonQueryHit> aimSearchHits,
                                                                 AbnormalRationTrackQueryParam queryParams) {
        List<AbnormalRationTrackoutputData> outputList = new ArrayList<AbnormalRationTrackoutputData>();
        // 查找同行人,将同行人信息写入到map中
        Map<String, AbnormalRationTrackoutputData> targetAndPeerMap = new ConcurrentHashMap<String, AbnormalRationTrackoutputData>();
        for (AimPersonQueryHit aimHit : aimSearchHits) {
            if (aimHit.getCameraId() != null && !aimHit.getCameraId().isEmpty()) {
                //查询每个目标人的同行人，并将其添加在同行人封装结果链表中
                getPeerInfo(aimHit, queryParams, targetAndPeerMap);
            }
        }
        // 缓存返回结果，并将同一个目标人的同行人信息组在一个结果集中
        for (Map.Entry<String, AbnormalRationTrackoutputData> entry : targetAndPeerMap.entrySet()) {
            AbnormalRationTrackoutputData val = entry.getValue();
            if (val.getPeerList() != null && val.getPeerList().size() > 1) {
                removeDuplicate(val.getPeerList());
            }
            outputList.add(val);
        }
        if (outputList != null || outputList.size() > 1) {
            getPeerSortList(outputList);
        }

        return outputList;
    }

    //删除同一目标人所对应同行人的重复元素
    private static void removeDuplicate(List<PeerOutputData> peerList) {
        for (int i = 0; i < peerList.size() - 1; i++) {
            for (int j = peerList.size() - 1; j > i; j--) {
                if (peerList.get(i).getPeerUuid().equals(peerList.get(j).getPeerUuid())) {
                    peerList.remove(j);
                }
            }
        }
    }


    // 查询结果降序排序
    public void getCatchTimeSortList(List<AimPersonQueryHit> list) {
        // enter_time降序排序
        Collections.sort(list, new Comparator<AimPersonQueryHit>() {
            @Override
            public int compare(AimPersonQueryHit o1, AimPersonQueryHit o2) {
                String catchTime1 = o1.getEnterTime();
                String catchTime2 = o2.getEnterTime();
                if (sortOrder.equals("asc")) {
                    return catchTime1.compareTo(catchTime2);
                } else if (sortOrder.equals("desc")) {
                    return catchTime2.compareTo(catchTime1);
                }
                return catchTime2.compareTo(catchTime1);// 默认降序
            }
        });
    }


    // 查询结果降序排序
    private void getPeerSortList(List<AbnormalRationTrackoutputData> list) {
        // enter_time降序排序
        Collections.sort(list, new Comparator<AbnormalRationTrackoutputData>() {
            @Override
            public int compare(AbnormalRationTrackoutputData o1, AbnormalRationTrackoutputData o2) {
                String catchTime1 = o1.getTargetData().getEnterTime();
                String catchTime2 = o2.getTargetData().getEnterTime();
                if (sortOrder.equals("asc")) {
                    return catchTime1.compareTo(catchTime2);
                } else if (sortOrder.equals("desc")) {
                    return catchTime2.compareTo(catchTime1);
                }
                return catchTime2.compareTo(catchTime1);// 默认降序
            }
        });
    }


    /**
     * 查询一个目标人的同行人
     *
     * @param aimHit
     * @param targetAndPeerMap
     * @param queryParams
     */
    private void getPeerInfo(AimPersonQueryHit aimHit, AbnormalRationTrackQueryParam queryParams, Map<String,
            AbnormalRationTrackoutputData> targetAndPeerMap) {
        List<String> featureStrs = new ArrayList<>();
        featureStrs = queryParams.getFeatureValue();
        List<byte[]> searchFeatures = new ArrayList<>(featureStrs.size());
        Base64 base64 = new Base64();
        for (String feature : featureStrs) {
            searchFeatures.add(base64.decode(feature));
        }
        int peerInterval = queryParams.getPeerinterval();
        float simThrehold = (float) queryParams.getSimThreshold();


        String enterTime = aimHit.getEnterTime();
        String leaveTime = aimHit.getLeaveTime();

        String[] cameraId = {aimHit.getCameraId()};
        String uuid = aimHit.getUuid();

        String startT = null;
        String stopT = null;

        try {
            long enterStamp = sdf.parse(enterTime).getTime();
            long leaveStamp;
            if (leaveTime != null && !leaveTime.isEmpty()) {
                leaveStamp = sdf.parse(leaveTime).getTime();
            } else {
                long durationStamp = aimHit.getDurationTime();
                if (durationStamp > 1) {
                    leaveStamp = enterStamp + durationStamp * 1000l;
                } else {
                    leaveStamp = enterStamp + 5 * 1000l;
                }
            }
            long startTime = enterStamp - peerInterval * 1000l;
            long stopTime = leaveStamp + peerInterval * 1000l;
            startT = sdf.format(startTime);
            stopT = sdf.format(stopTime);
        } catch (ParseException e) {
            LOGGER.error(e);
        }
        //为该目标人查找同行人需要的参数
        JSONObject peerSearchParam = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("camera_id", cameraId);
        params.put("enter_time_start", startT);
        params.put("enter_time_end", stopT);  //todo
        params.put("from", 0);
        params.put("size", 9999);
        peerSearchParam.put("id", EsManager.SearchId.FssHistorySearch.getId());
        peerSearchParam.put("params", params);


        //以脸搜脸获取目标人结果
        JSONObject peerSearchResult = null;
        try {
            BaseEsSearch search = EsManager.createSearch(peerSearchParam.toJSONString());
            peerSearchResult = search.getSearchResult(peerSearchParam.toJSONString());
        } catch (Exception e) {
            peerSearchResult.put("errorCode", "peerSearchFailed!");
            LOGGER.error("peerSearchFailed!" + e);
        }

        List<PeerOutputData> peerList = new ArrayList<PeerOutputData>();
        AbnormalRationTrackoutputData outData = new AbnormalRationTrackoutputData();
        outData.setPeerList(peerList);
        outData.setTargetData(aimHit);
        targetAndPeerMap.put(uuid, outData);

        //从es查询结果中获取hits
        JSONArray searchHits = peerSearchResult.getJSONObject("hits").getJSONArray("hits");
        for (int i = 0; i < searchHits.size(); i++) {
            JSONObject source = searchHits.getJSONObject(i).getJSONObject("_source");
            byte[] feature = Base64.decodeBase64(source.getString("rt_feature"));
            if (feature != null || feature.length > 0) {
                if (isSamePerson(feature, searchFeatures, simThrehold)) {
                    continue;//查询本人跳过
                }
            }
            PeerOutputData peerData = new PeerOutputData();
            String peerEnterTime = formatTime(source.getString("enter_time"));
            String peerLeaveTime = formatTime(source.getString("leave_time"));
            peerData.setImgUrl(source.getString("img_url"));
            peerData.setPeerUuid(source.getString("uuid"));
            peerData.setPeerEnterTime(peerEnterTime);
            try {
                long peerEnterTimeStamp = sdf.parse(peerEnterTime).getTime();
                long peerLeaveTimeStamp = sdf.parse(peerLeaveTime).getTime();
                long enterStamp = sdf.parse(enterTime).getTime();
                long leaveStamp = sdf.parse(leaveTime).getTime();
                // 三个时间取最小
                long intervalTime1 = Math.abs(enterStamp - peerEnterTimeStamp);
                long intervalTime2 = Math.abs(enterStamp - peerLeaveTimeStamp);
                long intervalTime3 = Math.abs(leaveStamp - peerEnterTimeStamp);
                long[] intervalList = {intervalTime1, intervalTime2, intervalTime3};
                long intervalTime = intervalTime1; // 三个间隔时间中的最小值
                for (int j = 0; j < intervalList.length; j++) {
                    if (intervalList[j] < intervalTime) {
                        intervalTime = intervalList[j];
                    }
                }
                peerData.setIntervalTime(intervalTime / 1000);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            peerList.add(peerData);

            // [lq-add]没有同行人时也返回目标人信息
            // 判断map中key是否存在，已存在则将查询结果中的同行人信息加入原数据中，不存在则组一个新的list
            if (targetAndPeerMap.containsKey(uuid)) {
                targetAndPeerMap.get(uuid).getPeerList().addAll(peerList);
            } else {
                AbnormalRationTrackoutputData resultOutData = new AbnormalRationTrackoutputData();
                resultOutData.setPeerList(peerList);
                resultOutData.setTargetData(aimHit);
                targetAndPeerMap.put(uuid, resultOutData);
            }
        }
    }

    private boolean isSamePerson(byte[] feature, List<byte[]> searchFeature, float simThreshold) {
        for (byte[] temp : searchFeature) { //只要有一个相似，即认为是同一人脸
            float sim = fc.Comp(feature, temp, 12); // 比对相似度
            if (sim >= simThreshold) { // 相似度超过阈值，认为是同一类人脸
                return true;
            }
        }
        return false;
    }

    /**
     * 错误码返回结果封装
     *
     * @param errorCode
     * @return
     */
    public JSONObject getErrorResult(int errorCode, int total) {
        JSONObject result = new JSONObject();
        // 组输出结果
        AbnormalRationTrackJsonOut fssPeerTrackReportServiceOut = new AbnormalRationTrackJsonOut();
        fssPeerTrackReportServiceOut.setId(EsManager.SearchId.AbnormalRationTrackSearch.getId());
        fssPeerTrackReportServiceOut.setErrorcode(errorCode);
        fssPeerTrackReportServiceOut.setTotal(total);
        fssPeerTrackReportServiceOut.setCount(0);
        result = (JSONObject) JSON.toJSON(fssPeerTrackReportServiceOut);
        return result;
    }

    //输入参数校验
    public int paramCheck(AbnormalRationTrackQueryParam inParam) {
        if (StringUtils.isEmpty(inParam.getEnterTimeStart())) {
            LOGGER.info("StartTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (StringUtils.isEmpty(inParam.getEnterTimeEnd())) {
            LOGGER.info("EndTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (inParam.getIsCalcSim() == true) {
            if (inParam.getFeatureValue().size() == 0) {
                LOGGER.info("FeatureValue can't be empty ！");
                return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
            }
        }
        if (inParam.getFrom() < 0) {
            LOGGER.info("From is too small ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (inParam.getSize() < 1) {
            LOGGER.info("Size is too small ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if ((inParam.getFrom() + inParam.getSize()) >= 10000) {
            LOGGER.info("Request result set out of range ！");
            return FssErrorCodeEnum.ES_SIZE_OUT_OF_RANGE.getCode();
        }
        return FssErrorCodeEnum.SUCCESS.getCode();
    }

    /**
     * 封装人脸目标人返回结果
     *
     * @param faceSearchResult
     * @return
     */
    private List<AimPersonQueryHit> getFaceSearchResultJson(JSONObject faceSearchResult) {
        JSONArray searchHits = faceSearchResult.getJSONObject("hits").getJSONArray("hits");
        List<AimPersonQueryHit> aimHits = new ArrayList();
        FeatureCompUtil fc = new FeatureCompUtil();
        for (int i = 0; i < searchHits.size(); i++) {
            JSONObject hit = searchHits.getJSONObject(i).getJSONObject("_source");
            AimPersonQueryHit aimHit = new AimPersonQueryHit();
            float sim = searchHits.getJSONObject(i).getFloatValue("score");
            aimHit.setSim(fc.Normalize(sim));
            String enterTime = formatTime(hit.getString("enter_time"));
            String leaveTime = formatTime(hit.getString("leave_time"));

            aimHit.setUuid(hit.getString("uuid"));
            aimHit.setEnterTime(enterTime);
            aimHit.setLeaveTime(leaveTime);
            aimHit.setImgUrl(hit.getString("img_url"));
            aimHit.setDurationTime(hit.getLongValue("duration_time"));
            aimHit.setCameraId(hit.getString("camera_id"));
            aimHit.setCameraName(hit.getString("camera_name"));
            aimHit.setGpsx(hit.getDoubleValue("gpsx"));
            aimHit.setGpsy(hit.getDoubleValue("gpsy"));
            aimHit.setGpsz(hit.getDoubleValue("gpsz"));
            aimHits.add(aimHit);
        }

        return aimHits;
    }

    /**
     * 从输入参数构造以脸搜脸参数
     *
     * @param queryParams
     * @return
     */
    private JSONObject getFaceSearchParam(AbnormalRationTrackQueryParam queryParams) {
        JSONObject faceSearchParam = new JSONObject();
        JSONObject params = new JSONObject();
        if (queryParams.getEnterTimeStart() != null) {
            params.put("enter_time_start", queryParams.getEnterTimeStart());
        }
        if (queryParams.getEnterTimeEnd() != null) {
            params.put("enter_time_end", queryParams.getEnterTimeEnd());
        }
        params.put("feature_value", queryParams.getFeatureValue());
        params.put("from", queryParams.getFrom());
        params.put("size", queryParams.getSize());
        params.put("sort_field", "enter_time");
        params.put("sort_order", "desc");
        if (queryParams.getOfficeId() != null) {
            params.put("office_id", queryParams.getOfficeId());
            params.put("minimum_should_match", 1);
        }
        if (queryParams.getCameraId() != null) {
            params.put("camera_id", queryParams.getCameraId());
            params.put("minimum_should_match", 1);
        }
        if (queryParams.getFilterType() != null) {
            params.put("filter_type", queryParams.getFilterType());
        }
        if (queryParams.getIsCalcSim()) {
            params.put("is_calcSim", queryParams.getIsCalcSim());
        } else {
            params.put("is_calcSim", false);
        }
        if (queryParams.getSimThreshold() != 0) {
            params.put("sim_threshold", queryParams.getSimThreshold());
        } else {
            params.put("sim_threshold", 0.89);
        }
        faceSearchParam.put("id", EsManager.SearchId.FssHistorySearch.getId());
        faceSearchParam.put("params", params);

        return faceSearchParam;
    }
}
