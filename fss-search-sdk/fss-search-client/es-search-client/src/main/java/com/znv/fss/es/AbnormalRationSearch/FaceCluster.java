package com.znv.fss.es.AbnormalRationSearch;


import com.znv.fss.common.VConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.znv.fss.common.utils.FeatureCompUtil;


import java.util.*;

/**
 * Created by tjy on 2017/12/29.
 */
public class FaceCluster {
    protected static final Logger LOGGER = LogManager.getLogger(FaceCluster.class);
    private final float thresholdmin = 0.89f;
    private final float thresholdmax = 0.92f;
    private float reservalThesholdMin = new FeatureCompUtil().reversalNormalize(0.89f);
    private float reservalThesholdMax = new FeatureCompUtil().reversalNormalize(0.92f);
    private int count = 0;


    public Map<AbnormalRationSearchHit, List<AbnormalRationSearchHit>> getFaceClusteringResult(List<AbnormalRationSearchHit> searchData, float threshold) {
        Map<AbnormalRationSearchHit, List<AbnormalRationSearchHit>> finalClusteringMap = new LinkedHashMap<>();
        if (null != searchData && searchData.size() > 0) {
            //todo 要先对同行人列表排序吗
            Map<Integer, List<AbnormalRationSearchHit>> clusteringMap = new LinkedHashMap();
            //粗略分组
            roughClustering(searchData, clusteringMap);

            //错误分组
            Map<Integer, AbnormalRationSearchHit> clusteringLeader = new LinkedHashMap();
            List<AbnormalRationSearchHit> wrongClustering = correctClustering(clusteringMap, clusteringLeader);
            //重新分组
            reClustering(wrongClustering, clusteringMap);
            // step4. 以每组相似度最高的人为键值返回
            for (Map.Entry<Integer, List<AbnormalRationSearchHit>> entry : clusteringMap.entrySet()) {
                AbnormalRationSearchHit leader = clusteringLeader.get(entry.getKey());
                List<AbnormalRationSearchHit> datalist = entry.getValue();
                if (leader != null) {
                    finalClusteringMap.put(leader, datalist);
                } else if (!datalist.isEmpty()) {
                    finalClusteringMap.put(datalist.get(0), datalist);
                }
            }

        } else {
            LOGGER.warn("searchData is null!");
        }
        return finalClusteringMap;
    }

    //排序
    // 根据阈值和personId直接分组
    private void roughClustering(List<AbnormalRationSearchHit> searchData, Map<Integer, List<AbnormalRationSearchHit>> clusteringMap) {
        boolean isFoundGroup;
        String personId;
        int groupId;
        FeatureCompUtil fc = new FeatureCompUtil();
        Map<String, Integer> groupMap = new HashMap<String, Integer>(); // personId vs groupId

        for (AbnormalRationSearchHit data : searchData) {
            if (data.getFeature() == null || data.getFeature().length == 0) {
                LOGGER.warn("data.getFeature() == null");
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
                Iterator<Map.Entry<Integer, List<AbnormalRationSearchHit>>> scnItr = clusteringMap.entrySet().iterator();
                while (scnItr.hasNext()) { // 依次和分组比对
                    Map.Entry<Integer, List<AbnormalRationSearchHit>> entry = scnItr.next();
                    List<AbnormalRationSearchHit> clusteringList = entry.getValue();
                    if (null != clusteringList && clusteringList.size() > 0) {
                        Iterator<AbnormalRationSearchHit> scnItrClustering = clusteringList.iterator();
                        while (scnItrClustering.hasNext()) {
                            AbnormalRationSearchHit clusterData = scnItrClustering.next();
                            float sim = fc.Dot(data.getFeature(), clusterData.getFeature(), 12); // 比对相似度
                            //   float sim = fc.Dot(data.getFeature(), clusterData.getFeature(), 0); // 比对相似度 //自研offset 0
                            if (sim >= reservalThesholdMin) { // 相似度超过最小阈值，暂且认为是同一类人脸
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
                List<AbnormalRationSearchHit> newClustering = new ArrayList<>();
                data.setGroupId(count);
                newClustering.add(data);
                clusteringMap.put(count, newClustering);
                count++;
            }
        }
    }

    // 纠正已分组数据
    private List<AbnormalRationSearchHit> correctClustering(Map<Integer, List<AbnormalRationSearchHit>> clusteringMap,
                                                            Map<Integer, AbnormalRationSearchHit> clusteringLeader) {
        boolean isFoundGroup;
        FeatureCompUtil fc = new FeatureCompUtil();
        List<AbnormalRationSearchHit> wrongClustering = new ArrayList<>();
        for (Map.Entry<Integer, List<AbnormalRationSearchHit>> entry : clusteringMap.entrySet()) {
            List<AbnormalRationSearchHit> datalist = entry.getValue();

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
                AbnormalRationSearchHit leader = null;
                Iterator it = datalist.iterator();
                while (it.hasNext()) { // 组内遍历
                    isFoundGroup = false;
                    AbnormalRationSearchHit data = (AbnormalRationSearchHit) it.next();
                    int simcount = data.getSimCount();
                    // 组内每个人与其他人依次比对
                    for (j = i + 1; j < datalist.size(); j++) {
                        AbnormalRationSearchHit data2 = datalist.get(j);
                        float sim = fc.Dot(data.getFeature(), data2.getFeature(), 12); //
                        // 比对相似度
                        //   float sim = fc.Dot(data.getFeature(), data2.getFeature(), 0); // 比对相似度 //自研 offset 0
                        if (sim >= reservalThesholdMin) { // 相似度超过最大阈值，认为肯定是同一类人脸
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
    private void reClustering(List<AbnormalRationSearchHit> searchData, Map<Integer, List<AbnormalRationSearchHit>> clusteringMap) {
        boolean isFoundGroup;
        FeatureCompUtil fc = new FeatureCompUtil();
        //对名单库数据进行分组
        for (AbnormalRationSearchHit data : searchData) {
            isFoundGroup = false; // 是否匹配到分组
            if (clusteringMap != null && clusteringMap.size() > 0) {
                Iterator<Map.Entry<Integer, List<AbnormalRationSearchHit>>> scnItr = clusteringMap.entrySet().iterator();
                while (scnItr.hasNext()) { // 依次和分组比对
                    Map.Entry<Integer, List<AbnormalRationSearchHit>> entry = scnItr.next();
                    List<AbnormalRationSearchHit> clusteringList = entry.getValue();
                    if (data.getGroupId() == entry.getKey()) { // 修正时跳过本组数据
                        continue;
                    }
                    if (null != clusteringList && clusteringList.size() > 0) {
                        Iterator<AbnormalRationSearchHit> scnItrClustering = clusteringList.iterator();
                        while (scnItrClustering.hasNext()) {
                            AbnormalRationSearchHit clusterData = scnItrClustering.next();
                            float sim = fc.Dot(data.getFeature(), clusterData.getFeature(),
                                    12); // 比对相似度
                            //   float sim = fc.Dot(data.getFeature(), clusterData.getFeature(), 0); // 比对相似度 //自研offset 0
                            if (sim >= reservalThesholdMax) { // 相似度超过最小阈值，暂且认为是同一类人脸
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
                List<AbnormalRationSearchHit> newClustering = new ArrayList<>();
                data.setGroupId(count);
                newClustering.add(data);
                clusteringMap.put(count, newClustering);
                count++;
            }
        }
    }


}
