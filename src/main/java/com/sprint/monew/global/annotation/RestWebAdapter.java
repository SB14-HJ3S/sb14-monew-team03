package com.sprint.monew.global.annotation;

import java.lang.annotation.*;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
@RequestMapping
public @interface RestWebAdapter {

    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String path() default "";
}
