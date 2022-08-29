package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 描述
 *
 * @author ELiaNaCc
 * @since 2022-08-25
 */
@Component
public class RuleNacosPublisher {

    @Autowired
    private ConfigService configService;

    public void publish(String dataId, String app, String rules) throws Exception {
        AssertUtil.notEmpty(app, "app name cannot be empty");
        if (rules == null) {
            return;
        }
        // 将服务名称设置为GroupId
        configService.publishConfig(dataId, app, rules);
    }
}
