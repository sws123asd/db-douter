# DB Router Spring Boot Starter

## 概述

`db-router-spring-boot-starter` 是一个轻量级的Spring Boot Starter，旨在简化Java应用程序中数据库分库分表的路由逻辑。它提供了灵活的路由策略，包括基于哈希码的路由和一致性哈希路由，并允许通过注解和配置文件轻松集成。

## 特性

*   **动态数据源切换**：根据路由键自动切换到正确的数据源。
*   **多种路由策略**：
    *   **哈希码路由 (HashCode)**：基于路由键的哈希码进行分库分表。
    *   **一致性哈希路由 (ConsistentHash)**：使用一致性哈希算法进行分库分表，有效减少数据迁移量。
*   **注解驱动**：通过 `@DBRouter` 注解在方法上指定路由键，简化使用。
*   **灵活配置**：通过 `application.properties` 或 `application.yml` 配置文件轻松配置分库数量、分表数量、路由键和路由策略等。若每个库的某些配置是固定都一样的，可以使用全局配置。如果某个数据源没有这个属性，取全局配置中的属性，否则取每个数据源中的配置属性。可以通过global下或者db下的type-class-name指定数据库连接池实现，如Druid、HikariCP,并在pool属性下配置对应连接池的私有属性(无论使用中划线格式的属性或者驼峰都兼容)
```
mini-db-router:
  jdbc:
    datasource:
      global:
        type-class-name: com.zaxxer.hikari.HikariDataSource
        pool:
            maximum-pool-size: 50
```
```
mini-db-router:
  jdbc:
    datasource:
      dbCount: 2 # 分库数量
      tbCount: 4 # 分表数量
      routerType: hashcode #路由计算方式
      default: db00 # 默认数据库
      routerKey: userId # 分库分表关键key
      routerType: consistentHash/hashcode（默认） #路由计算策略
      list: db01,db02 # 分库集合
      db00: # 每个数据源配置信息
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://数据库地址
        username: 用户名
        password: 密码
        type-class-name: com.zaxxer.hikari.HikariDataSource
        pool:
          pool-name: Retail_HikariCP
          minimum-idle: 15 #最小空闲连接数量
          idle-timeout: 180000 #空闲连接存活最大时间，默认600000（10分钟）
          maximum-pool-size: 25 #连接池最大连接数，默认是10
          auto-commit: true  #此属性控制从池返回的连接的默认自动提交行为,默认值：true
          max-lifetime: 1800000 #此属性控制池中连接的最长生命周期，值0表示无限生命周期，默认1800000即30分钟
          connection-timeout: 30000 #数据库连接超时时间,默认30秒，即30000
          connection-test-query: SELECT 1
      db01:
        driver-class-name: com.mysql.cj.jdbc.Driver
        url: jdbc:mysql://数据库地址
        username: 用户名
        password: 密码
        type-class-name: com.zaxxer.hikari.HikariDataSource
        pool:
          pool-name: Retail_HikariCP
          minimum-idle: 15 #最小空闲连接数量
          idle-timeout: 180000 #空闲连接存活最大时间，默认600000（10分钟）
          maximum-pool-size: 25 #连接池最大连接数，默认是10
          auto-commit: true  #此属性控制从池返回的连接的默认自动提交行为,默认值：true
          max-lifetime: 1800000 #此属性控制池中连接的最长生命周期，值0表示无限生命周期，默认1800000即30分钟
          connection-timeout: 30000 #数据库连接超时时间,默认30秒，即30000
          connection-test-query: SELECT 1
      db02:
      
```
*   **AOP拦截**：基于Spring AOP拦截指定方法，自动执行路由逻辑。
*   **易于集成**：作为Spring Boot Starter，可以方便地集成到现有的Spring Boot项目中。

## 核心组件

*   **`DBRouterConfig`**: 存储数据库路由的核心配置信息，如分库数量、分表数量、默认路由键、路由策略类型。
*   **`DBRouterJoinPoint`**: AOP切面，拦截带有 `@DBRouter` 注解的方法，执行路由逻辑。
*   **`IDBRouterStrategy`**: 路由策略接口，定义了路由的核心方法。
    *   **`DBRouterStrategyHashCode`**: 基于哈希码的路由策略实现。
    *   **`DBRouterStrategyConsistentHash`**: 基于一致性哈希的路由策略实现，包含虚拟节点以保证数据分布均匀。
*   **`DataSourceAutoConfig`**: 自动配置类，负责初始化数据源、路由策略、AOP切面等Bean。
*   **`DynamicDataSource`**: 动态数据源，根据 `DBContextHolder` 中设置的库标识切换数据源。
*   **`DBContextHolder`**: 使用 `ThreadLocal` 存储当前线程的数据库和表路由键。
*   **`@DBRouter`**: 注解，用于标记需要进行数据库路由的方法，并可指定路由键。
*   **`@RouterKey`**: 注解，用于标记方法参数作为路由键。
*   **`@DBROuterStrategy`**`: 注解，分表标记。

## 如何使用

### 1. 添加依赖

在您的 `pom.xml` 文件中添加此starter的依赖：

```xml
<dependency>
    <groupId>fun.wswj.middleware</groupId>
    <artifactId>db-router-spring-boot-starter</artifactId>
    <version>1.0.1</version>
