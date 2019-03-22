package com.znv.hbase.coprocessor.endpoint.nightFreqSearch;


import com.znv.fss.common.VConstants;
import com.znv.fss.common.utils.FeatureCompUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

;

/**
 * Created by ZNV on 2017/6/6.
 */
public class NightFreqFaceCluster {
    protected static final Log LOG = LogFactory.getLog(NightFreqFaceCluster.class);
    private final float thresholdmin = 0.89f;
    private final float thresholdmax = 0.92f;
    private float reservalThresholdMin; // 归一化前阈值
    private int count = 0; // 分组编号，从0开始

   /* public FaceCluster() {
        this.reservalThresholdMin = new FeatureCompUtil().reversalNormalize(thresholdmin);
    }*/

    public NightFreqFaceCluster(float threshold) {
        this.reservalThresholdMin = threshold;
    }

    /**
     * 人脸聚类方法
     */
    public Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> getFaceClusteringResult(
            List<NightFreqSearchOutData> searchData, float threshold) {
        // 根据低阈值粗略分组，再进行纠正
        // 不使用传入阈值，使用固定阈值进行聚类，经验值
        Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> finalClusteringMap = new LinkedHashMap<NightFreqSearchOutData, List<NightFreqSearchOutData>>();
        if (null != searchData && searchData.size() > 0) {

            // 按rowkey排序，则每次聚类结果相同
            Collections.sort(searchData, new Comparator<NightFreqSearchOutData>() {
                @Override
                public int compare(NightFreqSearchOutData o1, NightFreqSearchOutData o2) {
                    return Bytes.compareTo(o1.getRowKey(), o2.getRowKey());
                }
            });

            // step1. 根据低阈值粗略分组
            // 其它数据依次和分组里面的数据比对
            Map<Integer, List<NightFreqSearchOutData>> clusteringMap = new LinkedHashMap<Integer, List<NightFreqSearchOutData>>();
            roughClustering(searchData, clusteringMap); //得到的是组号和组号对应的成员

            // step2. 修正分组
            // 纠正分组时采用的经验值与阈值0.89对应
            Map<Integer, NightFreqSearchOutData> clusteringLeader = new LinkedHashMap<Integer, NightFreqSearchOutData>();
            List<NightFreqSearchOutData> wrongClustering = correctClustering(clusteringMap, clusteringLeader);//得到组号和组号对应的leader：clusteringLeader和错误分组wrongClustering

            // step3. 对待重新分组列表重新分组
            reClustering(wrongClustering, clusteringMap);

            // step4. 以每组相似度最高的人为键值返回
            for (Map.Entry<Integer, List<NightFreqSearchOutData>> entry : clusteringMap.entrySet()) {
                NightFreqSearchOutData leader = clusteringLeader.get(entry.getKey());
                List<NightFreqSearchOutData> datalist = entry.getValue();
                if (leader != null) {
                    finalClusteringMap.put(leader, datalist);
                } else if (!datalist.isEmpty()) {
                    finalClusteringMap.put(datalist.get(0), datalist);
                }
            }

        } else {
            LOG.warn("searchData is null!");
        }
        return finalClusteringMap;
    }

    // 根据阈值和personId直接分组
    private void roughClustering(List<NightFreqSearchOutData> searchData, Map<Integer, List<NightFreqSearchOutData>> clusteringMap) {
        boolean isFoundGroup;
        String personId;
        int groupId;
        FeatureCompUtil fc = new FeatureCompUtil();
        Map<String, Integer> groupMap = new HashMap<String, Integer>(); // personId vs groupId

        for (NightFreqSearchOutData data : searchData) {
            if (data.getFeature() == null || data.getFeature().length == 0) {
                LOG.warn("data.getFeature() == null ||data.getFeature().length == 0!");
                continue;
            }
            personId = data.getPersonId();
            if (!personId.equals("0") && groupMap.containsKey(personId)) {
                groupId = groupMap.get(personId);
                data.setGroupId(groupId);
                clusteringMap.get(groupId).add(data);
                continue;
            }
            isFoundGroup = false; // 是否匹配到分组
            if (clusteringMap != null && clusteringMap.size() > 0) {
                Iterator<Map.Entry<Integer, List<NightFreqSearchOutData>>> scnItr = clusteringMap.entrySet().iterator();
                while (scnItr.hasNext()) { // 依次和分组比对
                    Map.Entry<Integer, List<NightFreqSearchOutData>> entry = scnItr.next();
                    List<NightFreqSearchOutData> clusteringList = entry.getValue();
                    if (null != clusteringList && clusteringList.size() > 0) {
                        Iterator<NightFreqSearchOutData> scnItrClustering = clusteringList.iterator();
                        while (scnItrClustering.hasNext()) {
                            NightFreqSearchOutData clusterData = scnItrClustering.next();
                            float sim = fc.Dot(data.getFeature(), clusterData.getFeature(), 12); // 比对相似度
                            //   float sim = fc.Dot(data.getFeature(), clusterData.getFeature(), 0); // 比对相似度 //自研offset 0
                            if (sim >= reservalThresholdMin) { // 相似度超过最小阈值，暂且认为是同一类人脸
                                isFoundGroup = true;
                                break;
                            }
                        }

                        if (isFoundGroup) {
                            if (!personId.equals("0")) {
                                groupMap.put(personId, entry.getKey());
                            }
                            data.setGroupId(entry.getKey());
                            clusteringList.add(data);
                            break;
                        }
                    }
                }
            }
            if ((!isFoundGroup) && (clusteringMap != null)) { // 创建新分组
                if (!personId.equals("0")) {
                    groupMap.put(personId, count);
                }
                List<NightFreqSearchOutData> newClustering = new ArrayList<NightFreqSearchOutData>();
                data.setGroupId(count);
                newClustering.add(data);
                clusteringMap.put(count, newClustering);
                count++;
            }
        }
    }

