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
2. bean的初始化的时候，因为ReferenceBean实现了InitializingBean，所以会执行ReferenceBean.afterPropertiesSet()，这里会初始化一些配置

### 2.getBean()获取Bean对象
获取到的是一个代理对象，代理对象有一个handler属性，属性值是InvokerInvocationHandler对象，InvokerInvocationHandler持有一个invoker属性。

当我们有Bean去引用，或者显示的使用getBean()方法时，会调用AbstractBeanFactory.doGetBean()->AbstractBeanFactory.getObjectForBeanInstance()->FactoryBeanRegistrySupport.getObjectFromFactoryBean()
->FactoryBeanRegistrySupport.doGetObjectFromFactoryBean() ->object = factory.getObject();
也就会执行ReferenceBean.getObject()->ReferenceConfig.createProxy()
 - 这里会判断是不是同一个jvm内部的引用，如果是的话直接使用inJvm协议从内存中直接获取实例
 - 不是的话采用注册中心
 - 执行protocol.refer()方法，获取到注册中心，doRefer()注册到注册中心，并且处理订阅数据(也就是从注册中心拉下来服务提供者信息)，生成Invoker对象（根据容错机制实现对应的实现类，默认是FailoverClusterInvoker,因为MockClusterWrapper是Cluster的AOP切面，所以会执行切面逻辑生成一个MockClusterInvoker对象）
 - 最后执行proxyFactory.getProxy(invoker)，通过代理工厂将invoker转成接口代理对象返回（其实就是使用Javassist为接口生成了一个实现类）
    proxyFactory.getProxy(invoker)->AbstractProxyFactory.getProxy(invoker)->JavassistProxyFactory.getProxy(invoker,interfaces)
    - 1.这里会使用Proxy.getProxy(interfaces)方法，生成一个接口的代理类（其实就一个实现类，具体代码可以去Proxy.getProxy(ClassLoader, Class<?>...)看到）
    - 2.newInstance(new InvokerInvocationHandler(invoker))，这里会使用上面生成的代理类 的构造方法，实例化代理类，并且通过构造方法把InvokerInvocationHandler(invoker)传进去

上面生成的代理对象，以DemoService为例：
```java
public class proxy0 implements ClassGenerator.DC, EchoService, DemoService {
    // 方法数组
    public static Method[] methods;
    private InvocationHandler handler;
    public proxy0(InvocationHandler invocationHandler) {
        this.handler = invocationHandler;
    }
    public proxy0() {
    }
    public String sayHello(String string) {
        // 将参数存储到 Object 数组中
        Object[] arrobject = new Object[]{string};
        // 调用 InvocationHandler 实现类的 invoke 方法得到调用结果
        Object object = this.handler.invoke(this, methods[0], arrobject);
        // 返回调用结果
        return (String)object;
    }
    // 回声测试方法
    public Object $echo(Object object) {
        Object[] arrobject = new Object[]{object};
        Object object2 = this.handler.invoke(this, methods[1], arrobject);
        return object2;
    }
}
```

### 3.调用Bean对象的方法
通过看代理类的代码，可以知道，我们调用接口的方法，其实就是调用InvokerInvocationHandler.invoke()方法，下面就来一起分析下这个方法。
1. 对于Object中的方法toString, hashCode, equals直接调用invoker的对应方法.
2. 根据方法名和参数构建RpcInvocation对象
3. 执行invoker的invoke()，invoker对象是一个MockClusterInvoker类型
    1. 调用MockClusterInvoker.invoke方法
    2. 真实调用的是AbstractClusterInvoker.invoke方法
        1. 获取服务提供者集合：调用directory.list()方法获取Invoker列表(可将Invoker简单理解为服务提供者)，Directory的用途就是保存Invoker，他的实现类RegisterDirectory会维护一个服务目录，当服务目录变化的时候，会notify()通知目录动态改变。
        2. 根据配置的负载均衡策略生成LoadBalance对象,默认是加权随机(Dubbo 提供了4种负载均衡实现:权重随机算法的 RandomLoadBalance、最少活跃调用数算法的 LeastActiveLoadBalance、基于hash一致性的 ConsistentHashLoadBalance，基于加权轮询算法的 RoundRobinLoadBalance)
        3. 具体选择服务和调用服务是由AbstractClusterInvoker子类实现的
         AbstractClusterInvoker有好几个实现类，对应的就是dubbo的六个集群容错方式，
            默认是Failover（也就是失败自动切换，当出现失败，重试其它服务器。通常用于读操作，但重试会带来更长延迟。可以通过retries="2"设置重试次数）

