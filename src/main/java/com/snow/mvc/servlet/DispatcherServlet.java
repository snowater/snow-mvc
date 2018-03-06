/**
 * @(#) DispatcherServlet.java 2018/03/06
 * Copyright 2018 Snow.com, Inc. All rights reserved.
 */
package com.snow.mvc.servlet;

import com.snow.mvc.annotation.SnowAutowired;
import com.snow.mvc.annotation.SnowController;
import com.snow.mvc.annotation.SnowRequestMapping;
import com.snow.mvc.annotation.SnowRequestParam;
import com.snow.mvc.annotation.SnowService;
import com.snow.mvc.util.Play;
import com.snow.mvc.util.ToolUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
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
    private Map<String, HandlerModel> handlerMapping = new HashMap<>();

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

        // 4. 注入值
        doAutoWired();

        // 5.初始化HandlerMapping，建立url到method的映射关系
        initHandlerMapping();
    }

    private void doLoadConfig(String location) {
        // 将web.xml中的contextConfigLocation对应的value值的文件加载到流里面
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        // 用properties文件加载文件里的内容
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭输入流
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String pacakgeName) {
        // 将所有的.替换成/
        URL url = this.getClass().getClassLoader().getResource("/" + pacakgeName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        // 递归扫描所有的class文件
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                // 如果是目录，则递归目录的下一层
                doScanner(pacakgeName + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    // 不是类文件就忽略
                    continue;
                }

                // 类名结尾的.class应该去掉
                String className = pacakgeName + "." + file.getName().replaceAll(".class", "");

                // 判断是否被SnowService或者SnowController注解了，如果没有被注解，我们也不处理
                Class<?> clazz = null;
                try {
                    clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(SnowController.class) || clazz.isAnnotationPresent(SnowService.class)) {
                        // 将全限定名加入到classNames中
                        classNames.add(className);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {
        if (classNames == null || classNames.size() == 0) {
            return;
        }

        // 遍历所有需要被托管的类，并且实例化，最后存入到ioc容器中
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(SnowController.class)) {
                    // 是Controller
                    ioc.put(ToolUtils.toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                } else if (clazz.isAnnotationPresent(SnowService.class)) {
                    // 是Service
                    // 获取注解上的值
                    SnowService snowService = clazz.getAnnotation(SnowService.class);
                    String value = snowService.value();
                    if (!"".equals(value.trim())) {
                        // 如果有值，就以该值为key
                        ioc.put(value.trim(), clazz.newInstance());
                    } else {
                        // 没有值就用接口的首字母小写
                        // 获取该类实现的所有接口，比如FirstServiceImpl类实现了FirstService接口
                        Class[] interfaces = clazz.getInterfaces();
                        // 把所有实现的接口都注入上该类
                        for (Class inter : interfaces) {
                            ioc.put(ToolUtils.toLowerFirstWord(inter.getSimpleName()), clazz.newInstance());
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void doAutoWired() {
        if (ioc.isEmpty()) {
            return;
        }

        // 遍历所有被托管的对象，找出所有被SnowAutowired注解的属性
        // getFields()获得某个类的所有的public的字段，包括父类
        // getDeclaredFields()获得某个类的所有声明的字段，包括public、protected、private和默认权限的，但是不包括父类中声明的字段
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // entry.getValue()就是类对象实例
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(SnowAutowired.class)) {
                    // 如果不存在SnowAutowired注解则不需要注入处理
                    continue;
                }

                String beanName;
                // 获取SnowAutowired上面的值，例如@SnowAutowired("firstService")
                SnowAutowired snowAutowired = field.getAnnotation(SnowAutowired.class);
                String value = snowAutowired.value();
                if ("".equals(value)) {
                    // 如果没有值，则以field首字母小写作为beanName
                    beanName = ToolUtils.toLowerFirstWord(field.getType().getSimpleName());
                } else {
                    beanName = value;
                }

                // 将访问权限设置为不校验，以此访问私有化的属性，否则无法访问
                field.setAccessible(true);
                // 去ioc容器中根据beanName查找对应的实例对象
                if (ioc.containsKey(beanName)) {
                    try {
                        field.set(entry.getValue(), ioc.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        // 遍历托管的对象，寻找SnowController
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            // 只处理SnowController类，只有SnowController有SnowRequestMapping
            if (!clazz.isAnnotationPresent(SnowController.class)) {
                continue;
            }

            // 定义url，并初始化为"/"
            String url = "/";

            // 取到SnowController上的SnowRequestMapping的值
            if (clazz.isAnnotationPresent(SnowRequestMapping.class)) {
                SnowRequestMapping snowRequestMapping = clazz.getAnnotation(SnowRequestMapping.class);
                // 类上的SnowRequestMapping值作为基准值
                url += snowRequestMapping.value();
            }

            // 获取方法上的RequestMapping
            Method[] methods = clazz.getMethods();

            for (Method method : methods) {
                // 只处理SnowRequestMapping注解了的方法
                if (!method.isAnnotationPresent(SnowRequestMapping.class)) {
                    continue;
                }

                SnowRequestMapping snowRequestMapping = method.getAnnotation(SnowRequestMapping.class);
                // snowRequestMapping.value()即是SnowRequestMapping上注解的请求地址，不管用户写不写上"/"，我们都为用户补上
                String realUrl = url + "/" + snowRequestMapping.value();
                // 替换掉多余的/，将//替换为/
                realUrl = realUrl.replaceAll("/+", "/");

                // 获取所有的参数的注解，有几个参数就有几个annotation[]，为什么是数组呢？因为一个参数可以有多个注解......
                Annotation[][] annotations = method.getParameterAnnotations();
                // 由于后面的Method的invoke要传入所有参数的值的数组，所以需要保存各参数的位置
                /*  以get方法的这几个参数为例 @SnowRequestParam("name") String name, HttpServletRequest request, HttpServletResponse response
                    未来在invoke时，需要传入类似这样的一个数组["snow", request, response]。"snow"即是在Post方法中通过request.getParameter("name")来获取
                    Request和response这个简单，在post方法中直接就有。
                    所以我们需要保存@SnowRequestParam上的value值，和它的位置。譬如 name->0,只有拿到了这两个值，
                    才能将post中通过request.getParameter("name")得到的值放在参数数组的第0个位置。
                    同理，也需要保存request的位置1，response的位置2
                 */
                Map<String, Integer> paramMap = new HashMap<>();
                //获取方法里的所有参数的参数名（注意：此处使用了ASM.jar 版本为asm-3.3.1，需要在web-inf下建lib文件夹，引入asm-3.3.1.jar，自行下载）
                //如Controller的get方法，将得到如下数组["name", "request", "response"]
                String[] paramNames = Play.getMethodParameterNamesByAsm4(clazz, method);

                // 获取所有参数的类型，提取Request和Response的索引
                Class<?>[] paramTypes = method.getParameterTypes();

                for (int i = 0; i < annotations.length; i++) {
                    // 获取每个参数上的所有注解
                    Annotation[] anns = annotations[i];
                    if (anns.length == 0) {
                        // 如果没有注解，则是如String abc, Request request这种，没写注解的
                        // 如果没被SnowRequestParam注解
                        // 如果是Request或者Response，就直接用类名做key；如果是普通属性，就用属性名
                        Class<?> type = paramTypes[i];
                        if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                            paramMap.put(type.getName(), i);
                        } else {
                            //参数没写@RequestParam注解，只写了String name，那么通过java是无法获取到name这个属性名的
                            //通过上面asm获取的paramNames来映射
                            paramMap.put(paramNames[i], i);
                        }
                        continue;
                    }

                    // 有注解，就遍历每个参数上的所有注解
                    for (Annotation ann : anns) {
                        // 找到被SnowRequestMapping注解的参数，并取value值
                        if (ann.annotationType() == SnowRequestParam.class) {
                            // 也就是@SnowRequestParam("name")上的"name"
                            String paramName = ((SnowRequestParam)ann).value();
                            if (!"".equals(paramName.trim())) {
                                paramMap.put(paramName, i);
                            }
                        }
                    }
                }
                HandlerModel handlerModel = new HandlerModel(method, entry.getValue(), paramMap);
                handlerMapping.put(realUrl, handlerModel);
            }
        }
    }



    private class HandlerModel {
        Method method;
        Object object;
        Map<String, Integer> paramMap;

        public HandlerModel(Method method, Object object, Map<String, Integer> paramMap) {
            this.method = method;
            this.object = object;
            this.paramMap = paramMap;
        }
    }

}





















