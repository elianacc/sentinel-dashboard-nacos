# Sentinel Dashboard Nacos 修改sentinel源码过程



### 1. 下载源码压缩包

在Sentinel-github下载需要版本的压缩包，比如`Sentinel-1.8.1.zip` （使用其他版本就下那个版本的zip）

### 2 .加载源码

将下载好的`Sentinel-1.8.1.zip`解压，使用`IDE`工具，打开`sentinel-dashboard`工程

### 3. 修改pom

将`sentinel-datasource-nacos`的`scope`标签注释掉

```xml
<dependency>
    <groupId>com.alibaba.csp</groupId>
    <artifactId>sentinel-datasource-nacos</artifactId>
    <!--<scope>test</scope>-->
</dependency>
```

### 4. 创建公共配置

在进行规则代码修改之前需要创建Nacos配置文件，在`com.alibaba.csp.sentinel.dashboard.rule`包下创建`nacos`包，并且在包下创建四个类：`RuleNacosConfig`、`RuleNacosProvider`、`RuleNacosPublisher`、`RuleNacosConstants`

RuleNacosConfig：

```java
@Configuration
public class RuleNacosConfig{
    @Bean
    public ConfigService nacosConfigService() throws Exception {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.SERVER_ADDR, "xxx.xx.xx.xx:8848");   // nacos服务地址
        // properties.put(PropertyKeyConst.NAMESPACE, "xxx"); 命名空间
        // properties.put(PropertyKeyConst.USERNAME, "xxx"); 用户名
        // properties.put(PropertyKeyConst.PASSWORD, "xxx"); 密码
        return ConfigFactory.createConfigService(properties);
    }
}
```

RuleNacosProvider：

```java
@Component
public class RuleNacosProvider {

    @Autowired
    private ConfigService configService;

    public String getRules(String dataId, String app) throws Exception {
        // 将服务名称设置为GroupId
        return configService.getConfig(dataId, app, 3000);
    }
}
```

RuleNacosPublisher：

```java
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
```

RuleNacosConstants ：

```java
public class RuleNacosConstants {
    public static final String FLOW_DATA_ID = "sentinel.rule.flow";
    public static final String DEGRADE_DATA_ID = "sentinel.rule.degrade";
    public static final String SYSTEM_DATA_ID = "sentinel.rule.system";
    public static final String PARAM_DATA_ID = "sentinel.rule.param";
    public static final String AUTHORITY_DATA_ID = "sentinel.rule.authority";
    public static final String GATEWAY_API_DATA_ID = "sentinel.rule.gateway.api";
    public static final String GATEWAY_FLOW_DATA_ID = "sentinel.rule.gateway.flow";
}
```



### 5. 控制台规则配置

通过修改源码，实现`流控规则、降级规则、热点规则、系统规则、授权规则`的持久化操作



#### 流控规则

1、修改sidebar.html：

```html
<!--将dashboard.flowV1 修改为dashboard.flow -->
<li ui-sref-active="active">
    <a ui-sref="dashboard.flowV1({app: entry.app})">
        <i class="glyphicon glyphicon-filter"></i>&nbsp;&nbsp;流控规则
    </a>
</li>

<!--修改后代码-->
<li ui-sref-active="active">
    <a ui-sref="dashboard.flow({app: entry.app})">
        <i class="glyphicon glyphicon-filter"></i>&nbsp;&nbsp;流控规则
    </a>
</li>
```

2、修改FlowControllerV2：
将`RuleNacosProvider`和`RuleNacosPublisher`注入到`FlowControllerV2`中

```java
// 修改位置如下：
@Autowired
@Qualifier("flowRuleDefaultProvider")
private DynamicRuleProvider<List<FlowRuleEntity>> ruleProvider;
@Autowired
@Qualifier("flowRuleDefaultPublisher")
private DynamicRulePublisher<List<FlowRuleEntity>> rulePublisher;

// 将上面代码修改为以下代码：
@Autowired
private RuleNacosProvider ruleProvider;
@Autowired
private RuleNacosPublisher rulePublisher;
```

修改读取逻辑：

```java
// 修改位置如下：
List<FlowRuleEntity> rules = ruleProvider.getRules(app);
if (rules != null && !rules.isEmpty()) {
    for (FlowRuleEntity entity : rules) {
         entity.setApp(app);
         if (entity.getClusterConfig() != null && entity.getClusterConfig().getFlowId() != null) {
             entity.setId(entity.getClusterConfig().getFlowId());
          }
     }
}

// 将上面代码修改为以下代码：
String ruleStr = ruleProvider.getRules(RuleNacosConstants.FLOW_DATA_ID, app);
List<FlowRuleEntity> rules = new ArrayList<>();
if (ruleStr != null) {
    rules = JSON.parseArray(ruleStr, FlowRuleEntity.class);
    if (rules != null && !rules.isEmpty()) {
        for (FlowRuleEntity entity : rules) {
            entity.setApp(app);
        }
    }
 }
```

