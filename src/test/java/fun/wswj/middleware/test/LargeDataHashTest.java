package fun.wswj.middleware.test;

import fun.wswj.middleware.db.router.DBContextHolder;
import fun.wswj.middleware.db.router.DBRouterConfig;
import fun.wswj.middleware.db.router.strategy.IDBRouterStrategy;
import fun.wswj.middleware.db.router.strategy.impl.DBRouterStrategyConsistentHash;
import fun.wswj.middleware.db.router.strategy.impl.DBRouterStrategyHashCode;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LargeDataHashTest {

    private IDBRouterStrategy dbRouterStrategy;
    private IDBRouterStrategy normalHashStrategy;
    private final int DATA_COUNT = 1000000; // 100万条数据
    private final String DATA_FILE = "user_ids.txt";
    
    /**
     * 测试完成后清理ThreadLocal数据
     */
    private void clearThreadLocal() {
        if (dbRouterStrategy != null) dbRouterStrategy.clear();
        if (normalHashStrategy != null) normalHashStrategy.clear();
    }

    @Before
    public void init() {
        // 初始化一致性哈希配置
        DBRouterConfig dbRouterConfig = new DBRouterConfig();
        dbRouterConfig.setDbCount(4);
        dbRouterConfig.setTbCount(8);
        dbRouterConfig.setRouterKey("userId");
        dbRouterConfig.setRouterType("consistentHash");

        // 创建一致性哈希路由策略
        dbRouterStrategy = new DBRouterStrategyConsistentHash(dbRouterConfig);
        
        // 初始化普通哈希配置
        DBRouterConfig normalConfig = new DBRouterConfig();
        normalConfig.setDbCount(4);
        normalConfig.setTbCount(8);
        normalConfig.setRouterKey("userId");
        normalConfig.setRouterType("hashCode");
        
        // 创建普通哈希路由策略
        normalHashStrategy = new DBRouterStrategyHashCode(normalConfig);
        
        // 确保初始化时ThreadLocal是干净的
        clearThreadLocal();
    }

    @Test
    public void test_consistentHashDistribution() {
        dbRouterStrategy.doRouter("sws123asd");
        dbRouterStrategy.clear();
    }
    /**
     * 生成测试数据文件
     */
    @Test
    public void generateTestData() throws IOException {
        // 创建文件
        File file = new File(DATA_FILE);
        if (file.exists()) {
            System.out.println("文件已存在，将被覆盖");
        }

        // 写入数据
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < DATA_COUNT; i++) {
                // 生成随机用户ID
//                String userId = RandomStringUtils.randomNumeric(15);
                String userId = generateRandomString(4);
                writer.write(userId);
                writer.newLine();

                if (i % 10000 == 0) {
                    System.out.println("已生成 " + i + " 条数据");
                }
            }
        }

        System.out.println("测试数据生成完成，保存至：" + file.getAbsolutePath());
    }
    private String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
            sb.append(System.currentTimeMillis());
        }
        return sb.toString();
    }
    

    /**
     * 测试一致性哈希算法在大数据量下的分布情况
     */
    @Test
    public void testConsistentHashDistribution() throws IOException {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                System.out.println("测试数据文件不存在，请先运行 generateTestData() 方法生成测试数据");
                return;
            }
            
            // 记录路由结果
            Map<String, Integer> dbCountMap = new HashMap<>();
            Map<String, Integer> tbCountMap = new HashMap<>();
            
            System.out.println("开始测试一致性哈希算法在" + DATA_COUNT + "条数据下的分布情况...");
            
            int count = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String userId;
                while ((userId = reader.readLine()) != null) {
                    // 执行路由
                    dbRouterStrategy.doRouter(userId);
                    
                    // 获取路由结果
                    String dbKey = DBContextHolder.getDBKey();
                    String tbKey = DBContextHolder.getTBKey();
                    
                    // 统计分布
                    dbCountMap.put(dbKey, dbCountMap.getOrDefault(dbKey, 0) + 1);
                    tbCountMap.put(tbKey, tbCountMap.getOrDefault(tbKey, 0) + 1);
                    
                    // 清除路由
                    dbRouterStrategy.clear();
                    
                    count++;

                }
            }
            
            // 打印分布统计
            System.out.println("----------------------------------------------------");
            System.out.println("总数据量: " + count);
            System.out.println("----------------------------------------------------");
            System.out.println("库分布统计：");
            for (Map.Entry<String, Integer> entry : dbCountMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("库 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
            
            System.out.println("----------------------------------------------------");
            System.out.println("表分布统计：");
            for (Map.Entry<String, Integer> entry : tbCountMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("表 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
        } finally {
            // 确保清理ThreadLocal数据
            clearThreadLocal();
        }
    }
    
    /**
     * 测试普通哈希算法在大数据量下的分布情况
     */
    @Test
    public void testNormalHashDistribution() throws IOException {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                System.out.println("测试数据文件不存在，请先运行 generateTestData() 方法生成测试数据");
                return;
            }
            
            // 记录路由结果
            Map<String, Integer> dbCountMap = new HashMap<>();
            Map<String, Integer> tbCountMap = new HashMap<>();
            
            System.out.println("开始测试普通哈希算法在" + DATA_COUNT + "条数据下的分布情况...");
            
            int count = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String userId;
                while ((userId = reader.readLine()) != null) {
                    // 执行路由
                    normalHashStrategy.doRouter(userId);
                    
                    // 获取路由结果
                    String dbKey = DBContextHolder.getDBKey();
                    String tbKey = DBContextHolder.getTBKey();
                    
                    // 统计分布
                    dbCountMap.put(dbKey, dbCountMap.getOrDefault(dbKey, 0) + 1);
                    tbCountMap.put(tbKey, tbCountMap.getOrDefault(tbKey, 0) + 1);
                    
                    // 清除路由
                    normalHashStrategy.clear();
                    
                    count++;
                }
            }
            
            // 打印分布统计
            System.out.println("----------------------------------------------------");
            System.out.println("总数据量: " + count);
            System.out.println("----------------------------------------------------");
            System.out.println("库分布统计：");
            for (Map.Entry<String, Integer> entry : dbCountMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("库 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
            
            System.out.println("----------------------------------------------------");
            System.out.println("表分布统计：");
            for (Map.Entry<String, Integer> entry : tbCountMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("表 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
        } finally {
            // 确保清理ThreadLocal数据
            clearThreadLocal();
        }
    }
    
    /**
     * 对比测试：普通哈希算法与一致性哈希算法在大数据量下的分布情况
     */
    @Test
    public void compareHashAlgorithms() throws IOException {
        try {
            File file = new File(DATA_FILE);
            if (!file.exists()) {
                System.out.println("测试数据文件不存在，请先运行 generateTestData() 方法生成测试数据");
                return;
            }
            
            // 使用已初始化的普通哈希路由策略
            
            // 记录路由结果
            Map<String, Integer> consistentHashDbMap = new HashMap<>();
            Map<String, Integer> consistentHashTbMap = new HashMap<>();
            Map<String, Integer> normalHashDbMap = new HashMap<>();
            Map<String, Integer> normalHashTbMap = new HashMap<>();
            
            System.out.println("开始对比普通哈希算法与一致性哈希算法在" + DATA_COUNT + "条数据下的分布情况...");
            
            int count = 0;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String userId;
                while ((userId = reader.readLine()) != null) {
                    // 测试一致性哈希
                    dbRouterStrategy.doRouter(userId);
                    String consistentDbKey = DBContextHolder.getDBKey();
                    String consistentTbKey = DBContextHolder.getTBKey();
                    consistentHashDbMap.put(consistentDbKey, consistentHashDbMap.getOrDefault(consistentDbKey, 0) + 1);
                    consistentHashTbMap.put(consistentTbKey, consistentHashTbMap.getOrDefault(consistentTbKey, 0) + 1);
                    clearThreadLocal();
                    
                    // 测试普通哈希
                    normalHashStrategy.doRouter(userId);
                    String normalDbKey = DBContextHolder.getDBKey();
                    String normalTbKey = DBContextHolder.getTBKey();
                    normalHashDbMap.put(normalDbKey, normalHashDbMap.getOrDefault(normalDbKey, 0) + 1);
                    normalHashTbMap.put(normalTbKey, normalHashTbMap.getOrDefault(normalTbKey, 0) + 1);
                    clearThreadLocal();
                    
                    count++;
                    if (count % 10000 == 0) {
                        System.out.println("已处理 " + count + " 条数据");
                    }
                }
            }
            
            // 打印对比结果
            System.out.println("\n=====================================================");
            System.out.println("总数据量: " + count);
            System.out.println("=====================================================");
            
            System.out.println("\n一致性哈希算法分布情况：");
            System.out.println("----------------------------------------------------");
            System.out.println("库分布统计：");
            for (Map.Entry<String, Integer> entry : consistentHashDbMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("库 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
            
            System.out.println("----------------------------------------------------");
            System.out.println("表分布统计：");
            for (Map.Entry<String, Integer> entry : consistentHashTbMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("表 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
            
            System.out.println("\n普通哈希算法分布情况：");
            System.out.println("----------------------------------------------------");
            System.out.println("库分布统计：");
            for (Map.Entry<String, Integer> entry : normalHashDbMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("库 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
            
            System.out.println("----------------------------------------------------");
            System.out.println("表分布统计：");
            for (Map.Entry<String, Integer> entry : normalHashTbMap.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / count;
                System.out.println("表 " + entry.getKey() + ": " + entry.getValue() + " 条数据，占比: " + String.format("%.2f%%", percentage));
            }
        } finally {
            // 确保清理ThreadLocal数据
            clearThreadLocal();
        }
    }
}