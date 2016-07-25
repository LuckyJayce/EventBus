package com.shizhefei.eventbus.events;


import com.shizhefei.eventbus.IEvent;

public interface IAccountEvent extends IEvent {

    void onLogin(String name, String pass);

    void onLogout(String name);

}
