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

import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 整个思路就是观察者模式
 * 通过动态代理实现IEvent，一旦有方法被调用就通知注册的观察者
 */
class EventHandler implements IEventHandler {

    /**
     * 通过动态代理实现的IEvent
     */
    private Map<Class<? extends IEvent>, IEvent> eventProxy = new ConcurrentHashMap<>();
    /**
     * 通过动态代理实现的IEvent，多了发送粘性消息功能
     */
    private Map<Class<? extends IEvent>, IEvent> eventStickyProxy = new ConcurrentHashMap<>();
    /**
     * 注册的观察者
     */
    private Map<IEvent, Register> registers = new ConcurrentHashMap<>();
    /**
     * stickyDataMap 的key是事件的class，value是执行的方法和参数的集合.
     * 粘性消息包含多个事件的粘性消息，一个事件对应多个方法
     */
    private Map<Class<? extends IEvent>, LinkedHashMap<Method, Object[]>> stickyDataMap = new HashMap<>();
    /**
     * 粘性消息的线程锁对象
     */
    private final Object stickyDataLock = new Object();
    /**
     * 主线程通知的handle
     */
    private Handler handler;

    /**
     * 下面代码来自AsyTask的线程池 Executor
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "EventHandler #" + mCount.getAndIncrement());
        }
    };
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>();
    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    public EventHandler() {
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * 获取动态代理实现的IEvent，用来发送事件
     *
     * @param eventClass
     * @param <EVENT>
     * @return
     */
    @Override
    public <EVENT extends IEvent> EVENT get(Class<EVENT> eventClass) {
        return get(eventClass, false);
    }

    /**
     * 根据event移除掉对应的粘性消息
     *
     * @param eventClass
     */
    @Override
    public void removeStickyEvent(Class<? extends IEvent> eventClass) {
        synchronized (stickyDataLock) {
            stickyDataMap.remove(eventClass);
        }
    }

    /**
     * 移除掉所有的粘性消息
     */
    @Override
    public void removeAllStickyEvent() {
        synchronized (stickyDataLock) {
            stickyDataMap.clear();
        }
    }

    /**
     * 注册这个对象的所有event接口
     *
     * @param subscriber
     */
    @Override
    public void register(IEvent subscriber) {
        //如果已经注册了就不再注册
        if (registers.containsKey(subscriber)) {
            return;
        }
        //获取subscriber实现的所有事件接口
        ArrayList<Class<? extends IEvent>> eventClassList = Util.getInterfaces(subscriber);
        //获取subscriber的Subscribe注解
        Subscribe subscribeAnnotation = subscriber.getClass().getAnnotation(Subscribe.class);
        SubscribeInfo subscribeInfo;
        if (subscribeAnnotation == null) {
            subscribeInfo = SubscribeInfo.NONE;
        } else {
            subscribeInfo = new SubscribeInfo(subscribeAnnotation.sticky(), subscribeAnnotation.threadMode());
        }
        Register register = new Register(subscriber, eventClassList, subscribeInfo);
        //添加到map中
        registers.put(subscriber, register);

        //发送粘性消息
        synchronized (stickyDataLock) {
            //stickyDataMap 的key是事件的class，value是执行的方法和参数的集合.
            //粘性消息包含多个事件的粘性消息，一个事件对应多个方法
            for (Entry<Class<? extends IEvent>, LinkedHashMap<Method, Object[]>> entry : stickyDataMap.entrySet()) {
                //实现的所有事件是否包含，粘性消息里面的事件class
                if (eventClassList.contains(entry.getKey())) {
                    LinkedHashMap<Method, Object[]> inMap = entry.getValue();
                    //循环单个事件class对应的有粘性消息的方法
                    for (Entry<Method, Object[]> in : inMap.entrySet()) {
                        Method method = in.getKey();
                        Object[] args = in.getValue();
                        //获取注解信息
                        SubscribeInfo info = register.getSubscribeInfo(method);
                        //如果没有注解，就使用类的注解信息
                        if (info == SubscribeInfo.NONE) {
                            info = subscribeInfo;
                        }
                        //是否接收粘性消息
                        if (info.sticky) {
                            //执行通知订阅者，对应的实现事件的方法
                            invoke(register.subscriber, register, method, args);
                        }
                    }
                }
            }
        }
    }

    /**
     * 取消订阅事件
     *
     * @param subscriber
     */
    @Override
    public void unregister(IEvent subscriber) {
        Register register = registers.remove(subscriber);
        register.isRegister = false;
    }


    /**
     * 是否订阅
     *
     * @param subscriber 订阅者
     * @return 是否订阅
     */
    @Override
    public boolean isRegister(IEvent subscriber) {
        return registers.containsKey(subscriber);
    }

    @Override
    public <EVENT extends IEvent> EVENT get(Class<EVENT> eventClass, boolean sticky) {
        //判断这个eventClass是否合法
        Util.validateServiceInterface(eventClass);
        Map<Class<? extends IEvent>, IEvent> map;
        //是否发送粘性消息，存在不同的map中
        if (sticky) {
            map = eventStickyProxy;
        } else {
            map = eventProxy;
        }
        IEvent event = map.get(eventClass);
        if (event == null) {
            synchronized (EventHandler.class) {
                event = map.get(eventClass);
                if (event == null) {
                    if (sticky) {
                        event = (EVENT) Proxy.newProxyInstance(eventClass.getClassLoader(), new Class<?>[]{eventClass}, new StickyProxyInvocationHandler(eventClass));
                    } else {
                        event = (EVENT) Proxy.newProxyInstance(eventClass.getClassLoader(), new Class<?>[]{eventClass}, new ProxyInvocationHandler(eventClass));
                    }
                    map.put(eventClass, event);
                }
            }
        }
        return (EVENT) event;
    }


