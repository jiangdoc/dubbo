## 容器启动
```java
ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"META-INF/spring/dubbo-demo-consumer.xml"});
```
容器启动的时候会去解析配置文件，spring.handlers的配置文件如下，根据dubbo名找到DubboNamespaceHandler类解析
```
http\://dubbo.apache.org/schema/dubbo=org.apache.dubbo.config.spring.schema.DubboNamespaceHandler
http\://code.alibabatech.com/schema/dubbo=org.apache.dubbo.config.spring.schema.DubboNamespaceHandler
```
DubboNamespaceHandler则会注册DubboBeanDefinitionParser解析相关的elementName。
DubboBeanDefinitionParser类parse的主要逻辑就是把对应的标签解析成对应的实体类，如下：
- <dubbo:application >会被解析成ApplicationConfig实体类。
- <dubbo:registry >会被解析成RegistryConfig实体类。
- <dubbo:protocol >会被解析成ProtocolConfig实体类。
- <dubbo:service >会被解析成ServiceBean实体类。

其中ServiceBean.afterPropertiesSet()方法实现服务提供者的逻辑

## consumer
### 生成Bean对象
RPC服务的Bean定义中beanClass属性值都是ReferenceBean
BeanDefinnation -> beanClass:ReferenceBean
1. bean实例化的时候会调用，ReferenceBean的无参构造方法，实例化一个ReferenceBean。
```java
public ReferenceBean() {
   super();
}
```
2. bean的初始化的时候，因为ReferenceBean实现了InitializingBean，所以会执行ReferenceBean.afterPropertiesSet()
ReferenceBean.getObject()
ReferenceConfig.createProxy()
 - 这里会判断是不是同一个jvm内部的引用，如果是的话直接使用inJvm协议从内存中直接获取实例
 - 不是的话采用注册中心
 - 获取到注册中心，并且处理订阅数据
 - 最后通过代理工厂将invoker转成接口代理对象返回

### 2.getBean()获取Bean对象
获取到的是一个代理对象，代理对象有一个handler属性，属性值是InvokerInvocationHandler对象

### 3.调用Bean对象的方法
InvokerInvocationHandler.invoke()
1. 对于Object中的方法toString, hashCode, equals直接调用invoker的对应方法.
2. 根据方法名和参数构建RpcInvocation对象
3. 执行invoker的invoke()
    1. 调用MockClusterInvoker.invoke方法
    2. 真实调用的是AbstractClusterInvoker.invoke方法
        1. 获取服务提供者集合
        2. 通过负载均衡策略LoadBalance来选择一个Invoker,默认是随机
        3. 具体实现是由子类实现的
    3. AbstractClusterInvoker有好几个实现类，对应的就是dubbo的六个集群容错方式，
    默认是Failover（也就是失败自动切换，当出现失败，重试其它服务器。通常用于读操作，但重试会带来更长延迟。可以通过retries="2"设置重试次数）