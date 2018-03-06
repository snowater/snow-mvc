/**
 * @(#) SnowRequestMapping.java 2018/03/06
 * Copyright 2018 Snow.com, Inc. All rights reserved.
 */
package com.snow.mvc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author snow
 * @version 2018/03/06
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SnowRequestMapping {
    String value() default "";
}
