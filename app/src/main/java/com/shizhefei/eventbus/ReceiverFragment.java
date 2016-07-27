package com.shizhefei.eventbus;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shizhefei.eventbus.demo.R;
import com.shizhefei.eventbus.events.IMessageEvent;

/**
 * 接受消息
 * Created by LuckyJayce on 2016/7/25.
 */
@Subscribe(threadMode = Subscribe.MAIN, sticky = true)
public class ReceiverFragment extends Fragment implements IMessageEvent {

    private TextView messageTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_receiver, container, false);

        messageTextView = (TextView) layout.findViewById(R.id.receiver_message_textView);

        EventBus.withActivity(getActivity()).register(this);

        return layout;
    }

    @Override
    public void onReceiveMessage(String sendTime, String message) {
        StringBuilder builder = new StringBuilder(messageTextView.getText());
        builder.append(sendTime).append("  ");
        builder.append(message).append("\n\n");
        messageTextView.setText(builder);
    }

    @Override
    public void onDestroyView() {
        EventBus.withActivity(getActivity()).unregister(this);
        super.onDestroyView();
    }
}
