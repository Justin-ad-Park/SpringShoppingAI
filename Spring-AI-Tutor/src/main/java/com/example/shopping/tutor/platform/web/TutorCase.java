package com.example.shopping.tutor.platform.web;

import java.lang.annotation.*;

/** 학습 목록은 이 명시적 허용 목록에서만 생성한다. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TutorCase {
    Class<?> lesson();

    String learningMethod();

    String id();

    String title();

    String sample();

    String goal();

    boolean stream() default false;

    String requires() default "";
}
