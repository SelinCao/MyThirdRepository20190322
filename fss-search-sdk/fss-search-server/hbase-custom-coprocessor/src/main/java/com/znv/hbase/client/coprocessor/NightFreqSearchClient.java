package com.znv.hbase.client.coprocessor;


import com.znv.hbase.client.nightFreqSearch.NightFreqSearchParam;
import com.znv.hbase.coprocessor.endpoint.nightFreqSearch.NightFreqFaceCluster;
import com.znv.hbase.coprocessor.endpoint.nightFreqSearch.NightFreqSearchOutData;
import com.znv.hbase.protobuf.generated.NightFreqSearchProtos;
import com.znv.hbase.util.ProtoUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.coprocessor.Batch;
import org.apache.hadoop.hbase.ipc.BlockingRpcCallback;
import org.apache.hadoop.hbase.ipc.ServerRpcController;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by ZNV on 2017/6/8.
 */

public class NightFreqSearchClient {
    protected static final Log LOG = LogFactory.getLog(NightFreqSearchClient.class);
    float threshold = 0.0f;

    public Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> getNightFreq(final Table table, NightFreqSearchParam param, final Scan scan, int analysis) throws Throwable {
        final   NightFreqSearchProtos.NightFreqSearchRequest requestArg = validateArgAndGetPB(param, scan);

        /**
         * StayTimeStatNewCallBack
         */
        class NightFreqSearchCallBack implements Batch.Callback<List<NightFreqSearchProtos.NightFreqMap>> {
            //第一次聚类结果
            Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> resultMap = new ConcurrentHashMap<>();
            //第一次聚类leader信息
            List<NightFreqSearchOutData> list = new CopyOnWriteArrayList<NightFreqSearchOutData>();

            //客户端返回结果
            Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> retMap = new LinkedHashMap<>();

            // 客户端获取查询结果
            public Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> getNightFreqSearch() {
                // 人脸聚类
                Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> clusteringMap = new LinkedHashMap<>();
                NightFreqFaceCluster faceCluster = new NightFreqFaceCluster(threshold);
                //第二次聚类
                if (null != list && list.size() > 0) {
                    clusteringMap = faceCluster.getFaceClusteringResult(list, requestArg.getThreshold());
                }

                //聚类总结果
                Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> totalMap = new LinkedHashMap<>();

                if (null != clusteringMap && clusteringMap.size() > 0) {
                    //两次聚类之间对第二次聚类的每一个组员和第一次聚类的leader根据rowkey寻找相同的组员
                    Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItrCluster = clusteringMap.entrySet()
                            .iterator();

                    while (scnItrCluster.hasNext()) { //第二次聚类的每个组

                        Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> cluterEntry = scnItrCluster.next();
                        List<NightFreqSearchOutData> cluterPersonData = cluterEntry.getValue(); //第二次聚类组员

                        List<NightFreqSearchOutData> totalPerson = new ArrayList<NightFreqSearchOutData>();

                        for (NightFreqSearchOutData data : cluterPersonData) {
                            Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItrResultMap = resultMap.entrySet()
                                    .iterator();
                            while (scnItrResultMap.hasNext()) {
                                Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> mapResultEntry = scnItrResultMap.next();
                                NightFreqSearchOutData mapLeader = mapResultEntry.getKey(); //第一次聚类leader

                                if (0 == Bytes.compareTo(data.getRowKey(), mapLeader.getRowKey())) { //第二次聚类的每个组员找第一次leader
                                    totalPerson.addAll(mapResultEntry.getValue());
                                    break;
                                }
                            }
                            totalMap.put(cluterEntry.getKey(), totalPerson);
                        }
                    }
                }

           /*     //针对每个组员，根据camera_id进行合并，进行驻留时长累加,leader不变
                Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> groupMap = null;
                if (totalMap != null && totalMap.size() > 0) {
                    groupMap = faceCluster.groupByCameraId(totalMap);
                }
*/
                // 驻留时间排序后的结果，得到组员
                Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> sortMap = null;
                if (null != totalMap && totalMap.size() > 0) {
                    sortMap = faceCluster.sortByNightFreqMap(totalMap,analysis);
                }

                // 返回top N 结果
                int size = 0;
                if (sortMap != null && sortMap.size() > 0) {
                    Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItrSortMap = sortMap.entrySet()
                            .iterator();
                    while (scnItrSortMap.hasNext()) {
                        Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItrSortMap.next();
                        retMap.put(entry.getKey(), entry.getValue());
                        size++;
                        if (size >= requestArg.getSize()) {
                            break;
                        }
                    }
                }
                return retMap;
            }

            // 保存查询结果
            @Override
            public void update(byte[] region, byte[] row, List<NightFreqSearchProtos.NightFreqMap> result) {
                if (result != null && result.size() > 0) {
                    for (NightFreqSearchProtos.NightFreqMap out : result) {
                        //leader信息
                        NightFreqSearchOutData leader = new NightFreqSearchOutData();
                        leader.setRowKey(out.getRowKey().toByteArray());
                        leader.setFeature(out.getFeature().toByteArray());
                       // leader.setDurationTime(out.getStayTime());
                        leader.setCameraId(out.getCameraId());
                        leader.setPersonId(out.getPersonId());
                        leader.setFrequency(out.getFrequecy());
                        leader.setImgUrl(out.getImgUrl());
                        list.add(leader);
                        //数据信息
                        List<NightFreqSearchOutData> personList = new ArrayList<NightFreqSearchOutData>();
                        List<NightFreqSearchProtos.NightFreqSearchOut> dataList = out.getDatasList();
                        if (null != dataList && dataList.size() > 0) {
                            for (NightFreqSearchProtos.NightFreqSearchOut data : dataList) {
                                NightFreqSearchOutData person = new NightFreqSearchOutData();
                                person.setRowKey(data.getRowKey().toByteArray());
                                person.setFeature(data.getFeature().toByteArray());
                                person.setDurationTime(data.getStayTime());
                                person.setCameraId(data.getCameraId());
                                person.setCameraName(data.getCameraName());
                                person.setImgUrl(data.getImgUrl());
                                personList.add(person);
                            }
                        }
                        resultMap.put(leader, personList);
                    }
                }
            }
        }
        // 查询结果返回
        NightFreqSearchCallBack callBack = new NightFreqSearchCallBack();
        table.coprocessorService(NightFreqSearchProtos.NightFreqSearchService.class, null, null,
                new Batch.Call<NightFreqSearchProtos.NightFreqSearchService, List<NightFreqSearchProtos.NightFreqMap>>() {
                    @Override
                    public List<NightFreqSearchProtos.NightFreqMap> call(NightFreqSearchProtos.NightFreqSearchService instance) throws IOException {
                        ServerRpcController controller = new ServerRpcController();
                        BlockingRpcCallback<NightFreqSearchProtos.NightFreqSearchResponse> rpcCallback = new BlockingRpcCallback<NightFreqSearchProtos.NightFreqSearchResponse>();
                        instance.getNightFreqSearchResult(controller, requestArg, rpcCallback);
                        NightFreqSearchProtos.NightFreqSearchResponse response = rpcCallback.get();
                        if (controller.failedOnException()) {
                            throw controller.getFailedOn();
                        }
                        return response.getResultsList();
                    }
                }, callBack);
        return callBack.getNightFreqSearch();
    }