整个调用链如下：
```html
proxy0#sayHello(String)
  —> InvokerInvocationHandler#invoke(Object, Method, Object[])
    —> MockClusterInvoker#invoke(Invocation)
      —> AbstractClusterInvoker#invoke(Invocation)
        —> FailoverClusterInvoker#doInvoke(Invocation, List<Invoker<T>>, LoadBalance)
          —> Filter#invoke(Invoker, Invocation)  // 包含多个 Filter 调用
            —> ListenerInvokerWrapper#invoke(Invocation) 
              —> AbstractInvoker#invoke(Invocation) 
                —> DubboInvoker#doInvoke(Invocation)
                  —> ReferenceCountExchangeClient#request(Object, int)
                    —> HeaderExchangeClient#request(Object, int)
                      —> HeaderExchangeChannel#request(Object, int)
                        —> AbstractPeer#send(Object)
                          —> AbstractClient#send(Object, boolean)
                            —> NettyChannel#send(Object, boolean)
                              —> NioClientSocketChannel#write(Object)
```
### 4.返回值
什么时候获取返回值的逻辑在DubboInvoker.doInvoke()方法中。
返回值分为三种类型：
1. 异步无返回值：
    1. 发送消息
    2. 设置上下文RpcContext中feature字段为null
    3. 创建RpcResult对象返回
2. 异步有返回值
    1. 发送消息
    2. 设置上下文RpcContext中feature字段的值
    3. 创建RpcResult对象返回
3. 同步调用
    1. 发送消息
    2. 直接调用Feature对象的get()方法等待返回

## Provider
### 1. 创建Bean实例
- BeanDefinition：注册后的Bean的BeanDefinition的class变成了ServiceBean
- 实例化：所以Spring实例化出来的也是一个ServiceBean对象
- 初始化：
    1. ServiceBean实现了ApplicationContextAware接口，所以在执行Bean前置处理器的时候会执行ApplicationContextAwareProcessor.postProcessBeforeInitialization()方法，
        最后调用ServiceBean.setApplicationContext()方法，将applicationContext注入到ServiceBean对象中，并且利用反射调用addApplicationListener()，将自己注册成监听器。
    2. ServiceBean实现了InitializingBean接口，所以在执行bean的初始化方法时（init-method）会调用ServiceBean的afterPropertiesSet()方法，此方法会初始化一些配置。

### 2. Spring容器初始化完毕发送事件监听
1. Spring启动容器的最后一步会调用finishRefresh()方法刷新容器，这里会发送事件，触发监听器执行（因为上面注册过）
2. 收到事件后执行ServiceBean.onApplicationEvent()方法。
    整个逻辑大致可分为三个部分：
    - 第一部分是前置工作，主要用于检查参数，组装 URL。
    - 第二部分是导出服务，包含导出服务到本地 (JVM)，和导出服务到远程两个过程。(导出服务在我看来其实就是开辟端口，接收服务消费者的请求，默认是netty)
    - 第三部分是向注册中心注册服务，用于服务发现。
    
```java
// Wrapper0 是在运行时生成的，大家可使用 Arthas 进行反编译 
public class Wrapper0 extends Wrapper implements ClassGenerator.DC {
    public static String[] pns;
    public static Map pts;
    public static String[] mns;
    public static String[] dmns;
    public static Class[] mts0;
    // 省略其他方法
    public Object invokeMethod(Object object, String string, Class[] arrclass, Object[] arrobject) throws InvocationTargetException {
        DemoService demoService;
        try {
            // 类型转换
            demoService = (DemoService)object;
        }
        catch (Throwable throwable) {
            throw new IllegalArgumentException(throwable);
        }
        try {
            // 根据方法名调用指定的方法
            if ("sayHello".equals(string) && arrclass.length == 1) {
                return demoService.sayHello((String)arrobject[0]);
            }
        }
        catch (Throwable throwable) {
            throw new InvocationTargetException(throwable);
        }
        throw new NoSuchMethodException(new StringBuffer().append("Not found method \"").append(string).append("\" in class com.alibaba.dubbo.demo.DemoService.").toString());
    }
}
```
    
