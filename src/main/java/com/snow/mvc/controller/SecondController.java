/**
 * @(#) SecondController.java 2018/03/06
 * Copyright 2018 Snow.com, Inc. All rights reserved.
 */
package com.snow.mvc.controller;

import com.snow.mvc.annotation.SnowAutowired;
import com.snow.mvc.annotation.SnowController;
import com.snow.mvc.annotation.SnowRequestMapping;
import com.snow.mvc.annotation.SnowRequestParam;
import com.snow.mvc.service.SecondService;
import com.snow.mvc.util.ToolUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author snow
 * @version 2018/03/06
 */
@SnowController
@SnowRequestMapping(value = "/snow")
public class SecondController {

    @SnowAutowired
    private SecondService secondService;

    @SnowRequestMapping(value = "/get.json")
    public void get(@SnowRequestParam("name") String name, HttpServletRequest request, HttpServletResponse response) {
        ToolUtils.responseWriter(response, name);
    }

    @SnowRequestMapping(value = "/update.json")
    public void update(@SnowRequestParam("name") String name, HttpServletRequest request, HttpServletResponse response) {
        ToolUtils.responseWriter(response, name + "update");
    }
}
