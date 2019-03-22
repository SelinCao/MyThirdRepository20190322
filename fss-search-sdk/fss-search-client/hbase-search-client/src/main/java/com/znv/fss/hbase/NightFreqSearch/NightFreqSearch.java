package com.znv.fss.hbase.NightFreqSearch;

import com.alibaba.fastjson.JSON;
import com.znv.fss.common.VConstants;
import com.znv.fss.common.errorcode.FssErrorCodeEnum;
import com.znv.fss.common.utils.FeatureCompUtil;
import com.znv.fss.hbase.HBaseConfig;
import com.znv.fss.hbase.HBaseManager;
import com.znv.fss.hbase.JsonResultType;
import com.znv.fss.hbase.MultiHBaseSearch;
import com.znv.hbase.client.coprocessor.NightFreqSearchClient;
import com.znv.hbase.client.nightFreqSearch.NightFreqSearchParam;
import com.znv.hbase.coprocessor.endpoint.nightFreqSearch.NightFreqSearchOutData;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.master.procedure.ServerProcedureInterface;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by ZNV on 2017/6/7.
 */
public class NightFreqSearch extends MultiHBaseSearch {
    private final String schemaName = HBaseConfig.getProperty(VConstants.FSS_PHOENIX_SCHEMA_NAME);
    private final String historyTableName = HBaseConfig.getProperty(VConstants.FSS_HISTORY_V113_TABLE_NAME);
    private String tableName = schemaName + ":" + historyTableName;
    private int size = 10;
    private int maxSize = 100;
    private int frequency = 5;
    float simThreshold = 0.89F;
    private int analysis = 0; //0:频繁夜出,1,同天多次入住,默认为0
    private String officeIds[] = null;
    private String cameraIds[] = null;
    private String startTime = null;
    private String endTime = null;
    private static String attrFamily = "ATTR";
    private static String officeIdColumn = "OFFICE_ID";
    private static String cameraIdColumn = "CAMERA_ID";
    private List<String> exceptionList = new ArrayList<String>();
    private static byte[] phoenixGapChar = new byte[1];
    private static Log log = LogFactory.getLog(NightFreqSearch.class);
    private FeatureCompUtil fc = new FeatureCompUtil();

    public NightFreqSearch() {
        super("search");
    }

    /**
     * 获取查询结果
     */
    @Override
    public String requestSearch(String jsonParamStr) throws Exception {
        fc.setFeaturePoints(HBaseConfig.getFeaturePoints()); // [lq-add]
        long t1 = System.currentTimeMillis();
        try {
            exceptionList.clear();
            parseJsonParams(jsonParamStr);
        } catch (Exception e) {
            exceptionList.add(FssErrorCodeEnum.ES_INVALID_PARAM.getExplanation());
            return getJsonStr(t1, null);
        }
        this.phoenixGapChar[0] = (byte) 0xff;

        Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> NightFreqMap = null;
        NightFreqMap = getStatResult(analysis);
        if (exceptionList.size() > 0) {
            return getJsonStr(t1, new ArrayList<NightAnalyses>());
        }

        try {

             //按摄像头对组员分类
            List<NightAnalyses> analysesList = new ArrayList<>();
            if (NightFreqMap != null && NightFreqMap.size() > 0) {
                  //  analysesList = getAnalysisData(NightFreqMap);
                analysesList =getDataFromHbase(NightFreqMap);
            }
                //返回结果信息
            return getJsonStr(t1,analysesList);
             //   return getJsonStr(t1, NightFreqOut, analysesList);
            } catch (Exception e) {
            log.info(e);
            exceptionList.add("Error");
            return getJsonStr(t1, new ArrayList<NightAnalyses>());
        }
    }

