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

class EventBusImp {

    private Map<Class<IEvent>, IEvent> eventProxy = new ConcurrentHashMap<>();
    private Map<Class<IEvent>, IEvent> eventStickyProxy = new ConcurrentHashMap<>();
    private Map<IEvent, Register> registers = new ConcurrentHashMap<>();
    private Map<Class<? extends IEvent>, LinkedHashMap<Method, Object[]>> stickyDataMap = new HashMap<>();
    private final Object stickyDataLock = new Object();
    private Handler handler;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "EventBusImp #" + mCount.getAndIncrement());
        }
    };
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);
    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    public EventBusImp() {
        handler = new Handler(Looper.getMainLooper());
    }

    public <EVENT extends IEvent> EVENT get(Class<EVENT> service) {
        return get(service, false);
    }

    public void removeStickyEvent(Class<? extends IEvent> service) {
        synchronized (stickyDataLock) {
            stickyDataMap.remove(service);
        }
    }

    public void removeAllStickyEvent() {
        synchronized (stickyDataLock) {
            stickyDataMap.clear();
        }
    }

    /**
     * 注册这个对象的所有event接口
     *
     * @param event
     */

    public void register(IEvent event) {
        if (registers.containsKey(event)) {
            return;
        }
        ArrayList<Class<? extends IEvent>> eventClass = Util.getInterfaces(event);
        Subscribe subscribeAnnotation = event.getClass().getAnnotation(Subscribe.class);
        SubscribeInfo subscribeInfo;
        if (subscribeAnnotation == null) {
            subscribeInfo = SubscribeInfo.NONE;
        } else {
            subscribeInfo = new SubscribeInfo(subscribeAnnotation.sticky(), subscribeAnnotation.threadMode());
        }
        Register register = new Register(event, eventClass, subscribeInfo);
        registers.put(event, register);

        synchronized (stickyDataLock) {
            for (Entry<Class<? extends IEvent>, LinkedHashMap<Method, Object[]>> entry : stickyDataMap.entrySet()) {
                if (eventClass.contains(entry.getKey())) {
                    LinkedHashMap<Method, Object[]> inMap = entry.getValue();
                    for (Entry<Method, Object[]> in : inMap.entrySet()) {
                        Method method = in.getKey();
                        Object[] args = in.getValue();
                        SubscribeInfo info = register.getSubscribeInfo(method);
                        if (info == SubscribeInfo.NONE) {
                            info = subscribeInfo;
                        }
                        if (info.sticky) {
                            invoke(register.target, register, method, args);
                        }
                    }
                }
            }
        }
    }


    /**
     * 注销掉这个对象的有event接口
     *
     * @param event
     */

    public void unregister(IEvent event) {
        Register register = registers.remove(event);
        register.isRegister = false;
    }

    public boolean isRegister(IEvent event) {
        return registers.containsKey(event);
    }


    public <EVENT extends IEvent> EVENT get(Class<EVENT> service, boolean sticky) {
        Util.validateServiceInterface(service);
        Map<Class<IEvent>, IEvent> map;
        if (sticky) {
            map = eventStickyProxy;
        } else {
            map = eventProxy;
        }
        IEvent event = map.get(service);
        if (event == null) {
            synchronized (EventBusImp.class) {
                event = map.get(service);
                if (event == null) {
                    if (sticky) {
                        event = (EVENT) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service}, new StickyProxyInvocationHandler(service));
                    } else {
                        event = (EVENT) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service}, new ProxyInvocationHandler(service));
                    }
                }
            }
        }
        return (EVENT) event;
    }


    private class StickyProxyInvocationHandler extends ProxyInvocationHandler {
        public StickyProxyInvocationHandler(Class<? extends IEvent> service) {
            super(service);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
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


    private class ProxyInvocationHandler implements InvocationHandler {
        Class<? extends IEvent> service;

        public ProxyInvocationHandler(Class<? extends IEvent> service) {
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
            for (Entry<IEvent, Register> entry : registers.entrySet()) {
                Register register = entry.getValue();
                if (register.eventClass.contains(service)) {
                    EventBusImp.this.invoke(register.target, register, method, args);
                }
            }
            return null;
        }
    }

    private void invoke(IEvent target, Register register, Method method, Object[] args) {
        try {
            boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
            SubscribeInfo subscribeInfo = register.getSubscribeInfo(method);
            switch (subscribeInfo.threadMode) {
                case Subscribe.POSTING:
                default:
                    method.invoke(target, args);
                    break;
                case Subscribe.MAIN:
                    if (isMainThread) {
                        method.invoke(target, args);
                    } else {
                        handler.post(new InvokeRun(register, method, args));
                    }
                    break;
                case Subscribe.BACKGROUND:
                    if (isMainThread) {
                        THREAD_POOL_EXECUTOR.execute(new InvokeRun(register, method, args));
                    } else {
                        method.invoke(target, args);
                    }
                    break;
                case Subscribe.ASYNC:
                    THREAD_POOL_EXECUTOR.execute(new InvokeRun(register, method, args));
                    break;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class InvokeData {
        Method method;
        Object[] args;

        public InvokeData(Method method, Object[] args) {
            this.method = method;
            this.args = args;
        }
    }

    private static class InvokeRun implements Runnable {

        private final Method method;
        private final Object[] args;
        private WeakReference<Register> registerWeakReference;

        public InvokeRun(Register register, Method method, Object[] args) {
            registerWeakReference = new WeakReference<>(register);
            this.method = method;
            this.args = args;
        }

        @Override
        public void run() {
            try {
                Register register = registerWeakReference.get();
                if (register != null && register.isRegister) {
                    method.invoke(register.target, args);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class Register {
        IEvent target;
        ArrayList<Class<? extends IEvent>> eventClass;
        Map<Method, SubscribeInfo> methodInfoMap = new ConcurrentHashMap<>();
        volatile boolean isRegister;

        public Register(IEvent target, ArrayList<Class<? extends IEvent>> eventClass, SubscribeInfo subscribeInfo) {
            this.target = target;
            this.eventClass = eventClass;
            this.isRegister = true;
        }

        private SubscribeInfo getSubscribeInfo(Method proxyMethod) {
            SubscribeInfo subscribeInfo = methodInfoMap.get(proxyMethod);
            if (subscribeInfo == null) {
                synchronized (Register.this) {
                    subscribeInfo = methodInfoMap.get(proxyMethod);
                    if (subscribeInfo == null) {
                        try {
                            Method rm = target.getClass().getMethod(proxyMethod.getName(), proxyMethod.getParameterTypes());
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
        boolean sticky;
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
