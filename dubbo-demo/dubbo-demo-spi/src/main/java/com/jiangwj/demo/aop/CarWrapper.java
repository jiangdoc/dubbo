package com.jiangwj.demo.aop;

import com.alibaba.dubbo.common.URL;
import com.jiangwj.demo.spi.Car;

/**
 * spi 的AOP功能
 * @author jiangwenjie
 * @date 2021/5/14
 */
public class CarWrapper implements Car {

    private Car car;

    /**
     * 会使用构造方法去注入
     * @param car
     */
    public CarWrapper(Car car){
        this.car = car;
    }



    @Override
    public void sayColor(URL url) {
        System.out.println("wrapper before ...");
        car.sayColor(url);
        System.out.println("wrapper after ...");
    }
}
