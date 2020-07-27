package com.bil.bilmobileads.entity;

import android.os.CountDownTimer;

import com.bil.bilmobileads.PBMobileAds;
import com.bil.bilmobileads.interfaces.TimerCompleteListener;

public class TimerRecall extends CountDownTimer {

    private TimerCompleteListener listener;

    public TimerRecall(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
    }

    public TimerRecall(long millisInFuture, long countDownInterval, TimerCompleteListener timerCompleteListener) {
        super(millisInFuture, countDownInterval);

        this.listener = timerCompleteListener;
    }

    @Override
    public void onTick(long l) {
//        PBMobileAds.getInstance().log("onTick: " + l / 1000);
    }

    @Override
    public void onFinish() {
        if (this.listener != null) {
            this.listener.doWork();
        }
    }

    public void setListener(TimerCompleteListener listener) {
        this.listener = listener;
    }
}