    /**
     * 解析请求参数
     */
    private void parseJsonParams(String jsonParamStr) throws Exception {
        NightFreqJsonInput inputParam = JSON.parseObject(jsonParamStr, NightFreqJsonInput.class);
        NightFreqReportServiceIn service = inputParam.getReportService();
        HBaseManager.SearchId id = HBaseManager.SearchId.getSearchId(service.getId());
        String type = service.getType();
        if (id.equals(HBaseManager.SearchId.NightFreqSearch) && type.equals("request")) { // 12012
            size = service.getSize();
            frequency = service.getFrequency();
            if (size <= 0) {
                size = 10;
            }
            if (size > 100) {
                size = 100;
            }
            if (service.getAnalysis() != 0) {
                analysis = service.getAnalysis();
            }
            NightFreqInput NightFreqParam = service.getNightFreq();
            validateArg(NightFreqParam);
            try {
                int hbaseThreshold = NightFreqParam.getSimThreshold();
               // int hbaseThreshold = 89;
                simThreshold = hbaseThreshold / 100.00f;
                simThreshold = fc.reversalNormalize(simThreshold /*/ 100.00f*/); // [lq-modify]协处理器传入反归一化阈值
                officeIds = NightFreqParam.getOfficeIds();
                cameraIds = NightFreqParam.getCameraIds(); // 获取查询条件
                startTime = NightFreqParam.getStartTime();
                endTime = NightFreqParam.getEndTime();
            } catch (Exception e) {
                throw new Exception();
            }
        }
    }

    private Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> getStatResult(int analysis) {
        Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> statData = new LinkedHashMap<NightFreqSearchOutData, List<NightFreqSearchOutData>>();
        Scan newScan = new Scan();
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE);
        if (officeIds != null && officeIds.length > 0) {
            for (String officeId : officeIds) {
                if (officeId != null && !officeId.equals("")) {
                    BinaryComparator comp = new BinaryComparator(Bytes.toBytes(officeId));
                    SingleColumnValueFilter filter = new SingleColumnValueFilter(Bytes.toBytes(attrFamily),
                            Bytes.toBytes(officeIdColumn), CompareFilter.CompareOp.EQUAL, comp);
                    filter.setFilterIfMissing(true);
                    filterList.addFilter(filter);
                }
            }
        }
        if (cameraIds != null && cameraIds.length > 0) {
            for (String cameraId : cameraIds) {
                if (cameraId != null && !cameraId.equals("")) {
                    BinaryComparator comp = new BinaryComparator(Bytes.toBytes(cameraId));
                    SingleColumnValueFilter filter = new SingleColumnValueFilter(Bytes.toBytes(attrFamily),
                            Bytes.toBytes(cameraIdColumn), CompareFilter.CompareOp.EQUAL, comp);
                    filter.setFilterIfMissing(true);
                    filterList.addFilter(filter);
                }
            }
        }

        newScan.setFilter(filterList);
        //newScan.addFamily(Bytes.toBytes(attrFamily));
        //必须在客户端添加需要过滤的条件列
        newScan.addColumn(Bytes.toBytes(attrFamily), Bytes.toBytes(officeIdColumn));
        newScan.addColumn(Bytes.toBytes(attrFamily), Bytes.toBytes(cameraIdColumn));
        newScan.setMaxVersions(1);
        newScan.setCacheBlocks(true);

        NightFreqSearchParam searchParam = new NightFreqSearchParam();
        searchParam.setSize(maxSize);
        searchParam.setStartTime(startTime);
        searchParam.setEndTime(endTime);
        searchParam.setThreshold(simThreshold);
        searchParam.setFrequency(frequency);
      //  searchParam.setAnalysis(analysis);

