package fun.wswj.middleware.test;

import fun.wswj.middleware.db.router.annotation.DBRouter;
import org.junit.Test;

import java.lang.reflect.Method;

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
    public void test_annotation() throws NoSuchMethodException {
        Class<IUserDao> iUserDaoClass = IUserDao.class;
        Method method = iUserDaoClass.getMethod("insertUser", String.class);

        DBRouter dbRouter = method.getAnnotation(DBRouter.class);

        System.out.println(dbRouter.key());

    }

    @Test
    public void test_db_consistent() {
        int dbCount = 8, tbCount = 8;
        int physicalNodeCount = dbCount * tbCount;
        int virtualNodeCount = Math.max(1024, (int)(200 * Math.log(physicalNodeCount)));
        int optimalVNodes = Math.max(100, (int)(50 * Math.log(physicalNodeCount + 1)));
        System.out.println(virtualNodeCount);
        System.out.println(optimalVNodes);
    }
}


