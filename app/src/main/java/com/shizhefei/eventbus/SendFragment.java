package com.shizhefei.eventbus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.shizhefei.eventbus.demo.R;
import com.shizhefei.eventbus.events.IMessageEvent;

import java.util.Date;


/**
 * 发布事件
 * Created by LuckyJayce on 2016/7/25.
 */
public class SendFragment extends Fragment {
    private View sendActivityEventButton;
    private View sendActivityEventStickyButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_a, container, false);

        sendActivityEventButton = layout.findViewById(R.id.a_sendActivityEvent_button);
        sendActivityEventStickyButton = layout.findViewById(R.id.a_sendActivityEventSticky_button);

        sendActivityEventButton.setOnClickListener(onClickListener);
        sendActivityEventStickyButton.setOnClickListener(onClickListener);

        return layout;
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String time = String.valueOf(DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date()));
            if (v == sendActivityEventButton) {
                String message = getActivity() + " Activity EventHandler Test";
                EventBus.withActivity(getActivity()).get(IMessageEvent.class).onReceiveMessage(time, message);

            } else if (v == sendActivityEventStickyButton) {
                String message = getActivity() + " Activity EventHandler Test";
                //使用activity内部的EventHandler 进行发布事件，该事件通过相同的activity的 EventBus.withActivity(getActivity()).register();接收到
                EventBus.withActivity(getActivity()).get(IMessageEvent.class, true).onReceiveMessage(time, message);

            }
        }
    };
}