修改推送逻辑：

```java
// 修改位置如下：
private void publishRules(/*@NonNull*/ String app) throws Exception {
    List<FlowRuleEntity> rules = repository.findAllByApp(app);
    rulePublisher.publish(app, rules);
}

// 将上面代码修改为以下代码：
private void publishRules(String app) {
  try {
       List<FlowRuleEntity> rules = repository.findAllByApp(app);
       String ruleStr = JSON.toJSONString(rules);
       rulePublisher.publish(RuleNacosConstants.FLOW_DATA_ID, app, ruleStr);
  } catch (Exception e) {
      e.printStackTrace();
  }
}
```



#### 降级规则

修改DegradeController：
将`RuleNacosProvider`和`RuleNacosPublisher`注入到`DegradeController`中

```java
// 加入以下代码：
@Autowired
private RuleNacosProvider ruleProvider;
@Autowired
private RuleNacosPublisher rulePublisher;
```

修改读取逻辑：

```java
// 修改位置如下：
List<DegradeRuleEntity> rules = sentinelApiClient.fetchDegradeRuleOfMachine(app, ip, port);

// 将上面代码修改为以下代码：
String ruleStr = ruleProvider.getRules(RuleNacosConstants.DEGRADE_DATA_ID, app);
List<DegradeRuleEntity> rules = new ArrayList<>();
if (ruleStr != null) {
    rules = JSON.parseArray(ruleStr, DegradeRuleEntity.class);
    if (rules != null && !rules.isEmpty()) {
       for (DegradeRuleEntity entity : rules) {
           entity.setApp(app);
       }
   }
}
```

修改推送逻辑：

```java
// 1、修改位置如下：
private boolean publishRules(String app, String ip, Integer port) {
   List<DegradeRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
   return sentinelApiClient.setDegradeRuleOfMachine(app, ip, port, rules);
}

// 将上面代码修改为以下代码：
private void publishRules(String app) {
  try {
      List<DegradeRuleEntity> rules = repository.findAllByApp(app);
      String ruleStr = JSON.toJSONString(rules);
      rulePublisher.publish(RuleNacosConstants.DEGRADE_DATA_ID, app, ruleStr);
  } catch (Exception e) {
     e.printStackTrace();
  }
}
=======================================================================================

// 2、修改位置如下：有两处
if (!publishRules(entity.getApp(), entity.getIp(), entity.getPort())) {
     logger.warn("Publish degrade rules failed, app={}", entity.getApp());
}

// 将上面代码修改为以下代码：
publishRules(entity.getApp());
=======================================================================================

// 3、修改位置如下：
if (!publishRules(oldEntity.getApp(), oldEntity.getIp(), oldEntity.getPort())) {
   logger.warn("Publish degrade rules failed, app={}", oldEntity.getApp());
}

// 将上面代码修改为以下代码：
publishRules(oldEntity.getApp());
```



#### 热点规则

修改ParamRuleController：
将`RuleNacosProvider`和`RuleNacosPublisher`注入到`ParamFlowRuleController`中

```java
// 加入以下代码：
@Autowired
private RuleNacosProvider ruleProvider;
@Autowired
private RuleNacosPublisher rulePublisher;
```

修改读取逻辑：

```java
// 修改位置如下：
return sentinelApiClient.fetchParamFlowRulesOfMachine(app, ip, port)
                .thenApply(repository::saveAll)
                .thenApply(Result::ofSuccess)
                .get();

// 将上面代码修改为以下代码：
String ruleStr = ruleProvider.getRules(RuleNacosConstants.PARAM_DATA_ID, app);
List<ParamFlowRuleEntity> rules = new ArrayList<>();
if (ruleStr != null) {
    rules = JSON.parseArray(ruleStr, ParamFlowRuleEntity.class);
    if (rules != null && !rules.isEmpty()) {
        for (ParamFlowRuleEntity entity : rules) {
           entity.setApp(app);
   		}
    }
}
rules = repository.saveAll(rules);
return Result.ofSuccess(rules);
```

修改推送逻辑：

