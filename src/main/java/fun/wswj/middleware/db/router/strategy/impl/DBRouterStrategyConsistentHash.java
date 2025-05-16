package fun.wswj.middleware.db.router.strategy.impl;

import fun.wswj.middleware.db.router.DBContextHolder;
import fun.wswj.middleware.db.router.DBRouterConfig;
import fun.wswj.middleware.db.router.strategy.IDBRouterStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希路由策略
 */
public class DBRouterStrategyConsistentHash implements IDBRouterStrategy {

    private final Logger logger = LoggerFactory.getLogger(DBRouterStrategyConsistentHash.class);

    private static final int VIRTUAL_NODE_COUNT = 512; // 每个物理节点关联的虚拟节点数量
    private final SortedMap<Long, String> virtualNodes = new TreeMap<>(); // 哈希环，用于存储虚拟节点及其映射的物理节点

    private final DBRouterConfig dbRouterConfig;

    public DBRouterStrategyConsistentHash(DBRouterConfig dbRouterConfig) {
        this.dbRouterConfig = dbRouterConfig;
        initializeHashRing();
    }

    private void initializeHashRing() {
        int dbCount = dbRouterConfig.getDbCount();
        int tbCount = dbRouterConfig.getTbCount();

        // 遍历所有数据库和数据表的组合，创建物理节点，并为每个物理节点创建指定数量的虚拟节点
        for (int i = 0; i < dbCount; i++) {
            String dbKey = String.format("%02d", i + 1); // 格式化数据库编号，例如 "01", "02"
            for (int j = 0; j < tbCount; j++) {
                String tbKey = String.format("%03d", j); // 格式化数据表编号，例如 "000", "001"
                String physicalNodeName = dbKey + "_" + tbKey; // 物理节点名称，例如 "01_001"
                // 为每个物理节点创建 VIRTUAL_NODE_COUNT 个虚拟节点
                for (int k = 0; k < VIRTUAL_NODE_COUNT; k++) {
                    String virtualNodeName = physicalNodeName + "_VN" + k; // 虚拟节点名称，例如 "01_001_VN0"
                    long hash = consistentHash(virtualNodeName); // 计算虚拟节点的哈希值
                    virtualNodes.put(hash, physicalNodeName);
                }
            }
        }
        logger.info("一致性哈希环初始化完成，总共 {} 个虚拟节点。数据库数量: {}, 数据表数量: {}", virtualNodes.size(), dbCount, tbCount);
    }

    @Override
    public void doRouter(String dbKeyAttr) {
        if (virtualNodes.isEmpty()) {
            logger.warn("一致性哈希环为空或未初始化。可能导致路由失败或回退到默认路由策略。");
            return;
        }

        long hash = consistentHash(dbKeyAttr);
        
        //  哈希环中所有键大于等于给定 hash 值的条目组成的map集合
        SortedMap<Long, String> tailMap = virtualNodes.tailMap(hash);

        // 如果哈希值大于所有虚拟节点的哈希值，则说明该键应映射到哈希环的第一个节点（顺时针方向）
        // 否则，映射到tailMap中的第一个节点，即顺时针方向最近的虚拟节点
        String physicalNodeName = tailMap.isEmpty() ? virtualNodes.get(virtualNodes.firstKey()) : virtualNodes.get(tailMap.firstKey());

        if (physicalNodeName == null) {
            // 理论上，如果哈希环不为空，这种情况不应该发生
            logger.error("无法为路由键: {} 找到物理节点。哈希环大小: {}", dbKeyAttr, virtualNodes.size());
            throw new RuntimeException("路由键 " + dbKeyAttr + " 失败，未找到物理节点。");
        }

        // physicalNodeName 期望的格式是 "dbXX_tbYYY"，例如 "01_001"
        String[] parts = physicalNodeName.split("_");
        if (parts.length != 2) {
            logger.error("无效的物理节点名称格式: {}。期望格式为 'dbXX_tbYYY'。", physicalNodeName);
            throw new RuntimeException("无效的物理节点名称格式: " + physicalNodeName);
        }

        String dbIdx = parts[0];
        String tbIdx = parts[1];
        DBContextHolder.setDBKey(dbIdx);
        DBContextHolder.setTBKey(tbIdx);
        logger.info("一致性哈希路由决策完成。路由键: {}, 哈希值: {}, 目标物理节点: {}, 解析后库索引: {}, 表索引: {}", dbKeyAttr, Long.toBinaryString(hash), physicalNodeName, dbIdx, tbIdx);
    }

    // FNV1_32_HASH 算法的变种，用于计算字符串的哈希值，结果为 long 类型
    private long consistentHash(String key) {
        final int p = 16777619; // FNV 哈希算法的乘数
        long hash = 2166136261L; // FNV 哈希算法的初始偏移基准
        for (int i = 0; i < key.length(); i++) {
            hash = (hash ^ key.charAt(i)) * p; // 对每个字符进行异或和乘法操作
        }
        // 以下为增强哈希分布性的雪崩效应操作
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        // 确保哈希值为正数，通过与 Long.MAX_VALUE 进行按位与操作
        return hash & 0x7FFFFFFFFFFFFFFFL;
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