    // 纠正已分组数据
    private List<NightFreqSearchOutData> correctClustering(Map<Integer, List<NightFreqSearchOutData>> clusteringMap,
                                                           Map<Integer, NightFreqSearchOutData> clusteringLeader) {
        boolean isFoundGroup;
        FeatureCompUtil fc = new FeatureCompUtil();
        List<NightFreqSearchOutData> wrongClustering = new ArrayList<NightFreqSearchOutData>();
        for (Map.Entry<Integer, List<NightFreqSearchOutData>> entry : clusteringMap.entrySet()) {
            List<NightFreqSearchOutData> datalist = entry.getValue();

            int simMinNum = VConstants.FACE_SIMILARITY_NUM;
            // 相似度个数为经验值,不同场景及阈值时需重新调整
            if (datalist.size() > 10) {
                simMinNum = Math.max(datalist.size() / 5, VConstants.FACE_SIMILARITY_NUM);
            } else if (datalist.size() > VConstants.FACE_SIMILARITY_NUM && datalist.size() < 6) {
                simMinNum = 2;
            }

            if (datalist.size() > VConstants.FACE_SIMILARITY_NUM) { // 超过3个人
                int i = 0, j = 0;
                int maxSimCount = 0;
                NightFreqSearchOutData leader = null;
                Iterator it = datalist.iterator();
                while (it.hasNext()) { // 组内遍历
                    isFoundGroup = false;
                    NightFreqSearchOutData data = (NightFreqSearchOutData) it.next();
                    int simcount = data.getSimCount();
                    // 组内每个人与其他人依次比对
                    for (j = i + 1; j < datalist.size(); j++) {
                        NightFreqSearchOutData data2 = datalist.get(j);
                        float sim = fc.Dot(data.getFeature(), data2.getFeature(), 12); // 比对相似度
                        //   float sim = fc.Dot(data.getFeature(), data2.getFeature(), 0); // 比对相似度 //自研 offset 0
                        if (sim >= reservalThresholdMin) { // 相似度超过最大阈值，认为肯定是同一类人脸
                            simcount++;
                            data2.setSimCount(data2.getSimCount() + 1);
                        }
                    }

                    if (simcount >= simMinNum) {
                        // 相似个数超过阈值，认为确定属于该组
                        isFoundGroup = true;
                        data.setSimCount(simcount);
                        if (maxSimCount < simcount) {
                            maxSimCount = simcount;
                            leader = data;
                        }
                        i++;

                    }

                    if (!isFoundGroup) {
                        // 移除出该分组, 加入待重新分组数据列表
                        wrongClustering.add(data);
                        it.remove(); // 移除该对象
                    }
                }
                if (leader != null) {
                    clusteringLeader.put(entry.getKey(), leader);
                }
            }

            if (datalist.size() == 1) { // 纠正个别分组由于人员出现顺序分多组的情况
                wrongClustering.addAll(datalist);
                datalist.clear();
            }
        }
        return wrongClustering;
    }

