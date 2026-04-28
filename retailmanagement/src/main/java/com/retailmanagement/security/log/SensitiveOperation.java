package com.retailmanagement.security.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveOperation {
    ActionType action();

    String entity() default "";

    SeverityLevel severity() default SeverityLevel.MEDIUM;

    String description() default "";
}
