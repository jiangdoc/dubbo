/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.rpc.proxy.javassist;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.bytecode.Proxy;
import com.alibaba.dubbo.common.bytecode.Wrapper;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyFactory;
import com.alibaba.dubbo.rpc.proxy.AbstractProxyInvoker;
import com.alibaba.dubbo.rpc.proxy.InvokerInvocationHandler;

/**
 * JavaassistRpcProxyFactory
 */
public class JavassistProxyFactory extends AbstractProxyFactory {
    /**
     *以DemoService为例，生成的代理对象
     * package org.apache.dubbo.common.bytecode;
     * public class proxy0 implements ClassGenerator.DC, EchoService, DemoService {
     *     // 方法数组
     *     public static Method[] methods;
     *     private InvocationHandler handler;
     *     public proxy0(InvocationHandler invocationHandler) {
     *         this.handler = invocationHandler;
     *     }
     *     public proxy0() {
     *     }
     *     public String sayHello(String string) {
     *         // 将参数存储到 Object 数组中
     *         Object[] arrobject = new Object[]{string};
     *         // 调用 InvocationHandler 实现类的 invoke 方法得到调用结果
     *         Object object = this.handler.invoke(this, methods[0], arrobject);
     *         // 返回调用结果
     *         return (String)object;
     *     }
     *     // 回声测试方法
             *     public Object $echo(Object object) {
     *         Object[] arrobject = new Object[]{object};
     *         Object object2 = this.handler.invoke(this, methods[1], arrobject);
     *         return object2;
     *     }
     * }
     * **/
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
        // 生成 Proxy 子类（Proxy 是抽象类）。并调用 Proxy 子类的 newInstance 方法创建 Proxy 实例
        /**
         * 1.Proxy.getProxy(interfaces):执行此方法会生成一个interfaces的实现类，也就是代理类，这个类有有参构造方法，参数是InvocationHandler
         * 2.newInstance(new InvokerInvocationHandler(invoker))：会new一个InvokerInvocationHandler对象作为参数，调用构造函数实例化上面实现类。
         */
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }

    /**
     *
     * @param proxy
     * @param type
     * @param url
     * @param <T>
     * @return
     * // Wrapper0 是在运行时生成的，大家可使用 Arthas 进行反编译
     * public class Wrapper0 extends Wrapper implements ClassGenerator.DC {
     *     public static String[] pns;
     *     public static Map pts;
     *     public static String[] mns;
     *     public static String[] dmns;
     *     public static Class[] mts0;
     *     // 省略其他方法
     *     public Object invokeMethod(Object object, String string, Class[] arrclass, Object[] arrobject) throws InvocationTargetException {
     *         DemoService demoService;
     *         try {
     *             // 类型转换
     *             demoService = (DemoService)object;
     *         }
     *         catch (Throwable throwable) {
     *             throw new IllegalArgumentException(throwable);
     *         }
     *         try {
     *             // 根据方法名调用指定的方法
     *             if ("sayHello".equals(string) && arrclass.length == 1) {
     *                 return demoService.sayHello((String)arrobject[0]);
     *             }
     *         }
     *         catch (Throwable throwable) {
     *             throw new InvocationTargetException(throwable);
     *         }
     *         throw new NoSuchMethodException(new StringBuffer().append("Not found method \"").append(string).append("\" in class com.alibaba.dubbo.demo.DemoService.").toString());
     *     }
     * }
     */
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
        // TODO Wrapper cannot handle this scenario correctly: the classname contains '$'
        // 为目标类创建 Wrapper
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        // 创建匿名 Invoker 类对象，并实现 doInvoke 方法。
        return new AbstractProxyInvoker<T>(proxy, type, url) {
            @Override
            protected Object doInvoke(T proxy, String methodName,
                                      Class<?>[] parameterTypes,
                                      Object[] arguments) throws Throwable {
                // 调用 Wrapper 的 invokeMethod 方法，invokeMethod 最终会调用目标方法
                return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
            }
        };
    }

}