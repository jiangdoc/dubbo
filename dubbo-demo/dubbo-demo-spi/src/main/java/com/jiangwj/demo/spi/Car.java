package com.jiangwj.demo.spi;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Adaptive;
import com.alibaba.dubbo.common.extension.SPI;

/**
 * @author jiangwenjie
 * @date 2021/5/14
 */
// spi注解中的value值就是配置文件中对应的默认的extension
//@SPI("red")
@SPI
public interface Car {
    //@Adaptive("type")
    @Adaptive
    void sayColor(URL url);
}
