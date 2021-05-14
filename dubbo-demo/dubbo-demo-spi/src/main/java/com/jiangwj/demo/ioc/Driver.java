package com.jiangwj.demo.ioc;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author jiangwenjie
 * @date 2021/5/14
 */
@SPI("trucker")
public interface Driver {
    void driverCar(URL url);
}
