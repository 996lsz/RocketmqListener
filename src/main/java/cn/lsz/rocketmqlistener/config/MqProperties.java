package cn.lsz.rocketmqlistener.config;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.rocket-mq")
public class MqProperties {

    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String suffix;

    private String forwardEndpoint;
    private String proEndpoint;

    public Properties getPropertie() {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.AccessKey, this.accessKey);
        properties.setProperty(PropertyKeyConst.SecretKey, this.secretKey);
        properties.setProperty(PropertyKeyConst.NAMESRV_ADDR, this.endpoint);
        return properties;
    }

    public Properties getProMqPropertie() {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.AccessKey, this.accessKey);
        properties.setProperty(PropertyKeyConst.SecretKey, this.secretKey);
        properties.setProperty(PropertyKeyConst.NAMESRV_ADDR, this.proEndpoint);
        return properties;
    }

    public Properties getForwardMqPropertie() {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.AccessKey, this.accessKey);
        properties.setProperty(PropertyKeyConst.SecretKey, this.secretKey);
        properties.setProperty(PropertyKeyConst.NAMESRV_ADDR, this.forwardEndpoint);
        return properties;
    }
}
