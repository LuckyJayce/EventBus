package com.shizhefei.eventbus;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.shizhefei.eventbus.demo.R;
import com.shizhefei.view.indicator.Indicator;
import com.shizhefei.view.indicator.IndicatorViewPager;
import com.shizhefei.view.indicator.slidebar.ColorBar;
import com.shizhefei.view.indicator.transition.OnTransitionTextListener;

public class FragmentTestActivity extends AppCompatActivity {

    private IndicatorViewPager indicatorViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_test);
        Indicator indicator = (Indicator) findViewById(R.id.fragmentTest_indicator);
        ViewPager viewPager = (ViewPager) findViewById(R.id.fragmentTest_viewPager);

        indicator.setScrollBar(new ColorBar(this, Color.RED, 4));
        indicator.setOnTransitionListener(new OnTransitionTextListener().setColor(Color.parseColor("#000000"), Color.parseColor("#aaaaaa")));

        indicatorViewPager = new IndicatorViewPager(indicator, viewPager);
        indicatorViewPager.setAdapter(new PagersAdapter(getSupportFragmentManager()));
    }

    private class PagersAdapter extends IndicatorViewPager.IndicatorFragmentPagerAdapter {

        public PagersAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public View getViewForTab(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.tab_top, container, false);
            }
            TextView textView = (TextView) convertView;
            textView.setText("tab" + position);
            return convertView;
        }

        @Override
        public Fragment getFragmentForPage(int position) {
            Fragment fragment;
            if (position == 0) {
                fragment = new SendFragment();
            } else {
                fragment = new ReceiverFragment();
            }
            return fragment;
        }
    }
}
