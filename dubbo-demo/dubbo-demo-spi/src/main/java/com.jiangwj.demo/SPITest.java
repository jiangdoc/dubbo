package com.jiangwj.demo;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.jiangwj.demo.ioc.Driver;
import com.jiangwj.demo.spi.Car;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jiangwenjie
 * @date 2021/5/14
 */
public class SPITest {
    public static void main(String[] args) {

        /*ExtensionLoader<Car> extensionLoader = ExtensionLoader.getExtensionLoader(Car.class);

        //@SPI("red")
        //spi注解中的value值就是配置文件中对应的默认的extension
        *//*final Car defaultExtension = extensionLoader.getDefaultExtension();
        defaultExtension.sayColor();*//*

        // AOP
        final Car red = extensionLoader.getExtension("red");
        red.sayColor();*/

        ExtensionLoader<Driver> extensionLoader = ExtensionLoader.getExtensionLoader(Driver.class);
        final Driver defaultExtension = extensionLoader.getDefaultExtension();
        Map<String,String> map = new HashMap<String, String>();
        map.put("type","red");
        URL url = new URL("","",0,map);
        defaultExtension.driverCar(url);
    }
}
