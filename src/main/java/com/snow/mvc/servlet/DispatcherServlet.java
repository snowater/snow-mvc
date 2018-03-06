/**
 * @(#) DispatcherServlet.java 2018/03/06
 * Copyright 2018 Snow.com, Inc. All rights reserved.
 */
package com.snow.mvc.servlet;

import com.snow.mvc.util.ToolUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author snow
 * @version 2018/03/06
 */
public class DispatcherServlet extends HttpServlet {
    private Properties properties = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ToolUtils.responseWriter(resp, "请求到了");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        // 1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        // 2.初始化所有相关联的类，扫描用户设定的包下面所有的类
        doScanner(properties.getProperty("scanPackage"));

        // 3.拿到扫描到的类，通过反射机制实例化，并且放到ioc容器中（k - v : beanName - bean） beanName默认是类名首字母小写
        doInstance();

        // 4.初始化HandlerMapping（将url和method对应上）
        initHandlerMapping();
    }

    private void doLoadConfig(String location) {

    }

    private void doScanner(String pacakgeName) {

    }

    private void doInstance() {

    }

    private void initHandlerMapping() {

    }


}





















