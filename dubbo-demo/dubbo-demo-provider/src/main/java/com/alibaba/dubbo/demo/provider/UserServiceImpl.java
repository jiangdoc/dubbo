package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.User;
import com.alibaba.dubbo.demo.UserService;

import java.util.Arrays;
import java.util.List;

/**
 * @author jiangwenjie
 * @date 2021/5/9
 */
public class UserServiceImpl implements UserService {
    @Override
    public List<User> slectUser() {
        return Arrays.asList(new User("明明",22),new User("lili",33));
    }
}