```java
// 1、修改位置如下：
private CompletableFuture<Void> publishRules(String app, String ip, Integer port) {
    List<ParamFlowRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
    return sentinelApiClient.setParamFlowRuleOfMachine(app, ip, port, rules);
}

// 将上面代码修改为以下代码：
private void publishRules(String app) {
   try {
       List<ParamFlowRuleEntity> rules = repository.findAllByApp(app);
       String ruleStr = JSON.toJSONString(rules);
       rulePublisher.publish(RuleNacosConstants.PARAM_DATA_ID, app, ruleStr);
   } catch (Exception e) {
     e.printStackTrace();
   }
}
=======================================================================================

// 2、修改位置如下：有两处
try {
   entity = repository.save(entity);
   publishRules(entity.getApp(), entity.getIp(), entity.getPort()).get();
   return Result.ofSuccess(entity);
} catch (ExecutionException ex) {
	....
}
// 将上面代码修改为以下代码：
try {
   entity = repository.save(entity);
   publishRules(entity.getApp());
   return Result.ofSuccess(entity);
} catch (Exception ex) {
	....
}
=======================================================================================

// 3、修改位置如下：
try {
 	repository.delete(id);
    publishRules(oldEntity.getApp(), oldEntity.getIp(), oldEntity.getPort()).get();
    return Result.ofSuccess(id);
} catch (ExecutionException ex) {
	....
}

// 将上面代码修改为以下代码：
try {
 	repository.delete(id);
    publishRules(oldEntity.getApp());
    return Result.ofSuccess(id);
} catch (Exception ex) {
	....
}
```



#### 系统规则

修改SystemController：
将`RuleNacosProvider`和`RuleNacosPublisher`注入到`SystemController`中

```java
// 加入以下代码：
@Autowired
private RuleNacosProvider ruleProvider;
@Autowired
private RuleNacosPublisher rulePublisher;
```

修改读取逻辑：

```java
// 修改位置如下：
List<SystemRuleEntity> rules = sentinelApiClient.fetchSystemRuleOfMachine(app, ip, port);

// 将上面代码修改为以下代码：
String ruleStr = ruleProvider.getRules(RuleNacosConstants.SYSTEM_DATA_ID, app);
List<SystemRuleEntity> rules = new ArrayList<>();
if (ruleStr != null) {
    rules = JSON.parseArray(ruleStr, SystemRuleEntity.class);
    if (rules != null && !rules.isEmpty()) {
        for (SystemRuleEntity entity : rules) {
             entity.setApp(app);
        }
    }
}
```

修改推送逻辑：

```java
// 1、修改位置如下：
private boolean publishRules(String app, String ip, Integer port) {
   List<SystemRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
   return sentinelApiClient.setSystemRuleOfMachine(app, ip, port, rules);
}

// 将上面代码修改为以下代码：
private void publishRules(String app) {
  try {
       List<SystemRuleEntity> rules = repository.findAllByApp(app);
       String ruleStr = JSON.toJSONString(rules);
       rulePublisher.publish(RuleNacosConstants.SYSTEM_DATA_ID, app, ruleStr);
   } catch (Exception e) {
       e.printStackTrace();
  }
}
=======================================================================================

// 2、修改位置如下
if (!publishRules(app, ip, port)) {
    logger.warn("Publish system rules fail after rule add");
}

// 将上面代码修改为以下代码：
publishRules(entity.getApp());
=======================================================================================

// 3、修改位置如下
if (!publishRules(entity.getApp(), entity.getIp(), entity.getPort())) {
    logger.info("publish system rules fail after rule update");
}

// 将上面代码修改为以下代码：
publishRules(entity.getApp());
=======================================================================================

// 4、修改位置如下：
if (!publishRules(oldEntity.getApp(), oldEntity.getIp(), oldEntity.getPort())) {
    logger.info("publish system rules fail after rule delete");
}

// 将上面代码修改为以下代码：
publishRules(oldEntity.getApp());
```



#### 授权规则

修改AuthorityRuleController：
将`RuleNacosPublisher`和`RuleNacosProvider`注入到`AuthorityRuleController`中

```java
// 加入以下代码：
@Autowired
private RuleNacosProvider ruleProvider;
@Autowired
private RuleNacosPublisher rulePublisher;
```

修改读取逻辑：

```java
// 修改位置如下：
List<AuthorityRuleEntity> rules = sentinelApiClient.fetchAuthorityRulesOfMachine(app, ip, port);

// 将上面代码修改为以下代码：
 String ruleStr = ruleProvider.getRules(RuleNacosConstants.AUTHORITY_DATA_ID, app);
List<AuthorityRuleEntity> rules = new ArrayList<>();
if (ruleStr != null) {
     rules = JSON.parseArray(ruleStr, AuthorityRuleEntity.class);
     if (rules != null && !rules.isEmpty()) {
         for (AuthorityRuleEntity entity : rules) {
              entity.setApp(app);
         }
    }
}
  
123456789101112131415
```

