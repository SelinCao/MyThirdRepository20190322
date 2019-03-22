package com.znv.fss.es.FssPersonListGroup;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import com.znv.fss.es.EsManager;
import com.znv.fss.es.FormatObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by ZNV on 2018/12/16.
 */
public class FssPersonListGroup extends BaseEsSearch{
    protected static final Logger LOG = LogManager.getLogger(FssPersonListGroup.class);

    private String esurl;
    private String templateName;

    public FssPersonListGroup(String esurl, String tempalteName) {
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
        long t1 = System.currentTimeMillis();
        PersonListGroupQueryParam queryParams = null;

        try {
            PersonListGroupJsonIn inputParam = JSON.parseObject(params, PersonListGroupJsonIn.class);
            queryParams = inputParam.getParams();
        } catch (Exception e){
            LOG.error(e);
            return getErrorResult(FssErrorCodeEnum.ES_INVALID_PARAM.getCode());
        }

        JSONObject jsonEsResult = new JSONObject();
        try {
            JSONObject obj = getAggsReault(queryParams, jsonEsResult);
            if (!obj.get("errorCode").equals(FssErrorCodeEnum.SUCCESS.getCode())) {
                return obj;
            }

            obj = getAgeGroupReault(queryParams, jsonEsResult);
            if (!obj.get("errorCode").equals(FssErrorCodeEnum.SUCCESS.getCode())) {
                return obj;
            }

            obj = getAddrGroupReault(queryParams, jsonEsResult);
            if (!obj.get("errorCode").equals(FssErrorCodeEnum.SUCCESS.getCode())) {
                return obj;
            }
            jsonEsResult.put("errorCode", FssErrorCodeEnum.SUCCESS.getCode());
        }catch (Exception e){
            LOG.error(e);
            jsonEsResult.put("errorCode", FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        }

        jsonEsResult.put("id", EsManager.SearchId.FssPersonListGroup.getId());
        long time = System.currentTimeMillis();
        jsonEsResult.put("time",String.valueOf(time - t1));

        return jsonEsResult;
    }

    private JSONObject getAddrGroupReault(PersonListGroupQueryParam queryParams, JSONObject resObj){
        JSONArray bucketsArray = new JSONArray();
        int total = 0;

        if(queryParams.getAddr() != null && !queryParams.getAddr().isEmpty()){
            for(String key : queryParams.getAddr()){
                if(key == null || (key != null && key.trim().equals(""))){
                    continue;
                }
                JSONObject obj = getTemplateParams(queryParams);
                JSONObject params = obj.getJSONObject("params");
                params.put("addr", key);

                // HTTP连接
                JSONObject httpConResult = initConnectParams();
                if (httpConResult != null) {
                    return httpConResult;
                }

                StringBuffer sb = super.getSearchResult(obj);
                String esResults = sb.toString();

                if (esResults.equals(String.valueOf(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()))) {
                    return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
                } else if (esResults.equals(String.valueOf(FssErrorCodeEnum.ES_FILE_NOT_FOUND_EXCEPTION.getCode()))) {
                    return getErrorResult(FssErrorCodeEnum.ES_FILE_NOT_FOUND_EXCEPTION.getCode());
                }

                JSONObject jsonEsResult = JSONObject.parseObject(esResults);

                if (jsonEsResult.get("error") != null) {
                    LOG.info("Query es error!! params:" + obj.toJSONString() + ".\terror:" + jsonEsResult.toJSONString());
                    return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
                }
                if (jsonEsResult.getBoolean("timed_out")) {
                    return getErrorResult(FssErrorCodeEnum.ES_TIMEOUT_EXCEPTION.getCode());
                }

                JSONObject buckets = new JSONObject();
                buckets.put("key", key);
                int count = jsonEsResult.getJSONObject("hits").getInteger("total");
                total += count;
                buckets.put("doc_count", count);
                bucketsArray.add(buckets);
            }
        }

        JSONObject buckets = new JSONObject();
        buckets.put("key", "其它");
        buckets.put("doc_count", 0);
        if(resObj.get("total") != null && (Integer)resObj.get("total") >= total){
            buckets.put("doc_count", ((Integer)resObj.get("total") - total));
        }else{
            LOG.warn("addrGroup count exceeds totalCount: " + resObj.get("total") + ", addrGroup count: " + total);
        }
        bucketsArray.add(buckets);

        bucketsArray.sort(Comparator.comparing(obj -> ((JSONObject) obj).getInteger("doc_count")).reversed());

        JSONObject addrGroup = new JSONObject();
        addrGroup.put("buckets", bucketsArray);
        resObj.put("addr_group", addrGroup);
        return getErrorResult(FssErrorCodeEnum.SUCCESS.getCode());
    }

    private JSONObject getAgeGroupReault(PersonListGroupQueryParam queryParams, JSONObject resObj){
        if(queryParams.getAgeGroup()){
            JSONArray bucketsArray = new JSONArray();

            for(int i = 0; i < 4; i++){
                JSONObject obj = getTemplateParams(queryParams);
                JSONObject params = obj.getJSONObject("params");
                params.put("age_group", true);
                Date nowDate = new Date();
                String key = "";
                if(i == 0){
                    params.put("teenage_end", FormatObject.formatAgeDate(nowDate, 0));
                    params.put("teenage_start", FormatObject.formatAgeDate(nowDate, 15));
                    key = "teenage";
                }else if(i == 1){
                    params.put("youth_end", FormatObject.formatAgeDate(nowDate, 15));
                    params.put("youth_start", FormatObject.formatAgeDate(nowDate, 36));
                    key = "youth";
                }else if(i == 2){
                    params.put("midlife_end", FormatObject.formatAgeDate(nowDate, 36));
                    params.put("midlife_start", FormatObject.formatAgeDate(nowDate, 65));
                    key = "midlife";
                }else if(i == 3){
                    params.put("old_end", FormatObject.formatAgeDate(nowDate, 65));
                    key = "old";
                }
                // HTTP连接
                JSONObject httpConResult = initConnectParams();
                if (httpConResult != null) {
                    return httpConResult;
                }

                StringBuffer sb = super.getSearchResult(obj);
                String esResults = sb.toString();

                if (esResults.equals(String.valueOf(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()))) {
                    return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
                } else if (esResults.equals(String.valueOf(FssErrorCodeEnum.ES_FILE_NOT_FOUND_EXCEPTION.getCode()))) {
                    return getErrorResult(FssErrorCodeEnum.ES_FILE_NOT_FOUND_EXCEPTION.getCode());
                }

                JSONObject jsonEsResult = JSONObject.parseObject(esResults);

                if (jsonEsResult.get("error") != null) {
                    LOG.info("Query es error!! params:" + obj.toJSONString() + ".\terror:" + jsonEsResult.toJSONString());
                    return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
                }
                if (jsonEsResult.getBoolean("timed_out")) {
                    return getErrorResult(FssErrorCodeEnum.ES_TIMEOUT_EXCEPTION.getCode());
                }

                JSONObject buckets = new JSONObject();
                buckets.put("key", key);
                buckets.put("doc_count", jsonEsResult.getJSONObject("hits").getInteger("total"));
                bucketsArray.add(buckets);
            }

            JSONObject ageGroup = new JSONObject();
            ageGroup.put("buckets", bucketsArray);
            resObj.put("age_group", ageGroup);
        }

        return getErrorResult(FssErrorCodeEnum.SUCCESS.getCode());
    }

    private JSONObject getAggsReault(PersonListGroupQueryParam queryParams, JSONObject resObj){
        // HTTP连接
        JSONObject httpConResult = initConnectParams();
        if (httpConResult != null) {
            return httpConResult;
        }

        JSONObject obj = getTemplateParams(queryParams);
        JSONObject params = obj.getJSONObject("params");
        if(queryParams.getSexGroup() || queryParams.getFlagGroup() || queryParams.getTimeGroup()){
            params.put("is_aggs", true);
            params.put("sex_group", queryParams.getSexGroup());
            params.put("flag_group", queryParams.getFlagGroup());
            params.put("time_group", queryParams.getTimeGroup());
        }

        StringBuffer sb = super.getSearchResult(obj);
        String esResults = sb.toString();

        if (esResults.equals(String.valueOf(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()))) {
            return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        } else if (esResults.equals(String.valueOf(FssErrorCodeEnum.ES_FILE_NOT_FOUND_EXCEPTION.getCode()))) {
            return getErrorResult(FssErrorCodeEnum.ES_FILE_NOT_FOUND_EXCEPTION.getCode());
        }

        JSONObject jsonEsResult = JSONObject.parseObject(esResults);

        if (jsonEsResult.get("error") != null) {
            LOG.info("Query es error!! params:" + obj.toJSONString() + ".\terror:" + jsonEsResult.toJSONString());
            return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        }
        if (jsonEsResult.getBoolean("timed_out")) {
            return getErrorResult(FssErrorCodeEnum.ES_TIMEOUT_EXCEPTION.getCode());
        }
        // 从es查询结果中获取hits
        resObj.put("total", jsonEsResult.getJSONObject("hits").getInteger("total"));
        if(jsonEsResult.getJSONObject("aggregations") != null){
            JSONObject aggResult = jsonEsResult.getJSONObject("aggregations");

            if(aggResult.getJSONObject("time_group") != null){
                resObj.put("time_group", aggResult.getJSONObject("time_group"));
            }
            if(aggResult.getJSONObject("sex_group") != null){
                resObj.put("sex_group", aggResult.getJSONObject("sex_group"));
            }
            if(aggResult.getJSONObject("flag_group") != null){
                resObj.put("flag_group", aggResult.getJSONObject("flag_group"));
            }
        }

        return getErrorResult(FssErrorCodeEnum.SUCCESS.getCode());
    }

    private JSONObject getTemplateParams(PersonListGroupQueryParam inParam) {
        JSONObject paramsT = new JSONObject();
        if(inParam.getPersonLibType() != null && !inParam.getPersonLibType().isEmpty()){
            paramsT.put("is_personlib", true);
            paramsT.put("personlib_type", inParam.getPersonLibType());
        }

        if(inParam.getLibId() != null && !inParam.getLibId().isEmpty()){
            paramsT.put("is_lib", true);
            paramsT.put("lib_id", inParam.getLibId());
        }

        paramsT.put("is_del", inParam.getIsDel());
        paramsT.put("from", 0);
        paramsT.put("size", 0);

        JSONObject obj = new JSONObject();
        obj.put("id", templateName);
        obj.put("params", paramsT);

        return obj;
    }
}