    // 对分组错误数据重新分组
    private void reClustering(List<NightFreqSearchOutData> searchData, Map<Integer, List<NightFreqSearchOutData>> clusteringMap) {
        boolean isFoundGroup;
        FeatureCompUtil fc = new FeatureCompUtil();
        //对名单库数据进行分组
        for (NightFreqSearchOutData data : searchData) {
            isFoundGroup = false; // 是否匹配到分组
            if (clusteringMap != null && clusteringMap.size() > 0) {
                Iterator<Map.Entry<Integer, List<NightFreqSearchOutData>>> scnItr = clusteringMap.entrySet().iterator();
                while (scnItr.hasNext()) { // 依次和分组比对
                    Map.Entry<Integer, List<NightFreqSearchOutData>> entry = scnItr.next();
                    List<NightFreqSearchOutData> clusteringList = entry.getValue();
                    if (data.getGroupId() == entry.getKey()) { // 修正时跳过本组数据
                        continue;
                    }
                    if (null != clusteringList && clusteringList.size() > 0) {
                        Iterator<NightFreqSearchOutData> scnItrClustering = clusteringList.iterator();
                        while (scnItrClustering.hasNext()) {
                            NightFreqSearchOutData clusterData = scnItrClustering.next();
                            float sim = fc.Dot(data.getFeature(), clusterData.getFeature(), 12); // 比对相似度
                            //   float sim = fc.Dot(data.getFeature(), clusterData.getFeature(), 0); // 比对相似度 //自研offset 0
                            if (sim >= reservalThresholdMin) { // 相似度超过最小阈值，暂且认为是同一类人脸
                                isFoundGroup = true;
                                break;
                            }
                        }

                        if (isFoundGroup) {
                            data.setGroupId(entry.getKey());
                            clusteringList.add(data);
                            break;
                        }
                    }
                }
            }
            if (!isFoundGroup) { // 创建新分组
                List<NightFreqSearchOutData> newClustering = new ArrayList<NightFreqSearchOutData>();
                data.setGroupId(count);
                newClustering.add(data);
                clusteringMap.put(count, newClustering);
                count++;
            }
        }
    }

    /**
     * 根据驻留时间排序获取前top N 条数据
     */
    public List<NightFreqSearchOutData> sortByStayTime(
            Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> clusteringMap) {
        if (null == clusteringMap || clusteringMap.size() == 0) {
            LOG.warn("clusteringMap is null!");
            return null;
        }
        Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItr = clusteringMap.entrySet()
                .iterator();
        List<NightFreqSearchOutData> outDataList = null;
        // 排序后的数据值保留总的驻留时长和rowkey
        List<NightFreqSearchOutData> sortDataList = new ArrayList<NightFreqSearchOutData>();

        while (scnItr.hasNext()) {
            Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItr.next();
            outDataList = entry.getValue();
            long stayTime = 0L;
            if (null != outDataList && outDataList.size() > 0) {
                for (NightFreqSearchOutData data : outDataList) {
                    stayTime += data.getDurationTime();
                }
                NightFreqSearchOutData leader = entry.getKey();
                leader.setDurationTime(stayTime);
                sortDataList.add(leader);
            }
        }
        // 按驻留时间排序
        Collections.sort(sortDataList, new Comparator<NightFreqSearchOutData>() {
            @Override
            public int compare(NightFreqSearchOutData o1, NightFreqSearchOutData o2) {
                return Long.compare(o2.getDurationTime(), o1.getDurationTime());
            }
        });
        return sortDataList;
    }

    /**
     * 根据同行次数排序获取前top N 条数据
     */
    public List<NightFreqSearchOutData> sortByPeerCount(
            Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> clusteringMap) {
        if (null == clusteringMap || clusteringMap.size() == 0) {
            LOG.warn("clusteringMap is null!");
            return null;
        }
        Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItr = clusteringMap.entrySet()
                .iterator();
        List<NightFreqSearchOutData> outDataList;
        // 排序后的数据值保留总的驻留时长和rowkey
        List<NightFreqSearchOutData> sortDataList = new ArrayList<NightFreqSearchOutData>();

        while (scnItr.hasNext()) {
            Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItr.next();
            outDataList = entry.getValue();
            if (null != outDataList && outDataList.size() > 0) {
                NightFreqSearchOutData leader = entry.getKey();
                //leader.setCount(outDataList.size());
                sortDataList.add(leader);
            }
        }
        // 按驻留时间排序
       Collections.sort(sortDataList, new Comparator<NightFreqSearchOutData>() {
            @Override
            public int compare(NightFreqSearchOutData o1, NightFreqSearchOutData o2) {
                return Integer.compare(o2.getFrequency(), o1.getFrequency());
            }
        });
        return sortDataList;
    }

    /**
     * 根据驻留时间排序获取前top N 条数据,并获取组员
     */

