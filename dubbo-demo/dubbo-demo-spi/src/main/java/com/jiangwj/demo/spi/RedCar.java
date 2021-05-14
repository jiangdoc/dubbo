package com.jiangwj.demo.spi;

import com.alibaba.dubbo.common.URL;

/**
 * @author jiangwenjie
 * @date 2021/5/14
 */
public class RedCar implements Car {
    @Override
    public void sayColor(URL url) {
        System.out.println("color is red");
    }
}