        HTable table = null;
        try {
            table = HBaseConfig.getTable(tableName);
            NightFreqSearchClient client = new NightFreqSearchClient();
            statData = client.getNightFreq(table, searchParam, newScan,analysis); // 协处理器中已排序
        } catch (Throwable e) {
            exceptionList.add("Exception");
            log.error(e);
         //   e.printStackTrace();
        } finally {
            try {
                if (table != null) {
                    table.close();
                }
            } catch (IOException e) {
                exceptionList.add("IOException");
                log.error(e);
               // e.printStackTrace();
            }
        }
        return statData;
    }

    /**
     * 获取结果，rowKey(alarm_type, sub_type, enter_time, uuid)
     */
    private  List<NightAnalyses> getDataFromHbase(Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> NightFreqMap) {
        int count = 0;
        List<NightAnalyses> analysesList = new ArrayList<NightAnalyses>();

        if (null != NightFreqMap && NightFreqMap.size() > 0) {
            Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItr = NightFreqMap.entrySet()
                    .iterator();
            HTable table = null;
            try {
                table = HBaseConfig.getTable(tableName);
                while (scnItr.hasNext()) {
                    Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItr.next();
                    if(entry.getKey().getFrequency()>=frequency) {
                        if (null != entry && entry.getValue() != null && entry.getValue().size() > 0) {
                            NightAnalyses analyses = new NightAnalyses();
                            List<NightFreqSearchOutData> personList = entry.getValue();
                            //  analyses.setRowKey(entry.getKey().getRowKey());

                            analyses.setFrequency(entry.getKey().getFrequency());
                            analyses.setImg_url(entry.getKey().getImgUrl());
                            String rowkeyinfo1[] = getRowKeyInfos(entry.getKey().getRowKey()); // 盐值占一个字节*/
                            if (rowkeyinfo1 != null && rowkeyinfo1.length >= 2) {
                                analyses.setEnter_time(rowkeyinfo1[0]);
                                analyses.setUuid(rowkeyinfo1[1]);
                            }

                            if (personList != null && personList.size() > 0) {
                                List<Get> listGets = new ArrayList<Get>();
                                for (NightFreqSearchOutData tmp : personList) {
                                    Get get = new Get(tmp.getRowKey());
                                    listGets.add(get);
                                }
                                Result[] rs = table.get(listGets);
                                List<NightAnalysisData> analysisDataList = new ArrayList<NightAnalysisData>();
                                for (Result r : rs) {
                                    NightAnalysisData analysisData = new NightAnalysisData();
                                    Cell[] cells = r.rawCells();
                                    byte[] rowKey = r.getRow();
                                    // analysisData.setRowKey(rowKey);
                                    String rowkeyinfo[] = getRowKeyInfos(rowKey); // 盐值占一个字节*/
                                    if (rowkeyinfo != null && rowkeyinfo.length >= 2) {
                                        analysisData.setEnter_time(rowkeyinfo[0]);
                                        analysisData.setUuid(rowkeyinfo[1]);
                                    }
                                    int len = cells.length;
                                    for (int i = 0; i < len; i++) {
                                        Cell cell = cells[i];
                                        String col = Bytes.toString(CellUtil.cloneQualifier(cell));
                                        String value = Bytes.toString(CellUtil.cloneValue(cell));
                                        switch (col) {
                                            case "CAMERA_ID":
                                                analysisData.setCamera_id(value);
                                                break;
                                            case "CAMERA_NAME":
                                                analysisData.setCamera_name(value);
                                                break;
                                            case "GPSX":
                                                float gpsx = 0f;
                                                byte[] bytegpsx = (CellUtil.cloneValue(cell));
                                                if (bytegpsx != null && bytegpsx.length > 0) {
                                                    gpsx = Float.intBitsToFloat(Bytes.toInt(bytegpsx) ^ 0x80000001);
                                                }
                                                analysisData.setGpsx(gpsx);
                                                break;
                                            case "GPSY":
                                                float gpsy = 0f;
                                                byte[] bytegpsy = (CellUtil.cloneValue(cell));
                                                if (bytegpsy != null && bytegpsy.length > 0) {
                                                    gpsy = Float.intBitsToFloat(Bytes.toInt(bytegpsy) ^ 0x80000001);
                                                }
                                                analysisData.setGpsy(gpsy);
                                                break;
                                            case "OFFICE_ID":
                                                analysisData.setOffice_id(value);
                                                break;
                                            case "OFFICE_NAME":
                                                analysisData.setOffice_name(value);
                                                break;
                                            case "PERSON_ID":
                                                analysisData.setPerson_id(value);
                                                break;
                                            case "BIG_PICTURE_UUID":
                                                analysisData.setBig_picture_uuid(value);
                                                break;
                                            case "IMG_URL":
                                                analysisData.setImg_url(value);
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                    analysisDataList.add(analysisData);
                                }
                                Collections.sort(analysisDataList, new Comparator<NightAnalysisData>() {
                                    @Override
                                    public int compare(NightAnalysisData o1, NightAnalysisData o2) {
                                        SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        long t1=0;
                                        long t2=0;
                                        try {
                                            t1 = sdf2.parse(o2.getEnter_time()).getTime();
                                            t2 = sdf2.parse(o1.getEnter_time()).getTime();
                                        } catch (ParseException e) {
                                            exceptionList.add("Exception");
                                            log.error(e);
                                        }
                                          return Long.compare(t1 ,t2);
                                    }
                                });
                                analyses.setAnalysisData(analysisDataList);
                            }
                            analysesList.add(analyses);
                            count++;
                        }
                        if (count == size) {
                            break;
                        }
                    }else{
                        break;
                    }
                }
            } catch (Exception e) {
                exceptionList.add("Exception");
                log.error(e);
                // e.printStackTrace();
            } catch (Error e) {
                exceptionList.add("Error");
                log.error(e);
                // e.printStackTrace();
            } finally {
                try {
                    if (table != null) {
                        table.close();
                    }
                } catch (IOException e) {
                    exceptionList.add("IOException");
                    log.error(e);
                    // e.printStackTrace();
                }
            }
        }
        return analysesList;
    }


    /**
     * 获取json串
     */
      /*  private String getJsonStr(long t1, List<NightFreqOut> tempList) {
        String jsonstr = "";
        // 捕获到异常，直接返回失败
        if (exceptionList.size() > 0) {
            NightFreqReportServiceOut serviceOut = new NightFreqReportServiceOut();
            if (exceptionList.contains("Error")) {
                serviceOut.setErrorCode(JsonResultType.ERROR.toString());
            } else if (exceptionList.contains(FssErrorCodeEnum.ES_INVALID_PARAM.getExplanation())) {
                serviceOut.setErrorCode(FssErrorCodeEnum.ES_INVALID_PARAM.getExplanation());
            } else {
                serviceOut.setErrorCode(JsonResultType.TIMEOUT.toString());
            }
            NightFreqJsonOut outData = new NightFreqJsonOut();
            outData.setReportService(serviceOut);
            Object jsonObject = JSON.toJSON(outData);
            jsonstr = JSON.toJSONString(jsonObject);
            return jsonstr;
        } else {
            long t2 = System.currentTimeMillis() - t1;
            NightFreqReportServiceOut serviceOut = new NightFreqReportServiceOut();
            if (tempList != null && !tempList.isEmpty()) {
                NightFreqOut[] res = new NightFreqOut[tempList.size()];
                serviceOut.setNightFreq(tempList.toArray(res));
                serviceOut.setCount(tempList.size());
            } else {
                serviceOut.setCount(0);
            }
            serviceOut.setId(HBaseManager.SearchId.NightFreqSearch.getId());
            serviceOut.setType("response");

            serviceOut.setTime(String.valueOf(t2));
            serviceOut.setErrorCode(JsonResultType.SUCCESS.toString());

            NightFreqJsonOut outData = new NightFreqJsonOut();
            outData.setReportService(serviceOut);

            Object jsonObject = JSON.toJSON(outData);
            jsonstr = JSON.toJSONString(jsonObject);
            return jsonstr;
        }
    }*/

    private static String convSpanToStr(long spanMs) {
        String rtn = "";
        int secUnit = 1000;
        int minUnit = 60 * secUnit;
        int hourUnit = 60 * minUnit;
        int dayUnit = 24 * hourUnit;
        long dayNum = 0L;
        long hourNum = 0L;
        long minNum = 0L;
        long secNum = 0L;
        long temp = spanMs;
        if (temp > dayUnit) {
            dayNum = temp / dayUnit;
            temp = temp - dayUnit * dayNum;
        }
        if (temp > hourUnit) {
            hourNum = temp / hourUnit;
            temp = temp - hourUnit * hourNum;
        }
        if (temp > minUnit) {
            minNum = temp / minUnit;
            temp = temp - minUnit * minNum;
        }
        secNum = temp / secUnit;
        if (dayNum != 0) {
            rtn += String.format("%s天", dayNum);
        }
        if (hourNum != 0) {
            rtn += String.format("%s小时", hourNum);
        }
        if (minNum != 0) {
            rtn += String.format("%s分", minNum);
        }
        if (secNum >= 0) {
            rtn += String.format("%s秒", secNum);
        }
        return rtn;
    }

    private List<NightFreqSearchOutData> getLeaderData(Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> dataListMap) {
        List<NightFreqSearchOutData> leader = new ArrayList<NightFreqSearchOutData>();
        int count = 0;
        if (null != dataListMap && dataListMap.size() > 0) {
            Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItr = dataListMap.entrySet()
                    .iterator();

            while (scnItr.hasNext()) {
                Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItr.next();
                if (null != entry) {
                    leader.add(entry.getKey());
                    count++;
                }
                if (count == size) {
                    break;
                }
            }
        }
        return leader;
    }

    /**
     * 按摄像头对组员分类
     */
    /*private List<NightAnalyses> getAnalysisData(Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> dataListMap) {

        List<NightAnalyses> retDatas = new ArrayList<NightAnalyses>();
        Map<String, NightAnalyses> analysesMap = new LinkedHashMap<>();
        if (dataListMap != null && dataListMap.size() > 0) {
            Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItr = dataListMap.entrySet()
                    .iterator();
            int count = 0;
            while (scnItr.hasNext()) { //对每组循环
                Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItr.next();
                if (null != entry && entry.getValue() != null && entry.getValue().size() > 0) {
                    List<NightFreqSearchOutData> personList = entry.getValue(); //每组组员
                    for (NightFreqSearchOutData personData : personList) { //对每组的组员循环
                        NightAnalysisData analysisData = new NightAnalysisData();
                        String rowkeyinfo[] = getRowKeyInfos(personData.getRowKey());
                        if (rowkeyinfo != null && rowkeyinfo.length >= 2) {
                            analysisData.setEnter_time(rowkeyinfo[0]);
                            analysisData.setUuid(rowkeyinfo[1]);
                        }
                        analysisData.setDuration_time(convSpanToStr(personData.getDurationTime() * 1000));
                        analysisData.setDuration_timel(personData.getDurationTime());
                        analysisData.setImg_url(personData.getImgUrl());

                        if (analysesMap.containsKey(personData.getCameraId())) {
                            analysesMap.get(personData.getCameraId()).getAnalysisData().add(analysisData);
                        } else {
                            NightAnalyses analysesNew = new NightAnalyses();
                            analysesNew.setCamera_id(personData.getCameraId());
                            analysesNew.setCamera_name(personData.getCameraName());

                            List<NightAnalysisData> analysisDataList = new ArrayList<NightAnalysisData>();
                            analysisDataList.add(analysisData);

                            analysesNew.setAnalysisData(analysisDataList);
                            analysesMap.put(personData.getCameraId(), analysesNew);
                        }
                    }
                    count++;
                }
                if (count == size) {
                    break;
                }
            }
        }

        Iterator<Map.Entry<String, NightAnalyses>> retItr = analysesMap.entrySet().iterator();
        while (null != retItr && retItr.hasNext()) {
            Map.Entry<String, NightAnalyses> retEntry = retItr.next();
            NightAnalyses analyses = retEntry.getValue();
            List<NightAnalysisData> analysisDataList = analyses.getAnalysisData();
            // 按驻留时间排序
            Collections.sort(analysisDataList, new Comparator<NightAnalysisData>() {
                @Override
                public int compare(NightAnalysisData o1, NightAnalysisData o2) {
                    return Long.compare(o2.getDuration_timel(), o1.getDuration_timel());
                }
            });
            retDatas.add(analyses);
        }
        return retDatas;
    }*/

    /**
     * 获取json串
     */
    private String getJsonStr(long t1,/* List<NightFreqOut> tempList,*/ List<NightAnalyses > analysesList) {
        String jsonstr = "";
        // 捕获到异常，直接返回失败
        if (exceptionList.size() > 0) {
            NightFreqReportServiceOut serviceOut = new NightFreqReportServiceOut();
            if (exceptionList.contains("Error")) {
                serviceOut.setErrorCode(JsonResultType.ERROR.toString());
            } else if (exceptionList.contains(FssErrorCodeEnum.ES_INVALID_PARAM.getExplanation())) {
                serviceOut.setErrorCode(FssErrorCodeEnum.ES_INVALID_PARAM.getExplanation());
            } else {
                serviceOut.setErrorCode(JsonResultType.TIMEOUT.toString());
            }
            NightFreqJsonOut outData = new NightFreqJsonOut();
            outData.setReportService(serviceOut);
            Object jsonObject = JSON.toJSON(outData);
            jsonstr = JSON.toJSONString(jsonObject);
            return jsonstr;
        } else {
            long t2 = System.currentTimeMillis() - t1;
            NightFreqReportServiceOut serviceOut = new NightFreqReportServiceOut();
            if (analysesList != null && analysesList.size() > 0) {
                NightAnalyses[] res = new NightAnalyses[analysesList.size()];
                serviceOut.setCount(analysesList.size());
                serviceOut.setNightAnalyses(analysesList.toArray(res));
            }
          /*  if (tempList != null && !tempList.isEmpty()) {
                NightFreqOut[] res = new NightFreqOut[tempList.size()];
                serviceOut.setNightFreq(tempList.toArray(res));
                serviceOut.setCount(tempList.size());

                if (analysesList != null && analysesList.size() > 0) {
                    NightAnalyses[] res2 = new NightAnalyses[analysesList.size()];
                    serviceOut.setNightAnalyses(analysesList.toArray(res2));
                }
            }*/ else {
                serviceOut.setCount(0);
            }
            serviceOut.setId(HBaseManager.SearchId.NightFreqSearch.getId());
            serviceOut.setType("response");

            serviceOut.setTime(String.valueOf(t2));
            serviceOut.setErrorCode(JsonResultType.SUCCESS.toString());

            NightFreqJsonOut outData = new NightFreqJsonOut();
            outData.setReportService(serviceOut);

            Object jsonObject = JSON.toJSON(outData);
            jsonstr = JSON.toJSONString(jsonObject);
            return jsonstr;
        }
    }

    private List<NightFreqOut> getNightFreqOutData(List<NightFreqSearchOutData> leaderData, Map<String, NightFreqData> searchData) {
        List<NightFreqOut> outData = new ArrayList<NightFreqOut>();
        int count = 0; //处理要考虑每个组员多算的问题
        for (NightFreqSearchOutData leader : leaderData) {
            String rowkeyinfo[] = getRowKeyInfos(leader.getRowKey()); // 盐值占一个字节*/
            String uuid = "";
            if (rowkeyinfo != null && rowkeyinfo.length >= 2) {
                uuid = rowkeyinfo[1];
            }

            if (searchData.containsKey(uuid)) {

                NightFreqData search = searchData.get(uuid);
                NightFreqOut info = new NightFreqOut();

                info.setPerson_id(search.getPerson_id());
                //设置合并之后的聚合时间
              /*  long stayTime = leader.getDurationTime();
                info.setDuration_time(convSpanToStr(stayTime * 1000));*/
                info.setEnter_time(search.getEnter_time());
                info.setUuid(search.getUuid());
                info.setImg_url(search.getImg_url());
                info.setFrequency(search.getFrequency());
                info.setBig_picture_uuid(search.getBig_picture_uuid());
               /* info.setOffice_id("");
                info.setOffice_name("");
                info.setCamera_id("");
                info.setCamera_name("");
               *//* info.setCamera_id(search.getCamera_id());
                info.setCamera_name(search.getCamera_name());*//*


                if (officeIds != null && officeIds.length == 1) { //局站单选，设置局站相关
                    info.setOffice_id(search.getOffice_id());
                    info.setOffice_name(search.getOffice_name());
                }
                if (null != cameraIds && cameraIds.length == 1) {
                    info.setOffice_id(search.getOffice_id());
                    info.setOffice_name(search.getOffice_name());
                    info.setCamera_id(search.getCamera_id());
                    info.setCamera_name(search.getCamera_name());
                }*/

                outData.add(info);
                count++;
                if (count >= size) {
                    break;
                }
            }
        }
        return outData;
    }

    private String[] getRowKeyInfos(byte[] rowkey) {
        String rowkeyinfo[] = new String[2];
        try {
            String phoenixGapCharStr = Bytes.toString(phoenixGapChar);
            byte[] newRowKey = Bytes.copy(rowkey);
            for (int i = 1; i < 20; i++) {
                newRowKey[i] = (byte) (~(int) newRowKey[i]);
            }
            rowkeyinfo = Bytes.toString(newRowKey, 1).split(phoenixGapCharStr); // 盐值占一个字节*/
        } catch (Exception e) {
            log.info(e);
        }
        return rowkeyinfo;
    }

    private void validateArg(NightFreqInput param) throws IOException {
        if (param == null) {
            throw new IOException("RelationshipParam Exception: Param is null");
        } else {
            if (StringUtils.isEmpty(param.getStartTime())) {
                throw new IllegalArgumentException("StartTime can't be empty！");
            }
            if (StringUtils.isEmpty(param.getEndTime())) {
                throw new IllegalArgumentException("EndTime can't be empty！");
            }
         /*   String timeDelta = null;
            try {
                timeDelta = DateTimeFun.getTimeDelta(param.getStartTime(), param.getEndTime());
            } catch (ParseException e) {
                log.error(e);
                throw new IllegalArgumentException("Time format is wrong！");
            }
            if (timeDelta != null && Float.parseFloat(timeDelta) > 1 * 24 * 60) {
                throw new IllegalArgumentException("The range of time can't exceed 1 days！");
            }*/
        }
    }

}