    public Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> sortByNightFreqMap(
            Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> clusteringMap, int analysis) {
        Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> sortMap = new LinkedHashMap<>();

        if (null == clusteringMap || clusteringMap.size() == 0) {
            LOG.warn("clusteringMap is null!");
            return sortMap;
        }
        Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItr = clusteringMap.entrySet()
                .iterator();
        List<NightFreqSearchOutData> outDataList = null;
        // 排序后的数据值保留总的驻留时长和rowkey
        List<NightFreqSearchOutData> sortDataList = new ArrayList<NightFreqSearchOutData>();

        while (scnItr.hasNext()) {
            Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItr.next();
            outDataList = entry.getValue();

           int frequency = outDataList.size();
            if (null != outDataList && outDataList.size() > 0) {
                NightFreqSearchOutData leader = entry.getKey();
                  if(analysis==1){
                      Set<String> cameraSet = new HashSet();
                      for (NightFreqSearchOutData data : outDataList) {
                          cameraSet.add(data.getCameraId());
                     }
                      frequency = cameraSet.size();
                      leader.setFrequency(frequency);
                      sortDataList.add(leader);
                      entry.getKey().setFrequency(frequency);
                   }else{
                      leader.setFrequency(frequency);
                      sortDataList.add(leader);
                      entry.getKey().setFrequency(frequency);
                  }
            }

        }

        // 按驻留时间排序
        Collections.sort(sortDataList, new Comparator<NightFreqSearchOutData>() {
            @Override
            public int compare(NightFreqSearchOutData o1, NightFreqSearchOutData o2) {
                return Long.compare(o2.getFrequency(), o1.getFrequency());
            }
        });
        for (NightFreqSearchOutData tmp : sortDataList) {
            sortMap.put(tmp, clusteringMap.get(tmp));
        }
        return sortMap;
    }

    /**
     * 对组员根据cameraId进行合并
     */
    public Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> groupByCameraId(Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> dataMap) {
        Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> retDataMap = new LinkedHashMap<>();

        int testCount = 0;
        int retCount = 0;
        if (null != dataMap && dataMap.size() > 0) {
            Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItrDataMap = dataMap.entrySet()
                    .iterator();

            while (scnItrDataMap.hasNext()) {
                List<NightFreqSearchOutData> personListNew = new ArrayList<>(); //leader对应的所有数据
                testCount++;
                Map<String, List<NightFreqSearchOutData>> groupData = new LinkedHashMap<>(); //leader对应的所有数据分类之后的结果

                Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entryDataMap = scnItrDataMap.next();
                List<NightFreqSearchOutData> personList = entryDataMap.getValue(); //leader对应的所有数据

                NightFreqSearchOutData leader = entryDataMap.getKey();

                //数据按摄像头分类
                for (NightFreqSearchOutData person : personList) {
                    if (groupData.containsKey(person.getCameraId())) {
                        groupData.get(person.getCameraId()).add(person);
                    } else {
                        List<NightFreqSearchOutData> list = new ArrayList<NightFreqSearchOutData>();
                        list.add(person);
                        groupData.put(person.getCameraId(), list);
                    }
                }

                Iterator<Map.Entry<String, List<NightFreqSearchOutData>>> scnItrGroupData = groupData.entrySet()
                        .iterator();

                //针对leader数据计算在每个摄像头下的时长
                while (scnItrGroupData.hasNext()) {

                    Map.Entry<String, List<NightFreqSearchOutData>> entryGroupData = scnItrGroupData.next();
                    List<NightFreqSearchOutData> list = entryGroupData.getValue();
                    long stayTime = 0;

                    for (NightFreqSearchOutData person : list) {
                        stayTime += person.getDurationTime();
                    }
                    //组员取大众脸
                    Collections.sort(list, new Comparator<NightFreqSearchOutData>() {
                        @Override
                        public int compare(NightFreqSearchOutData o1, NightFreqSearchOutData o2) {
                            return Integer.compare(o2.getSimCount(), o1.getSimCount());
                        }
                    });

                    NightFreqSearchOutData dataNew = list.get(0);
                    NightFreqSearchOutData data = new NightFreqSearchOutData();

                    data.setCameraId(dataNew.getCameraId());
                    data.setCameraName(dataNew.getCameraName());
                    data.setDurationTime(stayTime);
                    //data.setFeature(dataNew.getFeature());
                   // data.setCount(dataNew.getCount());
                    data.setGroupId(dataNew.getGroupId());
                    data.setPersonId(dataNew.getPersonId());
                    data.setRowKey(dataNew.getRowKey());
                    data.setSimCount(dataNew.getSimCount());
                    //data.setLibId(dataNew.getLibId());
                    data.setImgUrl(dataNew.getImgUrl());

                    byte[] feature = new byte[1];
                    feature[0] = 0x00; //组员的feature不需要
                    data.setFeature(feature);
                    personListNew.add(data);
                }

                if (personListNew != null && personListNew.size() > 0) {
                    retDataMap.put(leader, personListNew);
                }
            }
        }
        return retDataMap;
    }

}
