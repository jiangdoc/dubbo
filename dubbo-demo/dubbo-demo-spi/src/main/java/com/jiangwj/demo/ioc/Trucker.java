package com.jiangwj.demo.ioc;

import com.alibaba.dubbo.common.URL;
import com.jiangwj.demo.spi.Car;

/**
 * @author jiangwenjie
 * @date 2021/5/14
 */
public class Trucker implements Driver {
    private Car car;

    public void setCar(Car car) {
        this.car = car;
    }

    @Override
    public void driverCar(URL url) {
        car.sayColor(url);
        System.out.println("trucker driver car");
    }
}
