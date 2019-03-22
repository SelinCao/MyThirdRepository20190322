package com.znv.fss.es.AbnormalBehaviorSearch;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.es.BaseEsSearch;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.znv.fss.es.FormatObject.formatTime;


public class AbnormalBehaviorSearch extends BaseEsSearch {
    protected static final Logger LOGGER = LogManager.getLogger(AbnormalBehaviorSearch.class);

    private String ipAndPort;
    private String templateName;
    private String indeNameAndType;
    private String scrollTime = "2m";

    public AbnormalBehaviorSearch(String ipAndPort, String tempalteName, String indeNameAndType) {
        this.ipAndPort = ipAndPort;
        this.templateName = tempalteName;
        this.indeNameAndType = indeNameAndType;
    }

    public String concatenateURL(boolean flag) {
        String esurl = null;
        //根据flag拼接不同的url，true为第一次的scroll查询，false为后续的scroll查询
        if (flag) {
            esurl = ipAndPort + "/" + indeNameAndType + "/_search/template?scroll=" + scrollTime;
        } else {
            esurl = ipAndPort + "/_search/scroll";
        }

        return esurl;
    }

    public JSONObject initConnectParams(String esurl) {
        JSONObject httpCon = super.httpConnection.esHttpConnect(esurl);
        return httpCon;
    }

    private int paramCheck(AbnBehaviorQueryParam inParam) {
        if (StringUtils.isEmpty(inParam.getEnterTimeStart())) {
            LOGGER.info("StartTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (StringUtils.isEmpty(inParam.getEnterTimeEnd())) {
            LOGGER.info("EndTime can't be empty ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }
        if (inParam.getEnterTimeStart().compareTo(inParam.getEnterTimeEnd()) > 0) {
            LOGGER.error("EndTime can't larger than startTime ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }

        if (inParam.getSize() < 1) {
            LOGGER.info("Size is too small ！");
            return FssErrorCodeEnum.ES_INVALID_PARAM.getCode();
        }

        return FssErrorCodeEnum.SUCCESS.getCode();
    }

    private JSONObject getTemplateParams(AbnBehaviorQueryParam inParam) {
        JSONObject paramsT = new JSONObject();
        paramsT.put("enter_time_start", inParam.getEnterTimeStart());
        paramsT.put("enter_time_end", inParam.getEnterTimeEnd());

        paramsT.put("from", 0);
        paramsT.put("size", 1000);

        paramsT.put("sortField", inParam.getSortField());
        paramsT.put("sortOrder", inParam.getSortOrder());

        if (inParam.getLibId() != null && !inParam.getLibId().isEmpty()) {
            paramsT.put("lib_id", inParam.getLibId());
            paramsT.put("is_lib", true);
        }

        String[] excludes = {"rt_feature"};
        paramsT.put("excludes", excludes);
        paramsT.put("is_excludes", true);

        JSONObject obj = new JSONObject();
        obj.put("id", templateName);
        obj.put("params", paramsT);

        return obj;
    }

    public JSONObject getESResult(JSONObject templateParams, boolean flag) {
        JSONObject httpConResult = initConnectParams(concatenateURL(flag));
        if (httpConResult != null) {
            return httpConResult;
        }

        StringBuffer sb = super.getSearchResult(templateParams);

        if (sb.toString().equals(String.valueOf(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode()))) {
            return getErrorResult(FssErrorCodeEnum.ES_GET_EXCEPTION.getCode());
        }

        String esResults = sb.toString();
        return JSONObject.parseObject(esResults);
    }

    @Override
    public JSONObject requestSearch(String inparams) {
        long timeStart = System.currentTimeMillis();
        AbnBehaviorJsonIn inputParams = JSON.parseObject(inparams, AbnBehaviorJsonIn.class);
        AbnBehaviorQueryParam queryParams = inputParams.getParams();
        //检查参数
        int errCode = paramCheck(queryParams);
        if (errCode != FssErrorCodeEnum.SUCCESS.getCode()) {
            return getErrorResult(errCode);
        }

        //指定的区域id
        List<String> officeIds = queryParams.getOfficeId();
        //满足条件的hits
        List<AbnBehaviorQueryHit> outHits = new ArrayList<>();

        JSONObject templateParams = getTemplateParams(queryParams);
        JSONObject jsonEsResult = getESResult(templateParams, true);

        String scorll_id = jsonEsResult.getString("_scroll_id");
        JSONArray esHits = jsonEsResult.getJSONObject("hits").getJSONArray("hits");

        JSONObject scrollJson = new JSONObject();
        scrollJson.put("scroll", scrollTime);

        while (!esHits.isEmpty()) {
            //遍历一次scroll查询出来的结果，
            for (int i = 0; i < esHits.size(); i++) {
                JSONObject source = esHits.getJSONObject(i).getJSONObject("_source");
                String office_id = source.getString("office_id");

                //判断是否在指定区域内
                if (office_id != null && !officeIds.contains(office_id)) {
                    AbnBehaviorQueryHit hit = JSON.parseObject(source.toString(), AbnBehaviorQueryHit.class);
                    outHits.add(hit);
                }
            }
            //下一批的scroll请求处理
            scrollJson.put("scroll_id", scorll_id);
            //获取下一批scroll数据
            JSONObject nextResult = getESResult(scrollJson, false);
            scorll_id = nextResult.getString("_scroll_id");
            esHits = nextResult.getJSONObject("hits").getJSONArray("hits");
        }

        int total = outHits.size();

        List<AbnBehaviorQueryHit> subList = new ArrayList<>();
        int from = queryParams.getFrom();
        int size = queryParams.getSize();

        if (from < total) {
            int toindex = Math.min(from + size, total);
            subList = outHits.subList(from, toindex);
        }

        //格式化时间输出
        for (int i = 0; i < subList.size(); i++) {
            AbnBehaviorQueryHit queryHit = subList.get(i);

            queryHit.setEnterTime(formatTime(queryHit.getEnterTime()));
            queryHit.setLeaveTime(formatTime(queryHit.getLeaveTime()));
            queryHit.setOpTime(formatTime(queryHit.getOpTime()));
        }

        AbnBehaviorJsonOut outputResult = new AbnBehaviorJsonOut();
        outputResult.setErrorcode(FssErrorCodeEnum.SUCCESS.getCode());
        outputResult.setTotal(total);
        outputResult.setHits(subList);
        outputResult.setTook(System.currentTimeMillis() - timeStart);

        return (JSONObject) JSONObject.toJSON(outputResult);
    }
}
