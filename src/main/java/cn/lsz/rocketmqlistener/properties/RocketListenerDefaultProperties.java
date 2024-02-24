package cn.lsz.rocketmqlistener.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 其它自定义属性未补充
 * 如有业务需求建议另起Properties，在RocketListener中配置另起的Properties
 *
 * @author LSZ 2023/06/21 10:16
 * @contact 648748030@qq.com
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rocket-listener.default")
public class RocketListenerDefaultProperties {

    @Value("${rocket-listener.default.consumeThreadNums:10}")
    private String consumeThreadNums;

}
