package fun.wswj.middleware.test;

import fun.wswj.middleware.db.router.annotation.DBRouter;
import com.alibaba.fastjson.JSON;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ApiTest {

    public static void main(String[] args) {
        System.out.println("Hi");
    }

    @Test
    public void test_db_hash() {
        String key = "fustackgiii";

        int dbCount = 2, tbCount = 4;
        int size = dbCount * tbCount;
        // 散列
        int idx = (size - 1) & (key.hashCode() ^ (key.hashCode() >>> 16));

        int dbIdx = idx / tbCount + 1;
        int tbIdx = idx - tbCount * (dbIdx - 1);

        System.out.println(dbIdx);
        System.out.println(tbIdx);

    }

    @Test
    public void test_str_format() {
        System.out.println(String.format("db%02d", 1));
        System.out.println(String.format("_%02d", 25));
    }

    @Test
    public void test_annotation() throws NoSuchMethodException {
        Class<IUserDao> iUserDaoClass = IUserDao.class;
        Method method = iUserDaoClass.getMethod("insertUser", String.class);

        DBRouter dbRouter = method.getAnnotation(DBRouter.class);

        System.out.println(dbRouter.key());

    }
    @Test
    public void test_getBean() {
        // 初始化一组字符串
        List<String> list = new ArrayList<>();
        list.add("jlkk");
        list.add("lopi");
        list.add("小傅哥");
        list.add("e4we");
        list.add("alpo");
        list.add("yhjk");
        list.add("plop");

// 定义要存放的数组
        String[] tab = new String[8];

// 循环存放
        for (String key : list) {
            int idx = key.hashCode() & (tab.length - 1);  // 计算索引位置
            System.out.println(String.format("key值=%s Idx=%d", key, idx));
            if (null == tab[idx]) {
                tab[idx] = key;
                continue;
            }
            tab[idx] = tab[idx] + "->" + key;
        }
// 输出测试结果
        System.out.println(JSON.toJSONString(tab));
    }

    @Test
    public void test_hash() {
        String dbKeyAttr = "sws123";
        int dbCount = 2, tbCount = 4;
        int size = dbCount * tbCount;
        int idx = size - 1 & (dbKeyAttr.hashCode() ^ dbKeyAttr.hashCode() >>> 16);
        System.out.println("idx = " + idx);
        int dbIdx = idx / tbCount + 1;
        int tbIdx = idx - tbCount * (dbIdx - 1);

        System.out.println("dbIdx = " + dbIdx);
        System.out.println("tbIdx = " + tbIdx);

        System.out.println(String.format("%02d", dbIdx));
        System.out.println(String.format("%03d", tbIdx));
    }

    @Test
    public void test_consistentHash() {
        String dbKeyAttr = "sws123";
        int dbCount = 4, tbCount = 8;
        long idx = consistentHash(dbKeyAttr);
        System.out.println("idx = " + idx);
        // 2. 计算库索引 (使用取模而不是位运算，保证扩容时只有部分数据需要迁移)
        // 库索引从1开始
        int dbIdx = (int)(idx % dbCount) + 1;

        // 3. 计算表索引 (使用二级哈希，确保同一个库中的数据分布均匀)
        // 表索引从1开始
        int tbIdx = (int)((idx / dbCount) % tbCount) + 1;

        System.out.println("dbIdx = " + dbIdx);
        System.out.println("tbIdx = " + tbIdx);

        System.out.println(String.format("%02d", dbIdx));
        System.out.println(String.format("%03d", tbIdx));
    }

    private long consistentHash(String key) {
        final int p = 16777619;
        long hash = 2166136261L;
        for (int i = 0; i < key.length(); i++) {
            hash = (hash ^ key.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;

        // 确保hash为正数
        return hash & 0x7FFFFFFFFFFFFFFFL;
    }

}


