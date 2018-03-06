/**
 * @(#) ToolUtils.java 2018/03/06
 * Copyright 2018 Snow.com, Inc. All rights reserved.
 */
package com.snow.mvc.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author snow
 * @version 2018/03/06
 */
public class ToolUtils {

    public static void responseWriter(HttpServletResponse response, String str) {
        try {
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write(str);
        } catch (IOException e) {
            System.out.println("response write failed!" + e);
        }
    }

    /**
     * 把字符串的首字母小写
     * @param name
     * @return
     */
    public static String toLowerFirstWord(String name){
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

}
