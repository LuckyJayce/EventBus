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
 * Created by LuckyJayce on 2016/7/25.
 */
public class SendFragment extends Fragment {
    private View sendButton;
    private View sendStickyButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_a, container, false);

        sendButton = layout.findViewById(R.id.a_send_button);
        sendStickyButton = layout.findViewById(R.id.a_sendSticky_button);

        sendButton.setOnClickListener(onClickListener);
        sendStickyButton.setOnClickListener(onClickListener);

        return layout;
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String time = String.valueOf(DateFormat.format("yyyy-MM-dd kk:mm:ss", new Date()));
            if (v == sendButton) {
                String message = "Test";
                EventBus.get(IMessageEvent.class).onReceiveMessage(time, message);
            } else if (v == sendStickyButton) {
                String message = "Sticky Test";
                EventBus.get(IMessageEvent.class, true).onReceiveMessage(time, message);
            }
        }
    };
}
