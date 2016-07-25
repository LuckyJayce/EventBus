/**
 * Copyright 2015 shizhefei（LuckyJayce）
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shizhefei.eventbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 标记事件的调用环境,可以注解在实现接口的方法上，也可以注解在类上。
 * 两个同时注解的话，方法的注解权限高于类的
 * Created by LuckyJayce on 2016/7/23.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {
    /**
     * 发事件后直接调用，收发在同一线程中
     */
    int POSTING = 0;
    /**
     * 无论之前什么线程发送，都在主线程接收
     */
    int MAIN = 1;
    /**
     * 在后台线程接收，如果发送的线程不是主线程直接调用这个，如果是主线程就开个线程执行这个方法
     */
    int BACKGROUND = 2;
    /**
     * 无论如何都会在开个线程执行（会有个线程池，不一定是真的开个线程，可能取线程池中空闲的线程）
     */
    int ASYNC = 3;

    /**
     * 执行在哪个线程
     */
    int threadMode() default POSTING;

    /**
     * 是否会被粘性消息调用到
     */
    boolean sticky() default false;
}
