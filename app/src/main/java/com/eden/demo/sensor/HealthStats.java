package com.eden.demo.sensor;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.AttributeSet;

import com.eden.demo.sensor.databinding.HealthStatsBinding;
import com.eden.demo.sensor.hud.SegmentBarsManager;

public class HealthStats extends LinearLayout {
    private HealthStatsBinding binding;
    private SegmentBarsManager segmentBarsManager;

 public HealthStats(Context context) {
 this(context, null);
 }

 public HealthStats(Context context, AttributeSet attrs) {
 super(context, attrs);
 init(context);
 }

 public HealthStats(Context context, AttributeSet attrs, int defStyleAttr) {
 super(context, attrs, defStyleAttr);
 init(context);
 }

    private void init(Context context) {
        binding = HealthStatsBinding.inflate(LayoutInflater.from(context), this, true);
        segmentBarsManager = new SegmentBarsManager(context);

        // Create signal bars
        segmentBarsManager.createSignalBars(binding.signalLayout2d);
        segmentBarsManager.createSegmentBars(binding.segmentBar2d);
    }

    public void updateSignalStrength(int signalStrength) {
        segmentBarsManager.updateSignalBars(binding.signalLayout2d, signalStrength);
    }

    public void updateBatteryLevel(int level) {
        binding.batteryBar2d.setProgress(level);
    }

    public void updateTime(String time) {
        binding.timeDisplay2d.setText(time);
    }

    public void updateDate(String day, String month) {
        binding.dayDisplay2d.setText(day);
        binding.monthDisplay2d.setText(month);
    }
}
