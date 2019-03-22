package com.znv.hbase.coprocessor.endpoint;

import com.google.protobuf.ByteString;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.Service;
import com.znv.fss.common.VConstants;
import com.znv.hbase.coprocessor.endpoint.nightFreqSearch.NightFreqFaceCluster;
import com.znv.hbase.coprocessor.endpoint.nightFreqSearch.NightFreqSearchOutData;
import com.znv.hbase.protobuf.generated.NightFreqSearchProtos;
import com.znv.hbase.util.PhoenixConvertUtil;
import com.znv.hbase.util.ProtoUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.Coprocessor;
import org.apache.hadoop.hbase.CoprocessorEnvironment;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.CoprocessorException;
import org.apache.hadoop.hbase.coprocessor.CoprocessorService;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.InternalScanner;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ZNV on 2017/11/6.
 */
public class NightFreqSearchImplementation
        extends NightFreqSearchProtos.NightFreqSearchService implements CoprocessorService, Coprocessor {
    protected static final Log LOG = LogFactory.getLog(NightFreqSearchImplementation.class);
    private RegionCoprocessorEnvironment env;
    private static String featureFamily = "FEATURE"; // 图片特征值列族
    private static String featureColumn = "RT_FEATURE"; // 特征值列
    private static String attrFamily = "ATTR"; // 除特征值和图片之外的列族 摄像头ID,局站ID列
    private static String durTimeColumn = "DURATION_TIME"; //驻留时长
    private static String cameraName = "CAMERA_NAME";
    private static String imgUrl = "IMG_URL";
    private static String personId = "PERSON_ID";
    private static byte[] phoenixGapbs = new byte[1];

    @Override
    public void getNightFreqSearchResult(RpcController controller, NightFreqSearchProtos.NightFreqSearchRequest request,
                                         RpcCallback<NightFreqSearchProtos.NightFreqSearchResponse> done) {
        NightFreqSearchProtos.NightFreqSearchResponse response = null;
        List<NightFreqSearchProtos.NightFreqMap> out = new ArrayList<>();

        int size = request.getSize();
      //  int frequency = request.getFrequency();
        float threshold = request.getThreshold();
        String startTime = request.getStartTime();
        String endTime = request.getEndTime();
        //String analysis = request.getAnalysis();
        // 按查询条件过滤数据
        List<NightFreqSearchOutData> searchData = getFilterData(request, startTime, endTime);
        // 人脸聚类
        Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> clusteringMap = null;
        NightFreqFaceCluster faceCluster = new NightFreqFaceCluster(threshold);
        if (null != searchData && searchData.size() > 0) {
            clusteringMap = faceCluster.getFaceClusteringResult(searchData, threshold);
        }
        //针对每个组员，根据camera_id进行合并，进行驻留时长累加,leader不变
       /* Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> groupMap = null;
        if (null != clusteringMap && clusteringMap.size() > 0) {
            groupMap = faceCluster.groupByCameraId(clusteringMap);
        }
*/
        // 修改leader的频次，sortMap为leader按照频次排序后的结果
        Map<NightFreqSearchOutData, List<NightFreqSearchOutData>> sortMap = null;
        if (null != clusteringMap && clusteringMap.size() > 0) {
            sortMap = faceCluster.sortByNightFreqMap(clusteringMap,0);//0是指服务端不按摄像头去重
        }

        // 解析数据到NightFreqSearchProtos.NightFreqMap,返回top N 条数据
        if (null != sortMap && sortMap.size() > 0) {
            Iterator<Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>>> scnItr = sortMap.entrySet()
                    .iterator();
            List<NightFreqSearchOutData> outDataList = null;
            int count = 0;
            while (scnItr.hasNext()) {
                Map.Entry<NightFreqSearchOutData, List<NightFreqSearchOutData>> entry = scnItr.next();
                outDataList = entry.getValue();

                NightFreqSearchProtos.NightFreqMap.Builder mapBuilder =  NightFreqSearchProtos.NightFreqMap.newBuilder();

                //设置leader相关信息
                mapBuilder.setFrequecy(entry.getKey().getFrequency());
               // mapBuilder.setStayTime(entry.getKey().getDurationTime());
                mapBuilder.setRowKey(ByteString.copyFrom(entry.getKey().getRowKey()));
                mapBuilder.setFeature(ByteString.copyFrom(entry.getKey().getFeature()));
                mapBuilder.setCameraId(entry.getKey().getCameraId());
                mapBuilder.setPersonId(entry.getKey().getPersonId());
                mapBuilder.setImgUrl(entry.getKey().getImgUrl());
                //设置组员相关信息
                for (NightFreqSearchOutData temp : outDataList) {
                    NightFreqSearchProtos.NightFreqSearchOut.Builder builder = NightFreqSearchProtos.NightFreqSearchOut.newBuilder();
                    builder.setStayTime(temp.getDurationTime());
                    builder.setRowKey(ByteString.copyFrom(temp.getRowKey()));
                    builder.setFeature(ByteString.copyFrom(temp.getFeature()));
                    builder.setCameraId(temp.getCameraId());
                    builder.setCameraName(temp.getCameraName());
                    builder.setImgUrl(temp.getImgUrl());
                 //   builder.setUuid(temp.getUuid);
                    mapBuilder.addDatas(builder);
                }
                out.add(mapBuilder.build());
                count++;
                if (count >= size) {
                    break;
                }
            }
        }
        // 查询结果返回给客户端
        if (out.size() > 0) {
            NightFreqSearchProtos.NightFreqSearchResponse.Builder responseBuilder = NightFreqSearchProtos.NightFreqSearchResponse
                    .newBuilder();
            responseBuilder.addAllResults(out);
            response = responseBuilder.build();
        }
        done.run(response);
    }

    /**
     * 根据过滤条件获取数据rowkey:(enter_time, uuid)
     */
    private List<NightFreqSearchOutData> getFilterData(NightFreqSearchProtos.NightFreqSearchRequest request, String startTime,
                                                       String endTime) {

        byte[] saltedKey = getSaltedKey();
        List<NightFreqSearchOutData> retList = new ArrayList<NightFreqSearchOutData>();
        phoenixGapbs[0] = (byte) 0xff; //时间降序，为xff; 升序则为x00
        if (saltedKey != null && saltedKey.length > 0) {
            InternalScanner scanner = null;

            try {
                Scan s = ProtoUtil.toScan(request.getScan()); // 过滤条件
                s.addColumn(Bytes.toBytes(featureFamily), Bytes.toBytes(featureColumn)); // 特征值
                //客户端已添加attrFamily列族
                s.addColumn(Bytes.toBytes(attrFamily), Bytes.toBytes(durTimeColumn)); // 驻留时长
                s.addColumn(Bytes.toBytes(attrFamily), Bytes.toBytes(cameraName));
                s.addColumn(Bytes.toBytes(attrFamily), Bytes.toBytes(imgUrl));
                s.addColumn(Bytes.toBytes(attrFamily), Bytes.toBytes(personId));

                byte[] starttimeConvt = Bytes.add(PhoenixConvertUtil.convertDescField(Bytes.toBytes(endTime)),
                        phoenixGapbs);
                byte[] stoptimeConvt = Bytes.add(PhoenixConvertUtil.convertDescField(Bytes.toBytes(startTime)),
                        phoenixGapbs);
                // 包含开始时间和结束时间
                byte[] startRowkey = Bytes.add(saltedKey, starttimeConvt);
                byte[] stopRowkey = Bytes.add(saltedKey, stoptimeConvt, phoenixGapbs);

                s.setStartRow(startRowkey);
                s.setStopRow(stopRowkey);

                List<Cell> results = new ArrayList<Cell>();

                boolean hasMoreRows = false;
                scanner = env.getRegion().getScanner(s);
                do {
                    hasMoreRows = scanner.next(results);
                    if (results.size() > 0) {
                        Cell res = results.get(0);
                        byte[] rowKey = res.getRow();
                        NightFreqSearchOutData outData = new NightFreqSearchOutData();
                        outData.setRowKey(rowKey);
                        outData.setFrequency(1);
                        for (Cell cell : results) {
                            String col = Bytes.toString(CellUtil.cloneQualifier(cell));
                            switch (col) {
                                case "RT_FEATURE":
                                    outData.setFeature(CellUtil.cloneValue(cell));
                                    break;
                                case "DURATION_TIME":
                                    long durationTime = (Bytes.toLong(CellUtil.cloneValue(cell)));
                                    outData.setDurationTime(durationTime);
                                    break;
                                case "CAMERA_ID":
                                    outData.setCameraId(Bytes.toString(CellUtil.cloneValue(cell)));
                                    break;
                                case "CAMERA_NAME":
                                    outData.setCameraName(Bytes.toString(CellUtil.cloneValue(cell)));
                                    break;
                                case "IMG_URL":
                                    outData.setImgUrl(Bytes.toString(CellUtil.cloneValue(cell)));
                                    break;
                                case "PERSON_ID":
                                    outData.setPersonId(Bytes.toString(CellUtil.cloneValue(cell)));
                                    break;
                                default:
                                    break;
                            }
                        }
                        retList.add(outData);
                    }
                    results.clear();
                } while (hasMoreRows);
            } catch (IOException e) {
                LOG.info(e);
            } catch (Exception e) {
                LOG.info(e);
            } finally {
                if (scanner != null) {
                    try {
                        scanner.close();
                    } catch (IOException e) {
                        LOG.info(e);
                    }
                }
            }
        }
        return retList;
    }

    private byte[] getSaltedKey() {
        byte[] saltedkey = new byte[VConstants.PHOENIX_ROWKEY_SALTING_LENGTH];
        byte[] regionStartKey = env.getRegion().getRegionInfo().getStartKey(); // 获取region的startkey
        if (regionStartKey.length == 0) {
            saltedkey[0] = VConstants.STAY_TIME_EMPTY_REGION_START_KEY;
        } else {
            System.arraycopy(regionStartKey, 0, saltedkey, 0, VConstants.PHOENIX_ROWKEY_SALTING_LENGTH);
        }
        return saltedkey;
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        if (env instanceof RegionCoprocessorEnvironment) {
            this.env = (RegionCoprocessorEnvironment) env;
        } else {
            throw new CoprocessorException("Must be loaded on a table region!");
        }
    }

    @Override
    public void stop(CoprocessorEnvironment env) throws IOException {
        // nothing to do
    }

    @Override
    public Service getService() {
        return this;
    }
}
