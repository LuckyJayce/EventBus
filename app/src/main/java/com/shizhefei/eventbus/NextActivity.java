package com.shizhefei.eventbus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ToggleButton;

import com.shizhefei.eventbus.demo.R;
import com.shizhefei.eventbus.events.IAccountEvent;
import com.shizhefei.eventbus.events.IAddEvent;
import com.shizhefei.eventbus.events.IDeleteEvent;
import com.shizhefei.eventbus.events.IUpdateDataEvent;

public class NextActivity extends AppCompatActivity {

    private View loginButton;
    private View logoutButton;
    private View deleteButton;
    private View addButton;
    private ToggleButton toggleButton;
    private View updateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);
        loginButton = findViewById(R.id.login_button);
        logoutButton = findViewById(R.id.logout_button);
        deleteButton = findViewById(R.id.delete_button);
        addButton = findViewById(R.id.add_button);
        updateButton = findViewById(R.id.update_button);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);


        loginButton.setOnClickListener(onClickListener);
        logoutButton.setOnClickListener(onClickListener);
        deleteButton.setOnClickListener(onClickListener);
        addButton.setOnClickListener(onClickListener);
        updateButton.setOnClickListener(onClickListener);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v == loginButton) {
                if (toggleButton.isChecked()) {
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            EventBus.get(IAccountEvent.class).onLogin("LuckyJayce", "123");
                        }
                    }.start();
                } else {
                    EventBus.get(IAccountEvent.class).onLogin("LuckyJayce", "123");
                }
            } else if (v == logoutButton) {
                if (toggleButton.isChecked()) {
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            EventBus.get(IAccountEvent.class).onLogout("LuckyJayce");
                        }
                    }.start();
                } else {
                    EventBus.get(IAccountEvent.class).onLogout("LuckyJayce");
                }
            } else if (v == addButton) {
                if (toggleButton.isChecked()) {
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            EventBus.get(IAddEvent.class).onAdd("java编程思想");
                        }
                    }.start();
                } else {
                    EventBus.get(IAddEvent.class).onAdd("java编程思想");
                }
            } else if (v == deleteButton) {
                if (toggleButton.isChecked()) {
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            EventBus.get(IDeleteEvent.class).onDelete("java编程思想");
                        }
                    }.start();
                } else {
                    EventBus.get(IDeleteEvent.class).onDelete("java编程思想");
                }
            } else if (v == updateButton) {
                IUpdateDataEvent updateDataEvent = EventBus.get(IUpdateDataEvent.class);
                updateDataEvent.onUpdate("1111");
            }
        }
    };


}
