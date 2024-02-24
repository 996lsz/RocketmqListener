package cn.lsz.rocketmqlistener.config;

import cn.lsz.rocketmqlistener.properties.RocketListenerDefaultProperties;
import com.aliyun.openservices.ons.api.PropertyKeyConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;



/**
 * 阿里云RocketMq配置
 * 其它属性自行补充
 * @author LSZ
 */
@Configuration
public class RocketListenerDefaultConfig {

    @Autowired
    private RocketListenerDefaultProperties defaultProperties;

    @Bean("rocketListenerDefaultPropertiesBean")
    public Properties rocketListenerDefaultProperties() {
        //配置文件
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.ConsumeThreadNums, defaultProperties.getConsumeThreadNums());

        return properties;

    }




}
