package com.shizhefei.eventbus.events;

import com.shizhefei.eventbus.IEvent;

public interface IAddEvent extends IEvent {
    void onAdd(String file);
}