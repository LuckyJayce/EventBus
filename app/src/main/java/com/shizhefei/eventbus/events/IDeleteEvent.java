package com.shizhefei.eventbus.events;


import com.shizhefei.eventbus.IEvent;

public interface IDeleteEvent extends IEvent {
    public void onDelete(String file);
}