### 3. 接收消费者调用

接收调用的执行链：
```html
NettyHandler#messageReceived(ChannelHandlerContext, MessageEvent)
  —> AbstractPeer#received(Channel, Object)
    —> MultiMessageHandler#received(Channel, Object)
      —> HeartbeatHandler#received(Channel, Object)
        —> AllChannelHandler#received(Channel, Object)
          —> ExecutorService#execute(Runnable)    // 由线程池执行后续的调用逻辑
```

```html
ChannelEventRunnable#run()
  —> DecodeHandler#received(Channel, Object)
    —> HeaderExchangeHandler#received(Channel, Object)
      —> HeaderExchangeHandler#handleRequest(ExchangeChannel, Request)
        —> DubboProtocol.requestHandler#reply(ExchangeChannel, Object)
          —> Filter#invoke(Invoker, Invocation)
            —> AbstractProxyInvoker#invoke(Invocation)
              —> Wrapper0#invokeMethod(Object, String, Class[], Object[])
                —> DemoServiceImpl#sayHello(String)
```

## 负载均衡算法
在 Dubbo 中，所有负载均衡实现类均继承自 AbstractLoadBalance，该类实现了 LoadBalance 接口，并封装了一些公共的逻辑。
Dubbo 提供了4种负载均衡实现:
1. 基于权重随机算法的 RandomLoadBalance
> RandomLoadBalance 是加权随机算法的具体实现，它的算法思想很简单。假设我们有一组服务器 servers = [A, B, C]，他们对应的权重为 weights = [5, 3, 2]，权重总和为10。
现在把这些权重值平铺在一维坐标值上，[0, 5) 区间属于服务器 A，[5, 8) 区间属于服务器 B，[8, 10) 区间属于服务器 C。
接下来通过随机数生成器生成一个范围在 [0, 10) 之间的随机数，然后计算这个随机数会落到哪个区间上。
比如数字3会落到服务器 A 对应的区间上，此时返回服务器 A 即可。权重越大的机器，在坐标轴上对应的区间范围就越大，因此随机数生成器生成的数字就会有更大的概率落到此区间内。
只要随机数生成器产生的随机数分布性很好，在经过多次选择后，每个服务器被选中的次数比例接近其权重比例。
2. 基于最少活跃调用数算法的 LeastActiveLoadBalance
> 活跃调用数越小，表明该服务提供者效率越高，单位时间内可处理更多的请求。此时应优先将请求分配给该服务提供者。
在具体实现中，每个服务提供者对应一个活跃数 active。初始情况下，所有服务提供者活跃数均为0。每收到一个请求，活跃数加1，完成请求后则将活跃数减1。
在服务运行一段时间后，性能好的服务提供者处理请求的速度更快，因此活跃数下降的也越快，此时这样的服务提供者能够优先获取到新的服务请求、这就是最小活跃数负载均衡算法的基本思想。
除了最小活跃数，LeastActiveLoadBalance 在实现上还引入了权重值。
举个例子，在一个服务提供者集群中，有两个性能优异的服务提供者。某一时刻它们的活跃数相同，此时 Dubbo 会根据它们的权重去分配请求，权重越大，获取到新请求的概率就越大。如果两个服务提供者权重相同，此时随机选择一个即可。
3. 基于一致性hash算法的 ConsistentHashLoadBalance
> 一致性hash算法是，首先会定义一个圆环，平均分为2^32个节点，然后根据机器的IP或者其他的唯一键值通过hash运算生成hash值，最后通过hash值确定在圆环上的位置。
当有查询或者写入进来时，同样通过一个唯一键生成一个hash值，并确定在环中的位置，最后顺时针向下找，找到的第一个机器节点就是要查询或者写入的机器节点。
当其中一个节点挂了，或者新加一个节点，只有这个节点和前一个节点中间的位置小部分会受影响，其他大部分都不受影响。
一致性hash算法有一个问题是数据倾斜，也就是大部分节点比较集中，导致大部分请求都落到了一个节点上。解决方法就是添加虚拟节点，查找或写入请求过来的流程和上面说的一致，只是如果是查到的虚拟节点，需要根据虚拟节点找到真实节点。
在duboo中默认是为每一个服务提供者生成160个虚拟节点，然后放到TreeMap中，服务消费者获取节点的时候，会先将参数转成Key，然后通过md5和hash运算获取hash值。最后使用TreeMap的tailMap()找到invoker返回。
4. 基于加权轮询算法的 RoundRobinLoadBalance
>经过加权，每台服务器能够得到的请求数比例，接近或等于他们的权重比。
比如服务器 A、B、C 权重比为 5:2:1。那么在8次请求中，服务器 A 将收到其中的5次请求，服务器 B 会收到其中的2次请求，服务器 C 则收到其中的1次请求。
[官方负载均衡实现](https://dubbo.apache.org/zh/docs/v2.7/dev/source/loadbalance/)

## 集群
集群工作过程可分为两个阶段，第一个阶段是在服务消费者初始化期间，集群 Cluster 实现类为服务消费者创建 Cluster Invoker 实例，也就是Invoker的合并操作。
第二个阶段是在服务消费者进行远程调用时。以 FailoverClusterInvoker 为例，该类型 Cluster Invoker 首先会调用 Directory 的 list 方法列举 Invoker 列表（可将 Invoker 简单理解为服务提供者）。
Directory 的用途是保存 Invoker，可简单类比为 List<Invoker>。其实现类 RegistryDirectory 是一个动态服务目录，可感知注册中心配置的变化，它所持有的 Invoker 列表会随着注册中心内容的变化而变化。
每次变化后，RegistryDirectory 会动态增删 Invoker，并调用 Router 的 route 方法进行路由，过滤掉不符合路由规则的 Invoker。
当 FailoverClusterInvoker 拿到 Directory 返回的 Invoker 列表后，它会通过 LoadBalance 从 Invoker 列表中选择一个 Invoker。
最后 FailoverClusterInvoker 会将参数传给 LoadBalance 选择出的 Invoker 实例的 invoke 方法，进行真正的远程调用。

### Cluster 和 AbstractClusterInvoker区别？
Cluster接口只有一个方法，就是join()方法，这个方法就是将多个Directory合并为一个AbstractClusterInvoker，其实就是根据集群容错创建Invoker实例。
AbstractClusterInvoker 封装了服务提供者选择逻辑，以及远程调用失败后的处理逻辑。

### 集群容错
AbstractClusterInvoker实现了一些公共的逻辑，比如调用list()从RegistryDirectory中获取到获取Invoker列表，选择一个负载均衡器（LoadBalance），还有select()也就是负载均衡方法等等。
AbstractClusterInvoker还有一个doInvoke()的模板方法，具体实现调用逻辑和容错逻辑子类实现。
- FailoverClusterInvoker：失败切换，调用失败会切换Invoker重试。
- FailbackClusterInvoker：失败恢复，会在调用失败后，返回一个空结果给服务消费者。并通过定时任务对失败的调用进行重试，适合执行消息通知等操作。
- FailfastClusterInvoker：快速失败，只会进行一次调用，失败后立即抛出异常。适用于幂等操作，比如新增记录。
- FailsafeClusterInvoker：失败安全，当调用过程中出现异常时，FailsafeClusterInvoker 仅会打印异常，而不会抛出异常。适用于写入审计日志等操作。
- ForkingClusterInvoker：并行调用多个服务提供者，会在运行时通过线程池创建多个线程，并发调用多个服务提供者。只要有一个服务提供者成功返回了结果，doInvoke 方法就会立即结束运行。ForkingClusterInvoker 的应用场景是在一些对实时性要求比较高读操作
- BroadcastClusterInvoker：会逐个调用每个服务提供者，如果其中一台报错，在循环调用结束后，BroadcastClusterInvoker 会抛出异常。该类通常用于通知所有提供者更新缓存或日志等本地资源信息。

## 领域模型
在 Dubbo 的核心领域模型中：
Protocol 是服务域，它是 Invoker 暴露和引用的主功能入口，它负责 Invoker 的生命周期管理。
Invoker 是实体域，它是 Dubbo 的核心模型，其它模型都向它靠扰，或转换成它，它代表一个可执行体，可向它发起 invoke 调用，它有可能是一个本地的实现，也可能是一个远程的实现，也可能一个集群实现。
Invocation 是会话域，它持有调用过程中的变量，比如方法名，参数等。



