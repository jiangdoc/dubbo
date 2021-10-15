package com.jiangwj.demo.spi;

import com.alibaba.dubbo.common.URL;

/**
 * @author jiangwenjie
 * @date 2021/5/14
 */
public class WhiteCar implements Car {
    @Override
    public void sayColor() {
        System.out.println("color is white");
    }
}