    // 解析输入参数
    NightFreqSearchProtos.NightFreqSearchRequest validateArgAndGetPB(NightFreqSearchParam param, final Scan scan) throws IOException {
        if (param == null) {
            throw new IOException("NightFreqSearchClient Exception: Param is Null!");
        }
        if (param.getSize() <= 0 || param.getThreshold() <= 0 || param.getThreshold() > 1) {
            throw new IOException("NightFreqSearchClient Exception: size or threshold is invalid!");
        }
        if (scan == null
                || (Bytes.equals(scan.getStartRow(), scan.getStopRow())
                && !Bytes.equals(scan.getStartRow(), HConstants.EMPTY_START_ROW))
                || ((Bytes.compareTo(scan.getStartRow(), scan.getStopRow()) > 0)
                && !Bytes.equals(scan.getStopRow(), HConstants.EMPTY_END_ROW))) {
            throw new IOException("StayTimeStatNewClient Exception: Startrow should be smaller than Stoprow");
        }
        final   NightFreqSearchProtos.NightFreqSearchRequest.Builder requestBuilder =   NightFreqSearchProtos.NightFreqSearchRequest
                .newBuilder();

        requestBuilder.setScan(ProtoUtil.toScan(scan));
        requestBuilder.setSize(param.getSize());
        requestBuilder.setFrequency(param.getFrequency());
        requestBuilder.setThreshold(param.getThreshold()); // 相似度阈值
        threshold = param.getThreshold();
        if (StringUtils.isBlank(param.getStartTime())) {
            throw new IllegalArgumentException("开始时间不能为空！");
        } else {
            requestBuilder.setStartTime(param.getStartTime());
        }
        if (StringUtils.isBlank(param.getEndTime())) {
            throw new IllegalArgumentException("结束时间不能为空！");
        } else {
            requestBuilder.setEndTime(param.getEndTime());
        }
        return requestBuilder.build();
    }
}
