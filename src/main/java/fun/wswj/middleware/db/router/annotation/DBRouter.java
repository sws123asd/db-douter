package fun.wswj.middleware.db.router.annotation;

import java.lang.annotation.*;


/**
 *  路由注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DBRouter {

    /** 分库分表字段 */
    String key() default "";

}
