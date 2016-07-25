package com.shizhefei.eventbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shizhefei.eventbus.demo.R;

import java.util.ArrayList;
import java.util.List;

public class ThreadTestActivity extends AppCompatActivity {

    private View updateButton;
    private View sleepButton;
    private RecyclerView recyclerView;
    private MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);

        updateButton = findViewById(R.id.work_doUpdate_button);
        sleepButton = findViewById(R.id.work_doSleep_button);
        recyclerView = (RecyclerView) findViewById(R.id.work_recyclerView);


        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter = new MyAdapter());

        updateButton.setOnClickListener(onClickListener);
        sleepButton.setOnClickListener(onClickListener);

        EventBus.register(iWorkEvent);
        EventBus.register(updateEvent);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.unregister(iWorkEvent);
        EventBus.unregister(updateEvent);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        private int mIndex;

        @Override
        public void onClick(View v) {
            if (v == sleepButton) {
                adapter.addItem("click work index:" + mIndex);
                EventBus.get(IWorkEvent.class).onDoWork(mIndex);
                mIndex++;
            } else if (v == updateButton) {
                EventBus.get(IUpdateEvent.class).onUpdate();
            }
        }
    };

    private IUpdateEvent updateEvent = new IUpdateEvent() {
        @Override
        public void onUpdate() {
            adapter.addItem("onUpdate");
        }
    };

    private IWorkEvent iWorkEvent = new IWorkEvent() {
        @Subscribe(threadMode = Subscribe.ASYNC)
        @Override
        public void onDoWork(final int index) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.addItem("start work index:" + index);
                }
            });
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.addItem("end work index:" + index);
                }
            });
        }
    };


    private interface IWorkEvent extends IEvent {
        void onDoWork(int index);
    }

    private interface IUpdateEvent extends IEvent {
        void onUpdate();
    }


    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<String> items = new ArrayList<>();

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View textView = getLayoutInflater().inflate(R.layout.item, parent, false);
            return new RecyclerView.ViewHolder(textView) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextView textView = (TextView) holder.itemView;
            textView.setText(items.get(position));
        }

        public void addItem(String item) {
            items.add(item);
            notifyItemChanged(items.size() - 1);
            recyclerView.scrollToPosition(items.size() - 1);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}