修改推送逻辑：

```java
// 1、修改位置如下：
private boolean publishRules(String app, String ip, Integer port) {
    List<AuthorityRuleEntity> rules = repository.findAllByMachine(MachineInfo.of(app, ip, port));
    return sentinelApiClient.setAuthorityRuleOfMachine(app, ip, port, rules);
}

// 将上面代码修改为以下代码：
private void publishRules(String app) {
   try {
       List<AuthorityRuleEntity> rules = repository.findAllByApp(app);
       String ruleStr = JSON.toJSONString(rules);
       rulePublisher.publish(RuleNacosConstants.AUTHORITY_DATA_ID, app, ruleStr);
   } catch (Exception e) {
       e.printStackTrace();
   }
}
=======================================================================================

// 2、修改位置如下：有两处
if (!publishRules(entity.getApp(), entity.getIp(), entity.getPort())) {
    logger.info("Publish authority rules failed after rule update");
}

// 将上面代码修改为以下代码：
publishRules(entity.getApp());
=======================================================================================

// 3、修改位置如下：
if (!publishRules(oldEntity.getApp(), oldEntity.getIp(), oldEntity.getPort())) {
   logger.error("Publish authority rules failed after rule delete");
}

// 将上面代码修改为以下代码：
publishRules(oldEntity.getApp());
```



### 6. 网关控制台规则配置

配置网关控制台规则，在启动网关时需要加上参数：`-Dcsp.sentinel.app.type=1`

#### API管理

修改GatewayApiController：
将`RuleNacosPublisher`和`RuleNacosProvider`注入到`GatewayApiController`中

```java
// 加入以下代码：
@Autowired
private RuleNacosProvider ruleProvider;
@Autowired
private RuleNacosPublisher rulePublisher;
```

修改读取逻辑：

```java
// 修改位置如下：
List<ApiDefinitionEntity> apis = sentinelApiClient.fetchApis(app, ip, port).get();

// 将上面代码修改为以下代码：
String ruleStr = ruleProvider.getRules(RuleNacosConstants.GATEWAY_API_DATA_ID, app);
List<ApiDefinitionEntity> apis = new ArrayList<>();
if (ruleStr != null) {
    apis = JSON.parseArray(ruleStr, ApiDefinitionEntity.class);
    if (apis != null && !apis.isEmpty()) {
        for (ApiDefinitionEntity entity : apis) {
             entity.setApp(app);
        }
    }
}
 
```

修改推送逻辑：

```java
// 1、修改位置如下：
private boolean publishApis(String app, String ip, Integer port) {
   List<ApiDefinitionEntity> apis = repository.findAllByMachine(MachineInfo.of(app, ip, port));
   return sentinelApiClient.modifyApis(app, ip, port, apis);
}

// 将上面代码修改为以下代码：
private void publishApi(String app) {
  try {
    	 List<ApiDefinitionEntity> apis= repository.findAllByApp(app);
    	 String ruleStr = JSON.toJSONString(apis);
    	 rulePublisher.publish(RuleNacosConstants.GATEWAY_API_DATA_ID, app, ruleStr);
     } catch (Exception e) {
         e.printStackTrace();
   }
}
=======================================================================================

// 2、修改位置如下
if (!publishApis(app, ip, port)) {
     logger.warn("publish gateway apis fail after add");
}

// 将上面代码修改为以下代码：
publishApi(entity.getApp());
=======================================================================================

// 3、修改位置如下
if (!publishApis(app, entity.getIp(), entity.getPort())) {
    logger.warn("publish gateway apis fail after update");
}

// 将上面代码修改为以下代码：
publishApi(entity.getApp());
=======================================================================================

// 4、修改位置如下：
if (!publishApis(oldEntity.getApp(), oldEntity.getIp(), oldEntity.getPort())) {
    logger.warn("publish gateway apis fail after delete");
}

// 将上面代码修改为以下代码：
publishApi(oldEntity.getApp());
```



#### 降级规则  系统规则   同上即可



### 7. 打包部署



进入到`sentinel-dashboard`所在目的，通过`mvn clean install package -DskipTests=true`进行打包

或使用idea打包都可



到这里就实现了sentinel控制台配置自动同步到nacos配置中，实现双向同步。











