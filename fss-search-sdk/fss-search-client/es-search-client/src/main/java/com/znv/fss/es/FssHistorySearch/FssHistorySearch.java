package com.znv.fss.es.FssHistorySearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.common.utils.FeatureCompUtil;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.EsConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Created by Administrator on 2017/12/5.
 */
public class FssHistorySearch extends BaseEsSearch {
    protected static final Logger LOGGER = LogManager.getLogger(FssHistorySearch.class);
    private String esurl;
    private String templateName;

    public FssHistorySearch(String esurl, String tempalteName) {
        this.esurl = esurl;
        this.templateName = tempalteName;
    }

    public JSONObject initConnectParams() {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;
    }

    // 重新封装返回结果
    @Override
    public JSONObject requestSearch(String params) {
        FssHistorySearchJsonIn inputParam = JSON.parseObject(params, FssHistorySearchJsonIn.class);
        FssHistorySearchQueryParam queryParams = inputParam.getParams();
        int errCode = paramCheck(queryParams);
        if (errCode != FssErrorCodeEnum.SUCCESS.getCode()) {
            return getErrorResult(errCode);
        }
        // HTTP连接
        JSONObject httpConResult = initConnectParams();
        if (httpConResult != null) {
            return httpConResult;
        }

        JSONObject obj = getTemplateParams(queryParams);

        StringBuffer sb = super.getSearchResult(obj);
        // [lq-add]
        if (sb.toString().equals(new StringBuffer(FssErrorCodeEnum.SENSETIME_FEATURE_POINTS_ERROR.getCode()).toString())) {
            return getErrorResult(FssErrorCodeEnum.SENSETIME_FEATURE_POINTS_ERROR.getCode());
        }
        if (sb.toString().equals(new StringBuffer(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()).toString())) {
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
        // 从es查询结果中获取hits
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("errorCode", FssErrorCodeEnum.SUCCESS.getCode());
        jsonResult.put("hits", jsonEsResult.getJSONObject("hits"));

        return jsonResult;
    }

    private int paramCheck(FssHistorySearchQueryParam inParam) {
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

    private JSONObject getTemplateParams(FssHistorySearchQueryParam inParam) {
        JSONObject paramsT = new JSONObject();
//        String excludes[] = {"rt_feature"};
        if (inParam.getEnterTimeStart() != null) {
            paramsT.put("enter_time_start", inParam.getEnterTimeStart());
        }
        if (inParam.getEnterTimeEnd() != null) {
            paramsT.put("enter_time_end", inParam.getEnterTimeEnd());
        }
//        paramsT.put("is_excludes", true);
//        paramsT.put("excludes", excludes);

        paramsT.put("from", inParam.getFrom());
        paramsT.put("size", inParam.getSize());
        if (inParam.getIsCalcSim() == true) {
            paramsT.put("is_calcSim", true);
            paramsT.put("feature_name", "rt_feature.feature_high");
            FeatureCompUtil fc = new FeatureCompUtil();
            fc.setFeaturePoints(EsConfig.getFeaturePoints());
            float sim = (float) inParam.getSimThreshold();
            paramsT.put("sim_threshold", fc.reversalNormalize(sim));//脚本中未归一化
            paramsT.put("feature_value", inParam.getFeatureValue());
        }

        if (inParam.getFilterType()!=null){
            paramsT.put("filter_type", inParam.getFilterType());
        }
        if (inParam.getSortField() != null) {
            paramsT.put("sortField", inParam.getSortField());
        }
        if (inParam.getSortOrder() != null) {
            paramsT.put("sortOrder", inParam.getSortOrder());
        }
        if (inParam.getOfficeId() != null && !inParam.getOfficeId().isEmpty()) {
            paramsT.put("office_id", inParam.getOfficeId());
            paramsT.put("is_office",true);
        }
        if (inParam.getCameraId() != null && !inParam.getCameraId().isEmpty()) {
            paramsT.put("camera_id", inParam.getCameraId());
            paramsT.put("is_camera",true);
        }
        JSONObject obj = new JSONObject();
        obj.put("id", templateName);
        obj.put("params", paramsT);

        return obj;
    }
}
