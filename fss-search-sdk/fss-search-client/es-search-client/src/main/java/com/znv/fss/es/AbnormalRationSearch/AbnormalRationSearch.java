package com.znv.fss.es.AbnormalRationSearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.common.utils.FeatureCompUtil;
import com.znv.fss.es.AbnormalRationTrackSearch.AbnormalRationTrackoutputData;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.EsManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.znv.fss.es.FormatObject.formatTime;

public class AbnormalRationSearch extends BaseEsSearch {
    protected static final Logger LOGGER = LogManager.getLogger(AbnormalRationSearch.class);
    private final int maxN = 100; // 最多返回条数

    private String esurl;
    private String templateName;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    FeatureCompUtil fc = new FeatureCompUtil();

    public AbnormalRationSearch(String esurl, String tempalteName) {
        this.esurl = esurl;
        this.templateName = tempalteName;
    }

    //输入参数校验
    public int paramCheck(AbnormalRationQueryParam inParam) {
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
        return FssErrorCodeEnum.SUCCESS.getCode();
    }

    @Override
    protected JSONObject requestSearch(String params) throws Exception {
        long timeStart = System.currentTimeMillis();
        AbnormalRationJsonIn fssSearchJsonIn = JSON.parseObject(params, AbnormalRationJsonIn.class);
        AbnormalRationQueryParam queryParams = fssSearchJsonIn.getParams();
        //输入参数校验
        int errCode = paramCheck(queryParams);
        if (errCode != FssErrorCodeEnum.SUCCESS.getCode()) {
            return getErrorResult(errCode);
        }
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
        //以脸搜脸失败则失败
        if (!faceSearchResult.getString("errorCode").equals("100000")) {
            return getErrorResult(Integer.parseInt(faceSearchResult.getString("errorCode")));
        }

        //  封装目标人查询结果，查询为空则返回查无数据
        List<AimPersonQueryHit> aimSearchResultHits = getFaceSearchResultJson(faceSearchResult);
        if (aimSearchResultHits == null || aimSearchResultHits.size() == 0) {
            return getErrorResult(FssErrorCodeEnum.SUCCESS.getCode());
        }

        //创建同行人查询且封装同行人返回结果，查询为空则返回查无数据 queryParam web输入参数
        List<AbnormalRationSearchHit> peerSearchHits = excutePeerSearch(aimSearchResultHits, queryParams);
        if (peerSearchHits == null || peerSearchHits.size() == 0) {
            return getErrorResult(FssErrorCodeEnum.SUCCESS.getCode());
        }
        //TODO 聚类 queryParam web输入参数
        List<AbnormalRationSearchHit> sortedPeerList = clusterPeerList(peerSearchHits, queryParams);

//        封装同行人查询结果并返回最终结果
        List<AbnormalRationSearchDataOut> outputList = getPeerSearchResult(sortedPeerList, queryParams.getTopn());
        // 组输出结果
        AbnormalRationJSONOut abnormalRationJSONOut = new AbnormalRationJSONOut();
        abnormalRationJSONOut.setId(EsManager.SearchId.AbnormalRationSearch.getId());
        abnormalRationJSONOut.setErrorCode(FssErrorCodeEnum.SUCCESS.getCode());
        if (outputList == null || outputList.isEmpty()) {
            abnormalRationJSONOut.setCount(0);
        } else {
            abnormalRationJSONOut.setCount(outputList.size());
            abnormalRationJSONOut.setFssPeerSearchPeerDataOut(outputList);
        }
        long timeEnd = System.currentTimeMillis();
        abnormalRationJSONOut.setTime((timeEnd - timeStart));
        /*AbnormalRationTrackSearchJsonOutput jsonout = new AbnormalRationTrackSearchJsonOutput();
        jsonout.setReportservice(abnormalRationJSONOut);*/
        JSONObject result = (JSONObject) JSON.toJSON(abnormalRationJSONOut);
        return result;
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
            aimHit.setUuid(hit.getString("uuid"));
            aimHit.setEnterTime(formatTime(hit.getString("enter_time")));
            aimHit.setLeaveTime(formatTime(hit.getString("leave_time")));
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
     * 执行同行人查询结果
     *
     * @param aimSearchHits
     * @param queryParams
     * @return
     */
    private List<AbnormalRationSearchHit> excutePeerSearch(List<AimPersonQueryHit> aimSearchHits, AbnormalRationQueryParam queryParams) {
        List<AbnormalRationSearchHit> peerSearchHits = new CopyOnWriteArrayList<>();
        Map<String, AbnormalRationSearchHit> peerMap = new ConcurrentHashMap<String,
                AbnormalRationSearchHit>();
        for (AimPersonQueryHit aimHit : aimSearchHits) {
            if (aimHit.getCameraId() != null && !aimHit.getCameraId().isEmpty()) {

                //查询每个目标人的同行人，并将其添加在同行人封装结果链表中
                getPeerInfo(aimHit, peerSearchHits, queryParams, peerMap);
            }
        }

        // 缓存返回结果，并将同一个目标人的同行人信息组在一个结果集中
        //List<PeerTrackOutputData> outputList = new ArrayList<PeerTrackOutputData>();
        for (Map.Entry<String, AbnormalRationSearchHit> entry : peerMap.entrySet()) {
            AbnormalRationSearchHit val = entry.getValue();
            peerSearchHits.add(val);
        }


        return peerSearchHits;
    }

    /**
     * 查询一个目标人的同行人
     *
     * @param aimHit
     * @param peerSearchHits
     */
    private void getPeerInfo(AimPersonQueryHit aimHit, List<AbnormalRationSearchHit> peerSearchHits,
                             AbnormalRationQueryParam queryParams, Map<String, AbnormalRationSearchHit> peerMap) {
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
        //从es查询结果中获取hits
        JSONArray searchHits = peerSearchResult.getJSONObject("hits").getJSONArray("hits");
        for (int i = 0; i < searchHits.size(); i++) {
            JSONObject source = searchHits.getJSONObject(i).getJSONObject("_source");
            byte[] feature = Base64.decodeBase64(source.getString("rt_feature"));
            String uuid = source.getString("uuid");
            if (feature != null) {
                if (isSamePerson(feature, searchFeatures, simThrehold)) {
                    continue;//查询本人跳过
                }
            }
            AbnormalRationSearchHit peerInfo = new AbnormalRationSearchHit();
            peerInfo.setImgUrl(source.getString("img_url"));
            peerInfo.setDurationTime(source.getLong("duration_time"));
            peerInfo.setFeature(feature);
            peerInfo.setLibId(Integer.parseInt(source.getString("lib_id")));
            peerInfo.setPersonId(source.getString("person_id"));
            if (peerMap.containsKey(uuid)){
                long duration=peerMap.get(uuid).getDurationTime()+source.getLong("duration_time");
                peerMap.get(uuid).setDurationTime(duration);
            }else {
                peerMap.put(uuid,peerInfo);
            }
//                peerSearchHits.add(peerInfo);
        }
    }

    /**
     * 同行人聚类
     *
     * @param peerSearchHits
     * @param peerSearchHits
     */
    private List<AbnormalRationSearchHit> clusterPeerList(List<AbnormalRationSearchHit> peerSearchHits, AbnormalRationQueryParam
            queryParams) {
        float simThreshold = (float) queryParams.getSimThreshold();
        FaceCluster faceCluster = new FaceCluster();
        Map<AbnormalRationSearchHit, List<AbnormalRationSearchHit>> peerClusterInfo = faceCluster.getFaceClusteringResult(peerSearchHits, simThreshold);

        //聚类后根据传入的排序类型对数据进行排序 并返回结果
        List<AbnormalRationSearchHit> sortPeerInfo = sortPeerInfo(peerClusterInfo, queryParams.getSorttype());
        return sortPeerInfo;
    }

    private List<AbnormalRationSearchHit> sortPeerInfo(Map<AbnormalRationSearchHit, List<AbnormalRationSearchHit>> clusteringMap, String sortType) {

        if (null == clusteringMap || clusteringMap.size() == 0) {
            LOGGER.warn("clusteringMap is null ");
            return null;
        }
        Iterator<Map.Entry<AbnormalRationSearchHit, List<AbnormalRationSearchHit>>> scnItr = clusteringMap.entrySet().iterator();
        List<AbnormalRationSearchHit> outDataList = new ArrayList<>();
        List<AbnormalRationSearchHit> sortDataList = new ArrayList<>();

        while (scnItr.hasNext()) {
            Map.Entry<AbnormalRationSearchHit, List<AbnormalRationSearchHit>> entry = scnItr.next();
            outDataList = entry.getValue();
            long peerTime = 0l;
            if (null != outDataList && outDataList.size() > 0) {
                for (AbnormalRationSearchHit peerData : outDataList) {
                    peerTime += peerData.getDurationTime();
                }
                AbnormalRationSearchHit leader = entry.getKey();
                leader.setCount(outDataList.size());
                leader.setDurationTime(peerTime);
                sortDataList.add(leader);
            }
        }
        if (sortType.equals("1")) {
            //次数
            Collections.sort(sortDataList, new Comparator<AbnormalRationSearchHit>() {
                @Override
                public int compare(AbnormalRationSearchHit o1, AbnormalRationSearchHit o2) {
                    return Integer.compare(o2.getCount(), o1.getCount());
                }
            });
        } else {//时长
            Collections.sort(sortDataList, new Comparator<AbnormalRationSearchHit>() {
                @Override
                public int compare(AbnormalRationSearchHit o1, AbnormalRationSearchHit o2) {
                    return Long.compare(o2.getDurationTime(), o1.getDurationTime());
                }
            });
        }
        return sortDataList;
    }

    /**
     * 封装同行人查询结果
     *
     * @param sortedPeerList
     * @return topN
     */
    private List<AbnormalRationSearchDataOut> getPeerSearchResult(List<AbnormalRationSearchHit> sortedPeerList, int topN) {
        if (null == sortedPeerList || sortedPeerList.size() <= 0) {
            return null;
        }
        List<AbnormalRationSearchDataOut> outList = new ArrayList(sortedPeerList.size());
        for (int i = 0; i < sortedPeerList.size(); i++) {
            AbnormalRationSearchDataOut outData = new AbnormalRationSearchDataOut();
            AbnormalRationSearchHit peerData = sortedPeerList.get(i);
            outData.setImgUrl(peerData.getImgUrl());
            outData.setPeerpersonid(peerData.getPersonId());
            outData.setPeerlibid(peerData.getLibId());
            outData.setPeercount(peerData.getCount());
            outData.setPeertime(peerData.getDurationTime());
//            outData.setImgUrl(peerData.getImgUrl());//todo imgUrl对应uuid？
            outList.add(outData);
            if (i == topN - 1) {
                break;
            }

        }
        return outList;
    }

    /**
     * 从输入参数构造以脸搜脸参数
     *
     * @param queryParams
     * @return
     */
    private JSONObject getFaceSearchParam(AbnormalRationQueryParam queryParams) {
        JSONObject faceSearchParam = new JSONObject();
        JSONObject params = new JSONObject();
        if (queryParams.getEnterTimeStart() != null) {
            params.put("enter_time_start", queryParams.getEnterTimeStart());
        }
        if (queryParams.getEnterTimeEnd() != null) {
            params.put("enter_time_end", queryParams.getEnterTimeEnd());
        }
        params.put("feature_value", queryParams.getFeatureValue());
        params.put("from", 0);
        params.put("size", 9999);
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
    public JSONObject getErrorResult(int errorCode) {
        JSONObject result = new JSONObject();
        // 组输出结果
        AbnormalRationJSONOut abnormalRationJSONOut = new AbnormalRationJSONOut();
        long timeEnd = System.currentTimeMillis();
        abnormalRationJSONOut.setId(EsManager.SearchId.AbnormalRationSearch.getId());
        abnormalRationJSONOut.setErrorCode(errorCode);
        abnormalRationJSONOut.setCount(0);
        result = (JSONObject) JSON.toJSON(abnormalRationJSONOut);
        return result;
    }

    public static String getSearchParam(String params) {
        JSONObject sParam = JSON.parseObject(params);
        String param = sParam.getString("params");
        return param;
    }
}
