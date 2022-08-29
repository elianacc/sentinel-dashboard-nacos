package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 描述
 *
 * @author ELiaNaCc
 * @since 2022-08-25
 */
@Configuration
public class RuleNacosConfig {
    @Bean
    public ConfigService nacosConfigService() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, "127.0.0.1:8848");
        // properties.put(PropertyKeyConst.NAMESPACE, "xxx"); 命名空间
        // properties.put(PropertyKeyConst.USERNAME, "xxx"); 用户名
        // properties.put(PropertyKeyConst.PASSWORD, "xxx"); 密码
        return ConfigFactory.createConfigService(properties);
    }
}
