package com.jiangwj.demo.spi;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class Car$Adaptive implements com.jiangwj.demo.spi.Car {
    public void sayColor(com.alibaba.dubbo.common.URL arg0) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("car");
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.jiangwj.demo.spi.Car) name from url(" + url.toString() + ") use keys([car])");
        com.jiangwj.demo.spi.Car extension = (com.jiangwj.demo.spi.Car) ExtensionLoader.getExtensionLoader(com.jiangwj.demo.spi.Car.class).getExtension(extName);
        extension.sayColor(arg0);
    }
}