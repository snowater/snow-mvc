/**
 * @(#) FirstServiceImpl.java 2018/03/06
 * Copyright 2018 Snow.com, Inc. All rights reserved.
 */
package com.snow.mvc.service;

import com.snow.mvc.annotation.SnowService;

/**
 * @author snow
 * @version 2018/03/06
 */
@SnowService
public class FirstServiceImpl implements FirstService {

    @Override
    public String get(String name) {
        return "this is first service get method and your paramater is " + name;
    }

    @Override
    public String update(String name) {
        return "this is first service update method and your parameter is " + name;
    }
}
