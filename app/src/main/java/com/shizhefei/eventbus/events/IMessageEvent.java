package com.shizhefei.eventbus.events;


import com.shizhefei.eventbus.IEvent;

/**
 * Created by LuckyJayce on 2016/7/25.
 */
public interface IMessageEvent extends IEvent {
    void onReceiveMessage(String sendTime, String message);
}
