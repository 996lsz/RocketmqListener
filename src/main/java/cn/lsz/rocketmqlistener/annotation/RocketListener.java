package cn.lsz.rocketmqlistener.annotation;

import java.lang.annotation.*;


/**
 * description
 *
 * @author LSZ 2023/06/29 15:05
 * @contact 648748030@qq.com
 */
@Inherited
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RocketListener {

    //消费topic，支持配置文件替换占位符，如${properties.topic}
    String topic();

    //消费group，支持配置文件替换占位符，如${properties.group}
    String gid();

    String[] tags();

    //消费者配置，通过定义配置文件bean进行使用
    String properties() default "rocketListenerDefaultPropertiesBean";

}