    /**
     * 粘性消息的发送的实现
     */
    private class StickyProxyInvocationHandler extends ProxyInvocationHandler {
        public StickyProxyInvocationHandler(Class<? extends IEvent> service) {
            super(service);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
            //一旦被调用就存消息到stickyDataMap中
            synchronized (stickyDataLock) {
                LinkedHashMap<Method, Object[]> invokeDataList = stickyDataMap.get(service);
                if (invokeDataList == null) {
                    invokeDataList = new LinkedHashMap<>();
                }
                invokeDataList.put(method, args);
                stickyDataMap.put(service, invokeDataList);
            }
            return super.invoke(proxy, method, args);
        }
    }

    /**
     * 消息发送的实现类
     */
    private class ProxyInvocationHandler implements InvocationHandler {
        Class<? extends IEvent> service;

        public ProxyInvocationHandler(Class<? extends IEvent> service) {
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
            for (Entry<IEvent, Register> entry : registers.entrySet()) {
                Register register = entry.getValue();
                if (register.eventClassList.contains(service)) {
                    EventHandler.this.invoke(register.subscriber, register, method, args);
                }
            }
            return null;
        }
    }

    private void invoke(IEvent subscriber, Register register, Method method, Object[] args) {
        try {
            boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
            SubscribeInfo subscribeInfo = register.getSubscribeInfo(method);
            switch (subscribeInfo.threadMode) {
                case Subscribe.POSTING:
                default:
                    method.invoke(subscriber, args);
                    break;
                case Subscribe.MAIN:
                    if (isMainThread) {
                        method.invoke(subscriber, args);
                    } else {
                        handler.post(new InvokeRunnable(register, method, args));
                    }
                    break;
                case Subscribe.BACKGROUND:
                    if (isMainThread) {
                        THREAD_POOL_EXECUTOR.execute(new InvokeRunnable(register, method, args));
                    } else {
                        method.invoke(subscriber, args);
                    }
                    break;
                case Subscribe.ASYNC:
                    THREAD_POOL_EXECUTOR.execute(new InvokeRunnable(register, method, args));
                    break;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行方法的Runnable
     */
    private static class InvokeRunnable implements Runnable {
        //执行的方法
        private final Method method;
        //参数
        private final Object[] args;
        //注册者，这里用弱引用
        private WeakReference<Register> registerWeakReference;

        public InvokeRunnable(Register register, Method method, Object[] args) {
            registerWeakReference = new WeakReference<>(register);
            this.method = method;
            this.args = args;
        }

        @Override
        public void run() {
            try {
                Register register = registerWeakReference.get();
                if (register != null && register.isRegister) {
                    method.invoke(register.subscriber, args);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class Register {
        /***/
        IEvent subscriber;
        /**
         * subscriber 实现的所有事件
         */
        ArrayList<Class<? extends IEvent>> eventClassList;
        /**
         * 方法的信息
         */
        Map<Method, SubscribeInfo> methodInfoMap = new ConcurrentHashMap<>();
        volatile boolean isRegister;

        public Register(IEvent subscriber, ArrayList<Class<? extends IEvent>> eventClassList, SubscribeInfo subscribeInfo) {
            this.subscriber = subscriber;
            this.eventClassList = eventClassList;
            this.isRegister = true;
        }

        /**
         * 获取方法的注解信息，信息有，在哪个线程执行threadMode，是否接收粘性消息sticky
         *
         * @param proxyMethod
         * @return
         */
        private SubscribeInfo getSubscribeInfo(Method proxyMethod) {
            SubscribeInfo subscribeInfo = methodInfoMap.get(proxyMethod);
            if (subscribeInfo == null) {
                synchronized (Register.this) {
                    subscribeInfo = methodInfoMap.get(proxyMethod);
                    if (subscribeInfo == null) {
                        try {
                            Method rm = subscriber.getClass().getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
                            Subscribe subscribeAnnotation = rm.getAnnotation(Subscribe.class);
                            if (subscribeAnnotation == null) {
                                subscribeInfo = SubscribeInfo.NONE;
                            } else {
                                subscribeInfo = new SubscribeInfo(subscribeAnnotation.sticky(), subscribeAnnotation.threadMode());
                            }
                            methodInfoMap.put(proxyMethod, subscribeInfo);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return subscribeInfo;
        }
    }

    private static class SubscribeInfo {
        /**
         * 是否接收粘性消息sticky
         */
        boolean sticky;
        /**
         * 在哪个线程执行threadMode
         */
        int threadMode;

        private SubscribeInfo() {
            sticky = false;
            threadMode = Subscribe.POSTING;
        }

        public SubscribeInfo(boolean sticky, int threadMode) {
            this.sticky = sticky;
            this.threadMode = threadMode;
        }

        public static final SubscribeInfo NONE = new SubscribeInfo();
    }
}