</dependency>
```
### 2. 配置 application.properties / application.yml
### 3. 在对应service方法使用声明式或编程式，在对应的DAO层标记是否分表

## 注意事项
- DBContextHolder 使用 ThreadLocal 存储路由信息，确保在每次请求处理完毕后调用 clear() 方法（通常在AOP的 finally 块中自动处理），以避免内存泄漏和数据串扰。
- 路由键的获取支持基本类型参数（需配合 @RouterKey 注解）和对象类型参数（通过反射获取指定属性值）。
- 同一个请求，如果在不同的方法中有的方法使用注解进行路由计算，有的使用手动路由计算，会存在threadLocal数据清除获取不到路由数据的问题，需要编码时进行确认。

## 高级用法：编程式事务与手动路由

在某些复杂的业务场景下，您可能需要在编程式事务中更精细地控制数据库路由。本组件支持您在代码中手动调用 `IDBRouterStrategy` 的方法来指定数据源和表，从而实现自定义的路由逻辑。

### 如何在编程式事务中使用

以下示例展示了如何在Spring编程式事务中，结合手动调用 `IDBRouterStrategy` 进行路由：

```java
@Service
public class OrderService {

    @Autowired
    private IDBRouterStrategy dbRouterStrategy; // 注入路由策略

    @Autowired
    private TransactionTemplate transactionTemplate; // 注入事务模板

    // 假设这是您的Mapper接口
    // @Autowired
    // private OrderMapper orderMapper;

    public void createOrderProgrammatically(String userId, String orderId) {
        transactionTemplate.execute(status -> {
            try {
                // 1. 在事务开始前，手动设置路由键
                //    您可以根据业务需求选择调用 doRouter(String) 或分别调用 setDBKey(int) 和 setTBKey(int)
                //    这里以 doRouter 为例，假设 userId 是您的分片键
                dbRouterStrategy.doRouter(userId);
                System.out.println("编程式事务：手动路由到库：" + DBContextHolder.getDBKey() + " 表：" + DBContextHolder.getTBKey());

                // 2. 执行数据库操作
                // orderMapper.insertOrder(orderId, userId, ...);
                // ... 其他数据库操作 ...

                System.out.println("订单创建成功: " + orderId);
                return true;
            } catch (Exception e) {
                status.setRollbackOnly(); // 出现异常，回滚事务
                System.err.println("订单创建失败，事务回滚: " + e.getMessage());
                return false;
            } finally {
                // 3. 在事务结束后（无论成功或失败），清理路由信息，非常重要！
                dbRouterStrategy.clear();
                System.out.println("编程式事务：路由信息已清理");
            }
        });
    }

    // 如果您需要更细粒度地控制库和表，可以这样：
    public void createOrderWithSpecificDbAndTable(int dbIdx, int tbIdx, String orderId) {
        transactionTemplate.execute(status -> {
            try {
                // 1. 手动设置库和表
                dbRouterStrategy.setDBKey(dbIdx);
                dbRouterStrategy.setTBKey(tbIdx);
                System.out.println("编程式事务：手动路由到库：" + DBContextHolder.getDBKey() + " 表：" + DBContextHolder.getTBKey());

                // 2. 执行数据库操作
                // orderMapper.insertOrderInSpecificTable(orderId, ...);

                System.out.println("订单创建成功 (指定库表): " + orderId);
                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                System.err.println("订单创建失败 (指定库表)，事务回滚: " + e.getMessage());
                return false;
            } finally {
                // 3. 清理路由信息
                dbRouterStrategy.clear();
                System.out.println("编程式事务：路由信息已清理 (指定库表)");
            }
        });
    }
}
```

**关键步骤：**

1.  **注入 `IDBRouterStrategy`**：确保您的服务类中注入了 `IDBRouterStrategy` 的实例。Spring容器会自动根据您的配置（`hashcode` 或 `consistentHash`）注入对应的实现。
2.  **手动路由**：在执行数据库操作之前，调用 `dbRouterStrategy.doRouter("您的路由键")` 或 `dbRouterStrategy.setDBKey(dbIndex)` 和 `dbRouterStrategy.setTBKey(tbIndex)` 来指定目标数据库和表。
3.  **执行业务逻辑**：在设置好路由后，执行您的数据库操作。
4.  **清理路由信息**：**非常重要！** 在事务完成（提交或回滚）后，务必在 `finally` 块中调用 `dbRouterStrategy.clear()` 来清除 `ThreadLocal` 中的路由信息，以避免影响后续的数据库操作。

通过这种方式，您可以灵活地将数据库路由逻辑集成到编程式事务管理中，满足更复杂的业务需求。
```
```
