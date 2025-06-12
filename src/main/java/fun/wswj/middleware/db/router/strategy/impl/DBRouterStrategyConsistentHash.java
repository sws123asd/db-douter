package fun.wswj.middleware.db.router.strategy.impl;

import com.google.common.hash.Hashing;
import fun.wswj.middleware.db.router.DBContextHolder;
import fun.wswj.middleware.db.router.DBRouterConfig;
import fun.wswj.middleware.db.router.strategy.IDBRouterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希路由策略
 */
public class DBRouterStrategyConsistentHash implements IDBRouterStrategy {

    private final Logger logger = LoggerFactory.getLogger(DBRouterStrategyConsistentHash.class);

    private SortedMap<Long, PhysicalNode> virtualNodes = new TreeMap<>();
    private final long[] hashRingKeys; // 用于二分查找的哈希键数组
    private final DBRouterConfig dbRouterConfig;
    /**
     * 物理节点类，用于存储数据库和数据表索引
     */
    private static class PhysicalNode {
        final int dbIdx;
        final int tbIdx;

        PhysicalNode(int dbIdx, int tbIdx) {
            this.dbIdx = dbIdx;
            this.tbIdx = tbIdx;
        }
    }

    public DBRouterStrategyConsistentHash(DBRouterConfig dbRouterConfig) {
        this.dbRouterConfig = dbRouterConfig;
        this.virtualNodes = initializeHashRing();
        this.hashRingKeys = virtualNodes.keySet().stream()
                .mapToLong(Long::longValue)
                .toArray();
    }

    private SortedMap<Long, PhysicalNode> initializeHashRing() {
        int dbCount = dbRouterConfig.getDbCount();
        int tbCount = dbRouterConfig.getTbCount();

        int physicalNodeCount = dbRouterConfig.getDbCount() * dbRouterConfig.getTbCount();
        int virtualNodeCount = calculateVirtualNodeCount(physicalNodeCount);

        SortedMap<Long, PhysicalNode> ring = new TreeMap<>();
        for (int dbIdx = 1; dbIdx <= dbCount; dbIdx++) {
            for (int tbIdx = 0; tbIdx < tbCount; tbIdx++) {
                String physicalNodeName = String.format("%02d_%03d", dbIdx, tbIdx);
                PhysicalNode node = new PhysicalNode(dbIdx, tbIdx);

                for (int k = 0; k < virtualNodeCount; k++) {
                    String virtualNodeName = physicalNodeName + "_VN" + k;
                    long hash = consistentHash(virtualNodeName);
                    // 处理极小概率的哈希冲突,添加冲突解决后缀
                    while (ring.containsKey(hash)) {
                        virtualNodeName += "'";
                        hash = consistentHash(virtualNodeName);
                    }
                    ring.put(hash, node);
                }
            }
        }
        logger.info("Hash ring initialized. Physical nodes: {}, Virtual nodes: {}", physicalNodeCount, ring.size());
        // 返回不可变视图（线程安全）
        return Collections.unmodifiableSortedMap(ring);
    }

    private int calculateVirtualNodeCount(int physicalNodeCount) {
        return Math.max(100, (int)(50 * Math.log(physicalNodeCount + 1)));
    }

    @Override
    public void doRouter(String dbKeyAttr) {
        if (virtualNodes.isEmpty()) {
            throw new IllegalStateException("Hash ring not initialized");
        }
        long dbKeyHash = consistentHash(dbKeyAttr);
        // 数组二分查找替代TreeMap.tailMap() 查询性能从 O(log n) 提升至 O(log n) 但常数更低
        PhysicalNode node = findNodeForHash(dbKeyHash);

        // physicalNodeName 期望的格式是 "dbXX_tbYYY"，例如 "01_001"
        DBContextHolder.setDBKey(String.format("%02d", node.dbIdx));
        DBContextHolder.setTBKey(String.format("%03d", node.tbIdx));
        logger.debug("Routing complete. Key: {}, DB: {}, TB: {}", dbKeyAttr, node.dbIdx, node.tbIdx);
    }

    private PhysicalNode findNodeForHash(long dbKeyHash) {
        // 处理环尾情况
        if (dbKeyHash > hashRingKeys[hashRingKeys.length - 1]) {
            return virtualNodes.get(hashRingKeys[0]);
        }

        // 二分查找第一个 >= hash 的位置
        int pos = Arrays.binarySearch(hashRingKeys, dbKeyHash);
        if (pos < 0) {
            pos = -pos - 1; // 转换为插入点
        }

        return virtualNodes.get(hashRingKeys[pos]);
    }

    private long consistentHash(String key) {
        int hash = Hashing.murmur3_32_fixed()
                .hashString(key, StandardCharsets.UTF_8)
                .asInt();
        return hash & 0xFFFFFFFFL;
    }

    @Override
    public void setDBKey(int dbIdx) {
        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    }

    @Override
    public void setTBKey(int tbIdx) {
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    }

    @Override
    public int dbCount() {
        return dbRouterConfig.getDbCount();
    }

    @Override
    public int tbCount() {
        return dbRouterConfig.getTbCount();
    }

    @Override
    public void clear(){
        DBContextHolder.clearDBKey();
        DBContextHolder.clearTBKey();
    }

}
