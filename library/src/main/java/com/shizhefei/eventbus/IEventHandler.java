package com.shizhefei.eventbus;

/**
 * 事件处理着
 * Created by LuckyJayce on 2016/7/26.
 */
public interface IEventHandler {
    /**
     * 获取动态代理实现的IEvent，用来发送事件
     *
     * @param eventClass 事件的class
     * @param <EVENT>    IEvent的class的泛型
     * @return
     */
    <EVENT extends IEvent> EVENT get(Class<EVENT> eventClass);

    /**
     * 获取动态代理实现的IEvent，用来发送事件
     *
     * @param eventClass 事件的class
     * @param sticky     是否粘性消息
     * @param <EVENT>    IEvent的class的泛型
     * @return
     */
    <EVENT extends IEvent> EVENT get(Class<EVENT> eventClass, boolean sticky);


    /**
     * 根据event移除掉对应的粘性消息
     *
     * @param eventClass 事件的class
     */
    void removeStickyEvent(Class<? extends IEvent> eventClass);

    /**
     * 移除掉所有的粘性消息
     */
    void removeAllStickyEvent();

    /**
     * 注册这个对象的所有event接口
     *
     * @param subscriber
     */

    void register(IEvent subscriber);

    /**
     * 注销掉这个对象的有event接口
     *
     * @param subscriber
     */

    void unregister(IEvent subscriber);

    /**
     * 是否订阅
     *
     * @param subscriber 订阅者
     * @return 是否订阅
     */
    boolean isRegister(IEvent subscriber);

}
