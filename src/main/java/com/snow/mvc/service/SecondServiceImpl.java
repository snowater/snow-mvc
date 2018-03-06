/**
 * @(#) SecondServiceImpl.java 2018/03/06
 * Copyright 2018 Snow.com, Inc. All rights reserved.
 */
package com.snow.mvc.service;

import com.snow.mvc.annotation.SnowService;

/**
 * @author snow
 * @version 2018/03/06
 */
@SnowService
public class SecondServiceImpl implements SecondService {

    @Override
    public String get() {
        return "this is first service get method!";
    }

    @Override
    public String udpate(String name) {
        return "this is first service update method and your parameter is " + name;
    }
}
